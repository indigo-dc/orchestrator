/*
 * Copyright © 2015-2018 Santer Reply S.p.A.
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

import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.tosca.model.ArchiveRoot;

import it.reply.orchestrator.config.properties.MesosProperties;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.ImageData;
import it.reply.orchestrator.dto.cmdb.Type;
import it.reply.orchestrator.dto.deployment.PlacementPolicy;
import it.reply.orchestrator.dto.deployment.SlaPlacementPolicy;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.onedata.OneData.OneDataProviderInfo;
import it.reply.orchestrator.dto.slam.Service;
import it.reply.orchestrator.dto.slam.Sla;
import it.reply.orchestrator.enums.DeploymentType;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.service.ToscaService;
import it.reply.orchestrator.utils.WorkflowConstants;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.MapUtils;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(WorkflowConstants.Delegate.PREFILTER_CLOUD_PROVIDERS)
@Slf4j
public class PrefilterCloudProviders extends BaseRankCloudProvidersCommand {

  @Autowired
  private MesosProperties mesosProperties;

  @Autowired
  private ToscaService toscaService;

  @Override
  public void execute(DelegateExecution execution,
      RankCloudProvidersMessage rankCloudProvidersMessage) {
    // TODO Filter cloud providers (i.e. based on OneData)

    Set<CloudProvider> providersToDiscard = new HashSet<>();
    Set<CloudService> servicesToDiscard = new HashSet<>();

    if (!rankCloudProvidersMessage.getPlacementPolicies().isEmpty()) {
      discardOnPlacementPolicies(rankCloudProvidersMessage.getPlacementPolicies(),
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

    if (DeploymentType.isMesosDeployment(rankCloudProvidersMessage.getDeploymentType())) {
      rankCloudProvidersMessage
          .getCloudProviders()
          .values()
          .stream()
          .filter(cloudProvider -> !mesosProperties.getInstance(cloudProvider.getId()).isPresent())
          .forEach(cloudProvider -> {
            LOG.debug("Discarded provider {} because it doesn't have any registered Mesos instance",
                cloudProvider.getId());
            addProviderToDiscard(providersToDiscard, servicesToDiscard, cloudProvider);
          });
    }

    discardProvidersAndServices(providersToDiscard, servicesToDiscard, rankCloudProvidersMessage);

    Deployment deployment = getDeployment(rankCloudProvidersMessage);
    
    // Filter out providers that do not support the requested images
    ArchiveRoot ar =
        toscaService.prepareTemplate(deployment.getTemplate(), deployment.getParameters());
    
    // Filter provider by image contextualization check
    rankCloudProvidersMessage.getCloudProviders().values().forEach(cloudProvider -> {
      cloudProvider.getCmbdProviderServicesByType(Type.COMPUTE).forEach(cloudService -> {
        Map<Boolean, Map<NodeTemplate, ImageData>> contextualizedImages =
            toscaService.contextualizeImages(ar, cloudProvider, cloudService.getId());
        if (!contextualizedImages.get(Boolean.FALSE).isEmpty()) {
          // Failed to match all required images -> discard provider
          LOG.debug(
              "Discarded service {} of provider {} because it doesn't match images requirements"
                  + " for deployment {}",
              cloudService.getId(), cloudProvider.getId(), deployment.getId());
          addServiceToDiscard(servicesToDiscard, cloudService);
          cloudProvider.getCmdbProviderImages().remove(cloudService.getId());
        }
      });
    });

    discardProvidersAndServices(providersToDiscard, servicesToDiscard, rankCloudProvidersMessage);
  }

  private void discardOnPlacementPolicies(Map<String, PlacementPolicy> placementPolicies,
      Collection<CloudProvider> cloudProviders, List<Sla> slas,
      Set<CloudService> servicesToDiscard) {
    
    if (placementPolicies.size() != 1) {
      //TODO relax this constraint
      throw new OrchestratorException("Only a single placement policy is supported");
    }

    placementPolicies.forEach((name, placementPolicy) -> {
      if (placementPolicy instanceof SlaPlacementPolicy) {
        final SlaPlacementPolicy slaPlacementPolicy = (SlaPlacementPolicy) placementPolicy;
        Sla selectedSla = slas
            .stream()
            .filter(sla -> Objects.equals(sla.getId(), slaPlacementPolicy.getSlaId()))
            .findFirst()
            .orElseThrow(() -> new OrchestratorException(
                String.format("No SLA with id %s available", slaPlacementPolicy.getSlaId())));

        slaPlacementPolicy.setServiceIds(selectedSla
            .getServices()
            .stream()
            .map(Service::getServiceId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList()));

        cloudProviders.forEach(cloudProvider -> {
          cloudProvider
              .getCmbdProviderServicesByType(Type.COMPUTE)
              .forEach(cloudService -> {
                boolean serviceIsInSlaPolicy = slaPlacementPolicy
                    .getServicesId()
                    .stream()
                    .anyMatch(serviceId -> serviceId.equals(cloudService.getId()));
                if (!serviceIsInSlaPolicy) {
                  addServiceToDiscard(servicesToDiscard, cloudService);
                }
              });
        });
      } else {
        throw new OrchestratorException("Only SLA placement policies are supported");
      }
    });
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
    providerEntry
        .getCmdbProviderServices()
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
      cloudProvider
          .getCmbdProviderServicesByType(Type.COMPUTE)
          .stream()
          .filter(computeService -> servicesToDiscard.contains(computeService))
          .forEach(computeServiceToDiscard -> cloudProvider
              .getCmdbProviderServices()
              .remove(computeServiceToDiscard.getId()));
      if (cloudProvider.getCmbdProviderServicesByType(Type.COMPUTE).isEmpty()) {
        addProviderToDiscard(providersToDiscard, servicesToDiscard, cloudProvider);
      }
    }

    // Remove discarded provider and services from SLAs and Preferences
    for (CloudProvider providerToDiscard : providersToDiscard) {
      rankCloudProvidersMessage.getCloudProviders().remove(providerToDiscard.getId());
      rankCloudProvidersMessage.getSlamPreferences().getSla().removeIf(
          sla -> Objects.equals(providerToDiscard.getId(), sla.getCloudProviderId()));
    }

    // for each cloudService to discard
    servicesToDiscard.forEach(cloudService -> {

      // remove all the preferences from rankCloudProvidersMessage with no preferenceCustomer
      rankCloudProvidersMessage.getSlamPreferences().getPreferences().removeIf(preference -> {

        // remove all the preferenceCustomer with no priorities
        preference.getPreferences().removeIf(preferenceCustomer -> {

          // remove all the priority with the id of the cloud service to remove
          preferenceCustomer.getPriority().removeIf(priority -> {
            return Objects.equals(cloudService.getId(), priority.getServiceId());
          });

          return preferenceCustomer.getPriority().isEmpty();
        });

        return preference.getPreferences().isEmpty();

      });

    });
    providersToDiscard.clear();
    servicesToDiscard.clear();
  }

  @Override
  protected String getErrorMessagePrefix() {
    return "Error filtering Cloud Providers";
  }
}
