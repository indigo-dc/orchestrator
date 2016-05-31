package it.reply.orchestrator.dto;

import it.reply.orchestrator.dto.ranker.RankedCloudProvider;
import it.reply.orchestrator.dto.slam.SlamPreferences;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RankCloudProvidersMessage implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  private SlamPreferences slamPreferences;

  /**
   * Cloud providers indexed by ID.
   */
  private Map<String, CloudProvider> cloudProviders = new HashMap<>();

  private Map<String, List<it.reply.monitoringpillar.domain.dsl.monitoring.pillar.wrapper.paas.PaaSMetric>> cloudProvidersMonitoringData =
      new HashMap<>();

  private List<RankedCloudProvider> rankedCloudProviders = new ArrayList<>();

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

  public
      Map<String, List<it.reply.monitoringpillar.domain.dsl.monitoring.pillar.wrapper.paas.PaaSMetric>>
      getCloudProvidersMonitoringData() {
    return cloudProvidersMonitoringData;
  }

  public void setCloudProvidersMonitoringData(
      Map<String, List<it.reply.monitoringpillar.domain.dsl.monitoring.pillar.wrapper.paas.PaaSMetric>> cloudProvidersMonitoringData) {
    this.cloudProvidersMonitoringData = cloudProvidersMonitoringData;
  }

  public List<RankedCloudProvider> getRankedCloudProviders() {
    return rankedCloudProviders;
  }

  public void setRankedCloudProviders(List<RankedCloudProvider> rankedCloudProviders) {
    this.rankedCloudProviders = rankedCloudProviders;
  }

  @Override
  public String toString() {
    return "RankCloudProvidersMessage [slamPreferences=" + slamPreferences + ", cloudProviders="
        + cloudProviders + ", cloudProvidersMonitoringData=" + cloudProvidersMonitoringData
        + ", rankedCloudProviders=" + rankedCloudProviders + "]";
  }

}
