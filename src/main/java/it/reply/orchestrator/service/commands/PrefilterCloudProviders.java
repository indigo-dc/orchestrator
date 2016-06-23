package it.reply.orchestrator.service.commands;

import alien4cloud.tosca.model.ArchiveRoot;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.slam.Preference;
import it.reply.orchestrator.dto.slam.PreferenceCustomer;
import it.reply.orchestrator.dto.slam.Priority;
import it.reply.orchestrator.dto.slam.Sla;
import it.reply.orchestrator.service.ToscaService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
public class PrefilterCloudProviders extends BaseRankCloudProvidersCommand {

  private static final Logger LOG = LogManager.getLogger(PrefilterCloudProviders.class);

  @Autowired
  private DeploymentRepository deploymentRepository;

  @Autowired
  private ToscaService toscaService;

  @Override
  protected RankCloudProvidersMessage
      customExecute(RankCloudProvidersMessage rankCloudProvidersMessage) throws Exception {
    // TODO Filter cloud providers (i.e. based on OneData)

    Deployment deployment =
        deploymentRepository.findOne(rankCloudProvidersMessage.getDeploymentId());

    // Filter out providers that do not support the requested images
    ArchiveRoot ar = toscaService.parseTemplate(deployment.getTemplate());
    Iterator<Map.Entry<String, CloudProvider>> it =
        rankCloudProvidersMessage.getCloudProviders().entrySet().iterator();
    List<String> providersToDiscard = new ArrayList<>();
    List<String> servicesToDiscard = new ArrayList<>();
    while (it.hasNext()) {
      Map.Entry<String, CloudProvider> entry = it.next();
      try {
        toscaService.contextualizeImages(ar, entry.getValue(), false);
      } catch (Exception ex) {
        // Failed to match all required images -> discard provider
        LOG.debug("Discarded provider {} because it doesn't match images requirements"
            + " for deployment {}: {}", entry.getKey(), deployment.getId(), ex.getMessage());
        providersToDiscard.add(entry.getKey());
        servicesToDiscard.addAll(entry.getValue().getCmdbProviderServices().keySet());
        it.remove();
      }

    }

    // Remove discarded provider from SLAs and Preferences
    for (String providerName : providersToDiscard) {
      Iterator<Sla> slaIt = rankCloudProvidersMessage.getSlamPreferences().getSla().iterator();
      while (slaIt.hasNext()) {
        if (providerName.equals(slaIt.next().getProvider())) {
          slaIt.remove();
        }
      }
    }

    for (String serviceId : servicesToDiscard) {
      Iterator<Preference> extPrefIt =
          rankCloudProvidersMessage.getSlamPreferences().getPreferences().iterator();
      while (extPrefIt.hasNext()) {
        Preference extPreference = extPrefIt.next();
        Iterator<PreferenceCustomer> intPrefIt = extPreference.getPreferences().iterator();
        while (intPrefIt.hasNext()) {
          PreferenceCustomer intPreference = intPrefIt.next();
          Iterator<Priority> priorityIt = intPreference.getPriority().iterator();
          while (priorityIt.hasNext()) {
            if (serviceId.equals(priorityIt.next().getServiceId())) {
              priorityIt.remove();
            }
          }
          if (intPreference.getPriority().isEmpty()) {
            intPrefIt.remove();
          }
        }
        if (extPreference.getPreferences().isEmpty()) {
          extPrefIt.remove();
        }

      }
    }

    return rankCloudProvidersMessage;
  }

}
