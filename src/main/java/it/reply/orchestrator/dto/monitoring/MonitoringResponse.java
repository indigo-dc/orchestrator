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

package it.reply.orchestrator.dto.monitoring;

import it.reply.domain.dsl.prisma.restprotocol.Meta;
import it.reply.monitoringpillar.domain.dsl.monitoring.pillar.wrapper.paas.MonitoringWrappedResponsePaas;
import it.reply.orchestrator.dto.AdditionalPropertiesAwareDto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MonitoringResponse extends AdditionalPropertiesAwareDto implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  @Nullable
  private Meta meta;

  @Nullable
  private MonitoringWrappedResponsePaas result;

}
