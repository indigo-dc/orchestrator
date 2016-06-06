package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.ranker.CloudProviderRankerRequest;
import it.reply.orchestrator.dto.ranker.Monitoring;
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
  protected RankCloudProvidersMessage
      customExecute(RankCloudProvidersMessage rankCloudProvidersMessage) {

    // Prepare Ranker's request
    List<Monitoring> monitoring =
        rankCloudProvidersMessage.getCloudProvidersMonitoringData().entrySet().stream()
            .map(e -> new Monitoring(e.getKey(), e.getValue())).collect(Collectors.toList());

    CloudProviderRankerRequest cprr =
        new CloudProviderRankerRequest()
            .withPreferences(rankCloudProvidersMessage.getSlamPreferences().getPreferences().get(0)
                .getPreferences())
            .withSla(rankCloudProvidersMessage.getSlamPreferences().getSla())
            .withMonitoring(monitoring);

    // Get provider rank and save in message
    rankCloudProvidersMessage
        .setRankedCloudProviders(cloudProviderRankerService.getProviderRanking(cprr));

    return rankCloudProvidersMessage;
  }

}
