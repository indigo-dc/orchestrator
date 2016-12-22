package it.reply.orchestrator.service.commands;

import com.google.common.collect.Sets;

import alien4cloud.tosca.model.ArchiveRoot;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.Type;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.onedata.OneData.OneDataProviderInfo;
import it.reply.orchestrator.dto.slam.Preference;
import it.reply.orchestrator.dto.slam.PreferenceCustomer;
import it.reply.orchestrator.dto.slam.Priority;
import it.reply.orchestrator.dto.slam.Sla;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.service.ToscaService;

import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

@Component
public class PrefilterCloudProviders extends BaseRankCloudProvidersCommand {

  private static final Logger LOG = LoggerFactory.getLogger(PrefilterCloudProviders.class);

  @Value("${chronos.cloudProviderName}")
  private String chronosCloudProviderName;

  @Autowired
  private DeploymentRepository deploymentRepository;

  @Autowired
  private ToscaService toscaService;

  @Override
  protected RankCloudProvidersMessage customExecute(
      RankCloudProvidersMessage rankCloudProvidersMessage) throws Exception {
    // TODO Filter cloud providers (i.e. based on OneData)

    Deployment deployment =
        deploymentRepository.findOne(rankCloudProvidersMessage.getDeploymentId());

    // Filter out providers that do not support the requested images
    ArchiveRoot ar = toscaService.parseTemplate(deployment.getTemplate());
    Set<CloudProvider> providersToDiscard = Sets.newHashSet();
    Set<CloudService> servicesToDiscard = Sets.newHashSet();

    if (!MapUtils.isEmpty(rankCloudProvidersMessage.getOneDataRequirements())) {
      OneData inputRequirement = rankCloudProvidersMessage.getOneDataRequirements().get("input");
      if (inputRequirement != null && inputRequirement.isSmartScheduling()) {
        for (CloudProvider cloudProvider : rankCloudProvidersMessage.getCloudProviders().values()) {
          boolean hasOneProviderSupportingSpace = false;
          for (CloudService cloudService : cloudProvider.getCmdbProviderServices().values()) {
            if (!cloudService.isOneProviderStorageService()) {
              continue;
            } else {
              for (OneDataProviderInfo providerInfo : inputRequirement.getProviders()) {
                if (Objects.equals(providerInfo.id, cloudService.getData().getEndpoint())) {
                  hasOneProviderSupportingSpace = true;
                  providerInfo.cloudProviderId = cloudProvider.getId();
                  providerInfo.cloudServiceId = cloudService.getId();
                }
              }
            }
          }
          if (!hasOneProviderSupportingSpace) {
            addProviderToDiscard(providersToDiscard, servicesToDiscard, cloudProvider);
          }
        }
      }
      OneData outputRequirement = rankCloudProvidersMessage.getOneDataRequirements().get("output");
      if (outputRequirement != null && outputRequirement.isSmartScheduling()) {
        for (CloudProvider cloudProvider : rankCloudProvidersMessage.getCloudProviders().values()) {
          boolean hasOneProviderSupportingSpace = false;
          for (CloudService cloudService : cloudProvider.getCmdbProviderServices().values()) {
            if (!cloudService.isOneProviderStorageService()) {
              continue;
            } else {
              for (OneDataProviderInfo providerInfo : outputRequirement.getProviders()) {
                if (Objects.equals(providerInfo.id, cloudService.getData().getEndpoint())) {
                  hasOneProviderSupportingSpace = true;
                  providerInfo.cloudProviderId = cloudProvider.getId();
                  providerInfo.cloudServiceId = cloudService.getId();
                }
              }
            }
          }
          if (!hasOneProviderSupportingSpace) {
            addProviderToDiscard(providersToDiscard, servicesToDiscard, cloudProvider);
          }
        }
      }
    }

    discardProvidersAndServices(providersToDiscard, servicesToDiscard, rankCloudProvidersMessage);
    providersToDiscard = Sets.newHashSet();
    servicesToDiscard = Sets.newHashSet();

    // Filter provider for Chronos
    // FIXME: It's just a demo hack to for Chronos jobs default provider override!!
    if (deployment.getDeploymentProvider().equals(DeploymentProvider.CHRONOS)) {
      for (CloudProvider cloudProvider : rankCloudProvidersMessage.getCloudProviders().values()) {
        if (!cloudProvider.getName().equalsIgnoreCase(chronosCloudProviderName)) {
          LOG.debug(
              "Discarded provider {} because it doesn't match Chronos default provider {}"
                  + " for deployment {}",
              cloudProvider.getId(), chronosCloudProviderName, deployment.getId());
          addProviderToDiscard(providersToDiscard, servicesToDiscard, cloudProvider);
        }
      }
    }

    discardProvidersAndServices(providersToDiscard, servicesToDiscard, rankCloudProvidersMessage);
    providersToDiscard = Sets.newHashSet();
    servicesToDiscard = Sets.newHashSet();

    // Filter provider by image contextualization check
    for (CloudProvider cloudProvider : rankCloudProvidersMessage.getCloudProviders().values()) {
      for (CloudService cloudService : cloudProvider.getCmbdProviderServicesByType(Type.COMPUTE)) {
        try {
          toscaService.contextualizeImages(deployment.getDeploymentProvider(), ar, cloudProvider,
              cloudService.getId(), false);
        } catch (Exception ex) {
          // Failed to match all required images -> discard provider
          LOG.debug(
              "Discarded service {} of provider {} because it doesn't match images requirements"
                  + " for deployment {}: {}",
              cloudService.getId(), cloudProvider.getId(), deployment.getId(), ex.getMessage());
          addServiceToDiscard(servicesToDiscard, cloudService);
          cloudProvider.getCmdbProviderImages().remove(cloudService.getId());
        }
      }
    }

    discardProvidersAndServices(providersToDiscard, servicesToDiscard, rankCloudProvidersMessage);

    return rankCloudProvidersMessage;
  }

  protected void addProviderToDiscard(Set<CloudProvider> providersToDiscard,
      Set<CloudService> servicesToDiscard, CloudProvider providerEntry) {
    providersToDiscard.add(providerEntry);
    providerEntry.getCmdbProviderServices()
        .forEach((key, value) -> addServiceToDiscard(servicesToDiscard, value));
  }

  protected void addServiceToDiscard(Set<CloudService> servicesToDiscard,
      CloudService csToDiscard) {
    servicesToDiscard.add(csToDiscard);
  }

  protected void discardProvidersAndServices(Set<CloudProvider> providersToDiscard,
      Set<CloudService> servicesToDiscard, RankCloudProvidersMessage rankCloudProvidersMessage) {
    // Add providers that doesn't have any compute service anymore
    for (CloudProvider cloudProvider : rankCloudProvidersMessage.getCloudProviders().values()) {
      boolean remove = true;
      for (CloudService cloudService : cloudProvider.getCmbdProviderServicesByType(Type.COMPUTE)) {
        if (!servicesToDiscard.contains(cloudService)) {
          remove = false;
        } else {
          cloudProvider.getCmdbProviderServices().remove(cloudService.getId());
        }
      }
      if (remove) {
        addProviderToDiscard(providersToDiscard, servicesToDiscard, cloudProvider);
      }
    }

    // Remove discarded provider and services from SLAs and Preferences
    for (CloudProvider providerToDiscard : providersToDiscard) {
      rankCloudProvidersMessage.getCloudProviders().remove(providerToDiscard.getId());
      Iterator<Sla> slaIt = rankCloudProvidersMessage.getSlamPreferences().getSla().iterator();
      while (slaIt.hasNext()) {
        Sla sla = slaIt.next();
        if (Objects.equals(providerToDiscard.getId(), sla.getProvider())) {
          slaIt.remove();
        }
      }
    }

    for (CloudService cloudService : servicesToDiscard) {
      Iterator<Preference> extPrefIt =
          rankCloudProvidersMessage.getSlamPreferences().getPreferences().iterator();
      while (extPrefIt.hasNext()) {
        Preference extPreference = extPrefIt.next();
        Iterator<PreferenceCustomer> intPrefIt = extPreference.getPreferences().iterator();
        while (intPrefIt.hasNext()) {
          PreferenceCustomer intPreference = intPrefIt.next();
          Iterator<Priority> priorityIt = intPreference.getPriority().iterator();
          while (priorityIt.hasNext()) {
            if (Objects.equals(cloudService.getId(), priorityIt.next().getServiceId())) {
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

  @Override
  protected String getErrorMessagePrefix() {
    return "Error filtering Cloud Providers";
  }
}
