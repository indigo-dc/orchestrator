/*
 * Copyright © 2015-2021 I.N.F.N.
 * Copyright © 2015-2020 Santer Reply S.p.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

  /**
   * Generated an {@link EventLoopGroup}.
   * @return the EventLoopGroup
   */
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
