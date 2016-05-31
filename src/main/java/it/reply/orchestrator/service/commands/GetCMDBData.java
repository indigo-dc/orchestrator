package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.cmdb.Service;
import it.reply.orchestrator.service.CmdbService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GetCMDBData extends BaseRankCloudProvidersCommand {

  @Autowired
  private CmdbService cmdbService;

  @Override
  protected RankCloudProvidersMessage
      customExecute(RankCloudProvidersMessage rankCloudProvidersMessage) {

    // Get CMDB data for each Cloud Provider
    for (Map.Entry<String, CloudProvider> providerEntry : rankCloudProvidersMessage
        .getCloudProviders().entrySet()) {
      CloudProvider cp = providerEntry.getValue();
      // Get provider's data
      cp.setCmdbProviderData(cmdbService.getProviderById(providerEntry.getKey()));
      cp.setId(providerEntry.getKey());
      cp.setName(cp.getCmdbProviderData().getId());

      // Get provider's services' data
      for (Map.Entry<String, Service> serviceEntry : cp.getCmdbProviderServices().entrySet()) {
        serviceEntry.setValue(cmdbService.getServiceById(serviceEntry.getKey()));
      }

      // TODO Get other data (i.e. OneData, Images mapping, etc)
    }

    return rankCloudProvidersMessage;
  }

}
