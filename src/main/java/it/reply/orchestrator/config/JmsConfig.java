package it.reply.orchestrator.config;

import io.netty.channel.EventLoopGroup;
import java.lang.reflect.Type;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.Reactor2StompCodec;
import org.springframework.messaging.simp.stomp.Reactor2TcpStompClient;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompDecoder;
import org.springframework.messaging.simp.stomp.StompEncoder;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.tcp.reactor.Reactor2TcpClient;
import org.springframework.stereotype.Component;

import reactor.Environment;
import reactor.core.support.NamedDaemonThreadFactory;
import reactor.io.net.NetStreams;
import reactor.io.net.Spec;
import reactor.io.net.impl.netty.NettyClientSocketOptions;
import reactor.io.net.impl.netty.NettyNativeDetector;

@Configuration
@Slf4j
public class JmsConfig {

  @Bean(destroyMethod = "shutdownGracefully")
  public EventLoopGroup reactorEventLoopGroup() {
    int ioThreadCount = Runtime.getRuntime().availableProcessors();
    NamedDaemonThreadFactory threadFactory = new NamedDaemonThreadFactory("reactor-tcp-io");
    return NettyNativeDetector.newEventLoopGroup(ioThreadCount, threadFactory);
  }

  @Bean
  public Environment reactorEnvironment() {
    return new Environment();
  }

  @Component
  public static class RucioStompClient implements DisposableBean {
    private final EventLoopGroup eventLoopGroup;
    private final Environment environment;
    private final List<StompSession> stompSessions;

    public RucioStompClient(EventLoopGroup eventLoopGroup, Environment environment) throws UnknownHostException {
      this.eventLoopGroup = eventLoopGroup;
      this.environment = environment;
      this.stompSessions = Arrays
        .stream(InetAddress.getAllByName("netmon-mb.cern.ch"))
        .filter(Inet4Address.class::isInstance)
        .map(address -> newStompSession(address.getHostAddress(), 61513))
        .collect(Collectors.toList());
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
        }

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
          StompHeaders stompHeaders = new StompHeaders();
          stompHeaders.setDestination("/queue/Consumer.xdc.doma.rucio.events");
          //stompHeaders.setAck("auto");
          stompHeaders.setAck("client-individual");
          session.subscribe(stompHeaders, new RucioMessageHandler(session));
        }

      };

      StompHeaders auth = new StompHeaders();
      auth.setLogin("domarucioc");
      auth.setPasscode("Y2jZ9Xdih2PkNBRZ");
      try {
        return stompClient.connect(auth, connectionHandler).get(1, TimeUnit.SECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        e.printStackTrace();
        return null;
      }
    }

    @Override
    public void destroy() {
      this.stompSessions.forEach(StompSession::disconnect);
    }

    private static class RucioMessageHandler implements StompFrameHandler {
      private StompSession session;

      public RucioMessageHandler(StompSession session) {
        this.session = session;
      }

      @Override
      public Type getPayloadType(StompHeaders headers) {
        return String.class;
      }

      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        LOG.info(payload.toString());
        if (headers.getAck() != null) {
          session.acknowledge(headers.getAck(), true);
        }
      }
    }
    private static class StompTcpClientSpecFactory implements NetStreams.TcpClientFactory<Message<byte[]>, Message<byte[]>> {

      private final String host;

      private final int port;

      private final NettyClientSocketOptions socketOptions;

      private final Environment environment;

      private final Reactor2StompCodec codec;


      StompTcpClientSpecFactory(String host, int port, EventLoopGroup group, Environment environment) {
        this.host = host;
        this.port = port;
        this.socketOptions = new NettyClientSocketOptions().eventLoopGroup(group);
        this.environment = environment;
        this.codec = new Reactor2StompCodec(new StompEncoder(), new StompDecoder());
      }

      @Override
      public Spec.TcpClientSpec<Message<byte[]>, Message<byte[]>> apply(
        Spec.TcpClientSpec<Message<byte[]>, Message<byte[]>> clientSpec) {

        return clientSpec
          .env(this.environment)
//        .dispatcher(this.environment.getDispatcher(Environment.WORK_QUEUE))
          .connect(this.host, this.port)
          .codec(this.codec)
          .options(this.socketOptions);
      }
    }
  }

}
