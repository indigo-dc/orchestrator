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
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.service.ToscaService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
public class PrefilterCloudProviders extends BaseRankCloudProvidersCommand {

  private static final Logger LOG = LogManager.getLogger(PrefilterCloudProviders.class);

  @Value("${chronos.cloudProviderName}")
  private String chronosCloudProviderName;

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
    Iterator<Map.Entry<String, CloudProvider>> it;
    List<String> providersToDiscard = new ArrayList<>();
    List<String> servicesToDiscard = new ArrayList<>();

    // Filter provider for Chronos
    it = rankCloudProvidersMessage.getCloudProviders().entrySet().iterator();
    // FIXME: It's just a demo hack to for Chronos jobs default provider override!!
    if (deployment.getDeploymentProvider().equals(DeploymentProvider.CHRONOS)) {
      while (it.hasNext()) {
        Map.Entry<String, CloudProvider> entry = it.next();
        if (!entry.getValue().getName().equalsIgnoreCase(chronosCloudProviderName)) {
          LOG.debug(
              "Discarded provider {} because it doesn't match Chronos default provider {}"
                  + " for deployment {}",
              entry.getKey(), chronosCloudProviderName, deployment.getId());
          addProviderToDiscard(providersToDiscard, servicesToDiscard, it, entry);
        }
      }
    }

    // Filter provider by image contextualization check
    it = rankCloudProvidersMessage.getCloudProviders().entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, CloudProvider> entry = it.next();
      try {
        toscaService.contextualizeImages(deployment.getDeploymentProvider(), ar, entry.getValue(), false);
      } catch (Exception ex) {
        // Failed to match all required images -> discard provider
        LOG.debug("Discarded provider {} because it doesn't match images requirements"
            + " for deployment {}: {}", entry.getKey(), deployment.getId(), ex.getMessage());
        addProviderToDiscard(providersToDiscard, servicesToDiscard, it, entry);
      }
    }

    discardProviders(providersToDiscard, servicesToDiscard, rankCloudProvidersMessage);

    return rankCloudProvidersMessage;
  }

  protected void addProviderToDiscard(List<String> providersToDiscard,
      List<String> servicesToDiscard, Iterator<Map.Entry<String, CloudProvider>> it,
      Map.Entry<String, CloudProvider> providerEntry) {
    providersToDiscard.add(providerEntry.getKey());
    servicesToDiscard.addAll(providerEntry.getValue().getCmdbProviderServices().keySet());
    it.remove();
  }

  protected void discardProviders(List<String> providersToDiscard, List<String> servicesToDiscard,
      RankCloudProvidersMessage rankCloudProvidersMessage) {
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
  }
}
