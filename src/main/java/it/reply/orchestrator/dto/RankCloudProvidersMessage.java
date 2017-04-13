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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import it.reply.monitoringpillar.domain.dsl.monitoring.pillar.wrapper.paas.PaaSMetric;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.deployment.PlacementPolicy;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.ranker.RankedCloudProvider;
import it.reply.orchestrator.dto.slam.SlamPreferences;
import it.reply.orchestrator.enums.DeploymentType;
import it.reply.utils.json.JsonUtility;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class RankCloudProvidersMessage implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  private String deploymentId;

  private SlamPreferences slamPreferences;

  /**
   * Cloud providers indexed by ID.
   */
  private Map<String, CloudProvider> cloudProviders = Maps.newHashMap();

  private Map<String, List<PaaSMetric>> cloudProvidersMonitoringData = Maps.newHashMap();

  private List<RankedCloudProvider> rankedCloudProviders = Lists.newArrayList();

  private Map<String, OneData> oneDataRequirements = Maps.newHashMap();

  /**
   * The Placement policies provided in the template.
   */
  private List<PlacementPolicy> placementPolicies = Lists.newArrayList();

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

  public String getDeploymentId() {
    return deploymentId;
  }

  public void setDeploymentId(String deploymentId) {
    this.deploymentId = deploymentId;
  }

  public SlamPreferences getSlamPreferences() {
    return slamPreferences;
  }

  public void setSlamPreferences(SlamPreferences slamPreferences) {
    this.slamPreferences = slamPreferences;
  }

  public Map<String, CloudProvider> getCloudProviders() {
    return cloudProviders;
  }

  public void setCloudProviders(Map<String, CloudProvider> cloudProviders) {
    this.cloudProviders = cloudProviders;
  }

  public Map<String, List<PaaSMetric>> getCloudProvidersMonitoringData() {
    return cloudProvidersMonitoringData;
  }

  public void setCloudProvidersMonitoringData(
      Map<String, List<PaaSMetric>> cloudProvidersMonitoringData) {
    this.cloudProvidersMonitoringData = cloudProvidersMonitoringData;
  }

  public List<RankedCloudProvider> getRankedCloudProviders() {
    return rankedCloudProviders;
  }

  public void setRankedCloudProviders(List<RankedCloudProvider> rankedCloudProviders) {
    this.rankedCloudProviders = rankedCloudProviders;
  }

  public Map<String, OneData> getOneDataRequirements() {
    return oneDataRequirements;
  }

  public void setOneDataRequirements(Map<String, OneData> oneDataRequirements) {
    this.oneDataRequirements = oneDataRequirements;
  }

  public List<PlacementPolicy> getPlacementPolicies() {
    return placementPolicies;
  }

  public void setPlacementPolicies(List<PlacementPolicy> placementPolicies) {
    this.placementPolicies = placementPolicies;
  }

  public DeploymentType getDeploymentType() {
    return deploymentType;
  }

  public void setDeploymentType(DeploymentType deploymentType) {
    this.deploymentType = deploymentType;
  }

  public OidcTokenId getRequestedWithToken() {
    return requestedWithToken;
  }

  public void setRequestedWithToken(OidcTokenId requestedWithToken) {
    this.requestedWithToken = requestedWithToken;
  }

  @Override
  public String toString() {
    try {
      return JsonUtility.serializeJson(this);
    } catch (Exception ex) {
      throw new RuntimeException(ex.getMessage(), ex);
    }
  }

}
