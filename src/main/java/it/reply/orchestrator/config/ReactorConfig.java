package it.reply.orchestrator.config;

import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.Environment;
import reactor.core.support.NamedDaemonThreadFactory;
import reactor.io.net.impl.netty.NettyNativeDetector;

@Configuration
@Slf4j
public class ReactorConfig {

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

}
