/*
 * Copyright © 2015-2018 Santer Reply S.p.A.
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

package it.reply.orchestrator.dto.mesos;

import it.reply.orchestrator.utils.Named;

import lombok.Data;
import lombok.Getter;

import javax.annotation.Nonnull;

@Data
public class MesosPortMapping {

  @Getter
  public enum Protocol implements Named {

    TCP("tcp"),
    UDP("udp"),
    IGMP("igmp");

    private final String name;

    Protocol(String name) {
      this.name = name;
    }

  }

  @Nonnull
  private Integer containerPort;

  private Integer servicePort;

  @Nonnull
  private Protocol protocol = Protocol.TCP;

}
