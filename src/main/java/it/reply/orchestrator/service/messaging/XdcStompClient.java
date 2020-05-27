package it.reply.orchestrator.service.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.EventLoopGroup;
import it.reply.orchestrator.config.properties.RucioClientProperties;
import it.reply.orchestrator.config.properties.XdcClientProperties;
import it.reply.orchestrator.dto.messaging.xdc.GenericEvent;
import it.reply.orchestrator.exception.OrchestratorException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.Reactor2TcpStompClient;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.tcp.reactor.Reactor2TcpClient;
import org.springframework.stereotype.Component;
import reactor.Environment;

@Component
@Slf4j
@EnableConfigurationProperties(XdcClientProperties.class)
public class XdcStompClient implements SmartLifecycle {

  private final XdcClientProperties stompClientProperties;
  private final EventLoopGroup eventLoopGroup;
  private final Environment environment;
  private final Map<String, StompSession> stompSessions;
  private final XdcMessageHandlerFactory xdcMessageHandlerFactory;

  private final Object lifecycleMonitor = new Object();

  private volatile boolean isRunning = false;

  public XdcStompClient(XdcClientProperties stompClientProperties, EventLoopGroup eventLoopGroup, Environment environment, XdcMessageHandlerFactory xdcMessageHandlerFactory) {
    this.stompClientProperties = stompClientProperties;
    this.eventLoopGroup = eventLoopGroup;
    this.environment = environment;
    this.xdcMessageHandlerFactory = xdcMessageHandlerFactory;
    this.stompSessions = new HashMap<>();
  }

  public StompSession newStompSession(String host, int port) {
    Reactor2TcpStompClient stompClient = new Reactor2TcpStompClient(new Reactor2TcpClient<>(new StompTcpClientSpecFactory(host, port, eventLoopGroup, environment)));

    stompClient.setMessageConverter(new StringMessageConverter());

    StompSessionHandler connectionHandler = new StompSessionHandlerAdapter() {
      @Override
      public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
        LOG.error("Error handling STOMP command {} with headers {}", command, headers, exception);
        if (headers.getAck() != null) {
          session.acknowledge(headers.getAck(), false);
        }
      }

      @Override
      public void handleTransportError(StompSession session, Throwable exception) {
        LOG.error("Transport error for session {}", session.getSessionId(), exception);
        resetSession(host, session);
      }

      @Override
      public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        StompHeaders stompHeaders = new StompHeaders();
        stompHeaders.setDestination(stompClientProperties.getDestination());
        stompHeaders.setAck("client-individual");
        stompHeaders.set("activemq.prefetchSize", "1");
        XdcMessageHandler messageHandler = xdcMessageHandlerFactory.getObject(session);
        session.subscribe(stompHeaders, messageHandler);
        LOG.info("STOMP session with id: {} subscribed to destination: {}", session.getSessionId(), stompClientProperties.getDestination());
      }

    };

    StompHeaders auth = new StompHeaders();
    auth.setLogin(stompClientProperties.getUsername());
    auth.setPasscode(stompClientProperties.getPassword());
    try {
      StompSession session = stompClient.connect(auth, connectionHandler).get(2, TimeUnit.SECONDS);
      LOG.info("STOMP session with id: {} created for host: {}, port: {}, ", session.getSessionId(), host, port);
      return session;
    } catch (InterruptedException | ExecutionException | TimeoutException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new OrchestratorException("Error initializing Stomp session with host: " +host +", port: " + port, ex);
    }
  }

  @Override
  public boolean isAutoStartup() {
    return stompClientProperties.isEnabled();
  }

  @Override
  public void stop(Runnable callback) {
    synchronized (this.lifecycleMonitor) {
      this.stop();
      callback.run();
    }
  }

  private boolean disconnectSession(String address, StompSession session) {
    LOG.info("Disconnecting STOMP session with id: {} from broker address: {}", session.getSessionId(), address);
    if (session.isConnected()) {
      try {
        session.disconnect();
        return true;
      } catch (RuntimeException ex) {
        LOG.error("Error disconnecting STOMP session with id: {}", session.getSessionId(), ex);
        return false;
      }
    } else {
      LOG.info("STOMP session with id: {} already disconnected", session.getSessionId());
      return false;
    }
  }

  private void resetSession(String address, StompSession session) {
    synchronized (lifecycleMonitor) {
      stompSessions.remove(address);
      session.disconnect();
      resetSessions(true);
    }
  }
  private void resetSessions(boolean retry) {
    synchronized (lifecycleMonitor){
      if (isRunning) {
        if (retry) {
          LOG.info("Retrying connection to STOMP broker {}:{}", stompClientProperties.getHost(), stompClientProperties.getPort());
        } else {
          LOG.info("Connecting to STOMP broker {}:{}", stompClientProperties.getHost(), stompClientProperties.getPort());
        }
        final Set<String> addresses;
        try {
          addresses = Arrays
            .stream(InetAddress.getAllByName(stompClientProperties.getHost()))
            .filter(Inet4Address.class::isInstance)
            .map(InetAddress::getHostAddress)
            .collect(Collectors.toSet());
        } catch (UnknownHostException ex) {
          if (retry) {
            LOG.info("Error connecting to STOMP broker {}:{}, retrying in 10 seconds", stompClientProperties.getHost(), stompClientProperties.getPort(), ex);
            this.eventLoopGroup.schedule(() -> resetSessions(true), 10, TimeUnit.SECONDS);
          }
          throw new OrchestratorException("Error resolving broker domain name", ex);
        }
        Map<String, StompSession> newSessions = new HashMap<>();
        this.stompSessions.forEach((address, session) -> {
          if (addresses.contains(address)) {
            if (session.isConnected()) {
              newSessions.put(address, session);
            } else {
              try {
                newSessions.put(address, newStompSession(address, stompClientProperties.getPort()));
              } catch (RuntimeException ex) {
                if (retry) {
                  LOG.info("Error connecting to STOMP broker {}:{}, retrying in 10 seconds", stompClientProperties.getHost(), stompClientProperties.getPort(), ex);
                  this.eventLoopGroup.schedule(() -> resetSessions(true), 10, TimeUnit.SECONDS);
                }
                throw ex;
              }
            }
          } else {
            disconnectSession(address, session);
          }
        });

        addresses.forEach(address -> {
          if (!newSessions.containsKey(address)) {
            newSessions.put(address, newStompSession(address, stompClientProperties.getPort()));
          }
        });
        this.stompSessions.clear();
        this.stompSessions.putAll(newSessions);
      }
    }
  }

  @Override
  public void start() {
    synchronized (lifecycleMonitor){
      this.isRunning = true;
      resetSessions(false);
    }
  }

  @Override
  public void stop() {
    synchronized (lifecycleMonitor){
      isRunning = false;
      stompSessions.forEach(this::disconnectSession);
      stompSessions.clear();
    }
  }

  @Override
  public boolean isRunning() {
    return this.isRunning;
  }

  @Override
  public int getPhase() {
    return Integer.MAX_VALUE;
  }

  @Component
  public static class XdcMessageHandlerFactory {
    private final ObjectMapper objectMapper;
    private final XdcEventHandlerService xdcEventHandlerService;

    private XdcMessageHandlerFactory(ObjectMapper objectMapper, XdcEventHandlerService xdcEventHandlerService) {
      this.objectMapper = objectMapper;
      this.xdcEventHandlerService = xdcEventHandlerService;
    }

    public XdcMessageHandler getObject(StompSession session) {
      return new XdcMessageHandler(objectMapper, xdcEventHandlerService, session);
    }
  }

  public static class XdcMessageHandler implements StompFrameHandler {
    private final StompSession session;

    private final ObjectMapper objectMapper;
    private final XdcEventHandlerService xdcEventHandlerService;

    public XdcMessageHandler(ObjectMapper objectMapper, XdcEventHandlerService xdcEventHandlerService, StompSession session) {
      this.xdcEventHandlerService = xdcEventHandlerService;
      this.session = session;
      this.objectMapper = objectMapper;
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
      return String.class;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
      try {
        if (payload != null) {
          GenericEvent xdcEvent = objectMapper.readValue(payload.toString(), GenericEvent.class);
          xdcEventHandlerService.handleEvent(xdcEvent);
        }
      } catch (RuntimeException | IOException ex) {
        LOG.error("Error handling XDC message {}", payload, ex);
        if (headers.getAck() != null) {
          session.acknowledge(headers.getAck(), false);
        }
        return;
      }
      if (headers.getAck() != null) {
        session.acknowledge(headers.getAck(), true);
      }
    }
  }

}
