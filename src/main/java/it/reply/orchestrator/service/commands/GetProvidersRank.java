package it.reply.orchestrator.service.commands;

import com.google.common.collect.Lists;

import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.ranker.CloudProviderRankerRequest;
import it.reply.orchestrator.dto.ranker.Monitoring;
import it.reply.orchestrator.dto.slam.PreferenceCustomer;
import it.reply.orchestrator.service.CloudProviderRankerService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class GetProvidersRank extends BaseRankCloudProvidersCommand {

  @Autowired
  private CloudProviderRankerService cloudProviderRankerService;

  @Override
  protected RankCloudProvidersMessage customExecute(
      RankCloudProvidersMessage rankCloudProvidersMessage) {

    if (rankCloudProvidersMessage.getCloudProviders().isEmpty()) {
      // nothing to rank
      return rankCloudProvidersMessage;
    }
    // Prepare Ranker's request
    List<Monitoring> monitoring =
        rankCloudProvidersMessage.getCloudProvidersMonitoringData().entrySet().stream()
            .map(e -> new Monitoring(e.getKey(), e.getValue())).collect(Collectors.toList());

    List<PreferenceCustomer> preferences = Lists.newArrayList();
    if (!rankCloudProvidersMessage.getSlamPreferences().getPreferences().isEmpty()) {
      preferences =
          rankCloudProvidersMessage.getSlamPreferences().getPreferences().get(0).getPreferences();
    }
    CloudProviderRankerRequest cprr = new CloudProviderRankerRequest().withPreferences(preferences)
        .withSla(rankCloudProvidersMessage.getSlamPreferences().getSla())
        .withMonitoring(monitoring);

    // Get provider rank and save in message
    rankCloudProvidersMessage
        .setRankedCloudProviders(cloudProviderRankerService.getProviderRanking(cprr));

    return rankCloudProvidersMessage;
  }

  @Override
  protected String getErrorMessagePrefix() {
    return "Error retrieving info from Cloud Provider Ranker";
  }
}
