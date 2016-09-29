package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.slam.Service;
import it.reply.orchestrator.dto.slam.Sla;
import it.reply.orchestrator.service.SlamService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GetSlam extends BaseRankCloudProvidersCommand {

  @Autowired
  private SlamService slamService;

  @Override
  protected RankCloudProvidersMessage customExecute(
      RankCloudProvidersMessage rankCloudProvidersMessage) {
    rankCloudProvidersMessage.setSlamPreferences(slamService.getCustomerPreferences());

    // Get VO (customer) preferences and SLAs (infer available Cloud Providers from it)
    for (Sla sla : rankCloudProvidersMessage.getSlamPreferences().getSla()) {
      // Create Cloud Provider, add to the list
      CloudProvider cp = rankCloudProvidersMessage.getCloudProviders().get(sla.getProvider());
      if (cp == null) {
        cp = new CloudProvider(sla.getProvider());
        rankCloudProvidersMessage.getCloudProviders().put(sla.getProvider(), cp);
      }

      // Get provider's services
      for (Service service : sla.getServices()) {
        cp.getCmdbProviderServices().put(service.getServiceId(), null);
      }
    }

    return rankCloudProvidersMessage;
  }

  @Override
  protected String getErrorMessagePrefix() {
    return "Error retrieving SLAs from SLAM";
  }
}
