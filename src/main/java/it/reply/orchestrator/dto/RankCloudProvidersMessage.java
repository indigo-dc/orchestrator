/*
 * Copyright © 2015-2019 Santer Reply S.p.A.
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

package it.reply.orchestrator.dto;

import it.reply.monitoringpillar.domain.dsl.monitoring.pillar.wrapper.paas.PaasMachine;
import it.reply.orchestrator.dto.cmdb.CloudProvider;
import it.reply.orchestrator.dto.deployment.BaseWorkflowMessage;
import it.reply.orchestrator.dto.ranker.RankedCloudService;
import it.reply.orchestrator.dto.slam.SlamPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Tolerate;

import org.checkerframework.checker.nullness.qual.NonNull;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RankCloudProvidersMessage extends BaseWorkflowMessage {

  private SlamPreferences slamPreferences;

  @NonNull
  private Map<String, CloudProvider> cloudProviders = new HashMap<>();

  @NonNull
  private Map<String, List<PaasMachine>> cloudProvidersMonitoringData = new HashMap<>();

  @NonNull
  private List<RankedCloudService> rankedCloudServices = new ArrayList<>();

  @Tolerate
  public RankCloudProvidersMessage(BaseWorkflowMessage baseWorkflowMessage) {
    super(baseWorkflowMessage);
  }
}
