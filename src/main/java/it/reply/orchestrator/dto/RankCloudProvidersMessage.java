/*
 * Copyright © 2015-2017 Santer Reply S.p.A.
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

import it.reply.monitoringpillar.domain.dsl.monitoring.pillar.wrapper.paas.PaaSMetric;
import it.reply.orchestrator.dto.deployment.BaseWorkflowMessage;
import it.reply.orchestrator.dto.ranker.RankedCloudProvider;
import it.reply.orchestrator.dto.slam.SlamPreferences;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Tolerate;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RankCloudProvidersMessage extends BaseWorkflowMessage implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  private SlamPreferences slamPreferences;

  /**
   * Cloud providers indexed by ID.
   */
  @NonNull
  private Map<String, CloudProvider> cloudProviders = new HashMap<>();

  @NonNull
  private Map<String, List<PaaSMetric>> cloudProvidersMonitoringData = new HashMap<>();

  @NonNull
  private List<RankedCloudProvider> rankedCloudProviders = new ArrayList<>();

  @Tolerate
  public RankCloudProvidersMessage(BaseWorkflowMessage baseWorkflowMessage) {
    super(baseWorkflowMessage);
  }
}
