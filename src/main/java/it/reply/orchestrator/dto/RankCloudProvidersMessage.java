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
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.deployment.PlacementPolicy;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.ranker.RankedCloudProvider;
import it.reply.orchestrator.dto.slam.SlamPreferences;
import it.reply.orchestrator.enums.DeploymentType;

import lombok.Data;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class RankCloudProvidersMessage implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  private String deploymentId;

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

  @NonNull
  private Map<String, OneData> oneDataRequirements = new HashMap<>();

  /**
   * The Placement policies provided in the template.
   */
  @NonNull
  private List<PlacementPolicy> placementPolicies = new ArrayList<>();

  private DeploymentType deploymentType;

  @Nullable
  private OidcTokenId requestedWithToken;

  public RankCloudProvidersMessage() {
  }

  public RankCloudProvidersMessage(String deploymentId) {
    this.deploymentId = deploymentId;
  }

  /**
   * Create a RankCloudProvidersMessage from a {@link DeploymentMessage}.
   * 
   * @param deploymentMessage
   *          the DeploymentMessage
   */
  public RankCloudProvidersMessage(DeploymentMessage deploymentMessage) {
    this.deploymentId = deploymentMessage.getDeploymentId();
    this.oneDataRequirements = deploymentMessage.getOneDataRequirements();
    this.placementPolicies = deploymentMessage.getPlacementPolicies();
    this.deploymentType = deploymentMessage.getDeploymentType();
    this.requestedWithToken = deploymentMessage.getRequestedWithToken();
  }

}
