/*
 * Copyright © 2015-2017 Santer Reply S.p.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.reply.orchestrator.service.commands;

import com.google.common.collect.Sets;

import alien4cloud.tosca.model.ArchiveRoot;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.Type;
import it.reply.orchestrator.dto.deployment.AwsSlaPlacementPolicy;
import it.reply.orchestrator.dto.deployment.CredentialsAwareSlaPlacementPolicy;
import it.reply.orchestrator.dto.deployment.PlacementPolicy;
import it.reply.orchestrator.dto.deployment.SlaPlacementPolicy;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.onedata.OneData.OneDataProviderInfo;
import it.reply.orchestrator.dto.slam.Preference;
import it.reply.orchestrator.dto.slam.PreferenceCustomer;
import it.reply.orchestrator.dto.slam.Priority;
import it.reply.orchestrator.dto.slam.Sla;
import it.reply.orchestrator.enums.DeploymentType;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.service.ToscaService;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
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

    if (!CollectionUtils.isEmpty(rankCloudProvidersMessage.getPlacementPolicies())) {
      this.discardOnPlacementPolicies(rankCloudProvidersMessage.getPlacementPolicies(),
          rankCloudProvidersMessage.getCloudProviders().values(),
          rankCloudProvidersMessage.getSlamPreferences().getSla(), servicesToDiscard);
    }

    discardProvidersAndServices(providersToDiscard, servicesToDiscard, rankCloudProvidersMessage);

    if (!MapUtils.isEmpty(rankCloudProvidersMessage.getOneDataRequirements())) {
      OneData inputRequirement = rankCloudProvidersMessage.getOneDataRequirements().get("input");
      discardOnOneDataRequirements(inputRequirement,
          rankCloudProvidersMessage.getCloudProviders().values(), providersToDiscard,
          servicesToDiscard);
      OneData outputRequirement = rankCloudProvidersMessage.getOneDataRequirements().get("output");
      discardOnOneDataRequirements(outputRequirement,
          rankCloudProvidersMessage.getCloudProviders().values(), providersToDiscard,
          servicesToDiscard);
    }

    discardProvidersAndServices(providersToDiscard, servicesToDiscard, rankCloudProvidersMessage);

    // Filter provider for Chronos
    // FIXME: It's just a demo hack to for Chronos jobs default provider override!!
    if (rankCloudProvidersMessage.getDeploymentType() == DeploymentType.CHRONOS
        || rankCloudProvidersMessage.getDeploymentType() == DeploymentType.MARATHON) {
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

    // Filter provider by image contextualization check
    for (CloudProvider cloudProvider : rankCloudProvidersMessage.getCloudProviders().values()) {
      for (CloudService cloudService : cloudProvider.getCmbdProviderServicesByType(Type.COMPUTE)) {
        try {
          toscaService.contextualizeImages(ar, cloudProvider, cloudService.getId());
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

  private void discardOnPlacementPolicies(List<PlacementPolicy> placementPolicies,
      Collection<CloudProvider> cloudProviders, List<Sla> slas,
      Set<CloudService> servicesToDiscard) {
    if (placementPolicies.size() != 1) {
      throw new OrchestratorException("Only single placement policies are supported");
    }
    if (!(placementPolicies.get(0) instanceof SlaPlacementPolicy)) {
      throw new OrchestratorException("Only SLA placement policies are supported");
    }
    final SlaPlacementPolicy slaPlacementPolicy = (SlaPlacementPolicy) placementPolicies.get(0);

    Sla selectedSla = slas.stream()
        .filter(sla -> Objects.equals(sla.getId(), slaPlacementPolicy.getSlaId()))
        .findFirst()
        .orElseThrow(() -> new OrchestratorException(
            String.format("No SLA with id %s available", slaPlacementPolicy.getSlaId())));

    for (CloudProvider cloudProvider : cloudProviders) {
      for (CloudService cloudService : cloudProvider.getCmbdProviderServicesByType(Type.COMPUTE)) {
        if (!(Objects.equals(selectedSla.getService().getServiceId(), cloudService.getId()))) {
          addServiceToDiscard(servicesToDiscard, cloudService);
        } else {
          // TODO change this
          if (cloudService.isAwsComputeProviderService()
              && slaPlacementPolicy instanceof CredentialsAwareSlaPlacementPolicy) {
            placementPolicies.set(0,
                new AwsSlaPlacementPolicy((CredentialsAwareSlaPlacementPolicy) slaPlacementPolicy));
          }
        }
      }
    }
  }

  private void discardOnOneDataRequirements(OneData requirement,
      Collection<CloudProvider> cloudProviders, Set<CloudProvider> providersToDiscard,
      Set<CloudService> servicesToDiscard) {
    if (requirement != null && requirement.isSmartScheduling()) {
      for (CloudProvider cloudProvider : cloudProviders) {
        boolean hasOneProviderSupportingSpace = false;
        for (CloudService cloudService : cloudProvider.getCmdbProviderServices().values()) {
          if (!cloudService.isOneProviderStorageService()) {
            continue;
          } else {
            for (OneDataProviderInfo providerInfo : requirement.getProviders()) {
              if (Objects.equals(providerInfo.getId(), cloudService.getData().getEndpoint())) {
                hasOneProviderSupportingSpace = true;
                providerInfo.setCloudProviderId(cloudProvider.getId());
                providerInfo.setCloudServiceId(cloudService.getId());
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
        if (Objects.equals(providerToDiscard.getId(), sla.getCloudProviderId())) {
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
    providersToDiscard.clear();
    servicesToDiscard.clear();
  }

  @Override
  protected String getErrorMessagePrefix() {
    return "Error filtering Cloud Providers";
  }
}
