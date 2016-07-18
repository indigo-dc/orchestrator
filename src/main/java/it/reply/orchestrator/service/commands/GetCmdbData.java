package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.cmdb.Service;
import it.reply.orchestrator.dto.cmdb.Type;
import it.reply.orchestrator.service.CmdbService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GetCmdbData extends BaseRankCloudProvidersCommand {

  private static final Logger LOG = LogManager.getLogger(GetCmdbData.class);

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

      // FIXME Get other data (i.e. OneData, Images mapping, etc)

      // Get images for provider (requires to know the compute service)
      // FIXME: What if there are multiple compute service for a provider (remember that those are
      // SLAM given)?
      Service imageService = cp.getCmbdProviderServiceByType(Type.COMPUTE);
      if (imageService != null) {
        LOG.debug("Retrieving image list for service <{}> of provider <{}>", imageService.getId(),
            cp.getId());
        cp.setCmdbProviderImages(cmdbService.getImagesByService(imageService.getId()).stream()
            .map(e -> e.getData()).collect(Collectors.toList()));
      } else {
        LOG.debug("No image service to retrieve image list from for provider <{}>", cp.getId());
      }
    }

    return rankCloudProvidersMessage;
  }

}
