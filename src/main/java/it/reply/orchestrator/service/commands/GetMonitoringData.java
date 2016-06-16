package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.service.MonitoringService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GetMonitoringData extends BaseRankCloudProvidersCommand {

  @Autowired
  private MonitoringService monitoringService;

  @Override
  protected RankCloudProvidersMessage
      customExecute(RankCloudProvidersMessage rankCloudProvidersMessage) {

    // Get monitoring data for each Cloud Provider
    for (Map.Entry<String, CloudProvider> providerEntry : rankCloudProvidersMessage
        .getCloudProviders().entrySet()) {
      CloudProvider cp = providerEntry.getValue();
      // TODO fix ugliness
      rankCloudProvidersMessage.getCloudProvidersMonitoringData().put(providerEntry.getKey(),
          monitoringService.getProviderData(cp.getId()).getGroups().get(0).getPaasMachines().get(0)
              .getServices().get(0).getPaasMetrics());
    }
    return rankCloudProvidersMessage;
  }

}
