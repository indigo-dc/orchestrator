package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.ranker.CloudProviderRankerRequest;
import it.reply.orchestrator.dto.ranker.Monitoring;
import it.reply.orchestrator.dto.ranker.RankedCloudProvider;
import it.reply.orchestrator.service.CloudProviderRankerService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
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

    // FIXME Remove once the ranker is available
    // Get provider rank and save in message
    try {
      rankCloudProvidersMessage
          .setRankedCloudProviders(cloudProviderRankerService.getProviderRanking(cprr));
    } catch (Exception e2) {
      rankCloudProvidersMessage.setRankedCloudProviders(
          Arrays.asList(new RankedCloudProvider("Name1", 1, true, ""), new RankedCloudProvider(
              "STUB PROVIDER NAME -> Remove once the ranker is available", 0, true, "Error msg")));
    }

    return rankCloudProvidersMessage;
  }

}
