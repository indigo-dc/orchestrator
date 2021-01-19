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

package it.reply.orchestrator.service.messaging;

import io.netty.channel.EventLoopGroup;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.Reactor2StompCodec;
import org.springframework.messaging.simp.stomp.StompDecoder;
import org.springframework.messaging.simp.stomp.StompEncoder;
import reactor.Environment;
import reactor.io.net.NetStreams;
import reactor.io.net.Spec;
import reactor.io.net.impl.netty.NettyClientSocketOptions;

class StompTcpClientSpecFactory
    implements NetStreams.TcpClientFactory<Message<byte[]>, Message<byte[]>> {

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
        .connect(this.host, this.port)
        .codec(this.codec)
        .options(this.socketOptions);
  }
}
