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

import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.tosca.model.ArchiveRoot;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.ImageData;
import it.reply.orchestrator.dto.cmdb.Type;
import it.reply.orchestrator.dto.deployment.AwsSlaPlacementPolicy;
import it.reply.orchestrator.dto.deployment.CredentialsAwareSlaPlacementPolicy;
import it.reply.orchestrator.dto.deployment.PlacementPolicy;
import it.reply.orchestrator.dto.deployment.SlaPlacementPolicy;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.onedata.OneData.OneDataProviderInfo;
import it.reply.orchestrator.dto.slam.Service;
import it.reply.orchestrator.dto.slam.Sla;
import it.reply.orchestrator.enums.DeploymentType;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.service.ToscaService;
import it.reply.orchestrator.utils.CommonUtils;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@Slf4j
public class PrefilterCloudProviders
    extends BaseRankCloudProvidersCommand<PrefilterCloudProviders> {

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
    Set<CloudProvider> providersToDiscard = new HashSet<>();
    Set<CloudService> servicesToDiscard = new HashSet<>();

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
      rankCloudProvidersMessage.getCloudProviders()
          .values()
          .stream()
          .filter(
              cloudProvider -> !cloudProvider.getId().equalsIgnoreCase(chronosCloudProviderName))
          .forEach(cloudProvider -> {
            LOG.debug(
                "Discarded provider {} because it doesn't match Chronos default provider {}"
                    + " for deployment {}",
                cloudProvider.getId(), chronosCloudProviderName, deployment.getId());
            addProviderToDiscard(providersToDiscard, servicesToDiscard, cloudProvider);
          });
    }

    discardProvidersAndServices(providersToDiscard, servicesToDiscard, rankCloudProvidersMessage);

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
        boolean serviceSlaIsCloudService = selectedSla.getService()
            .map(Service::getServiceId)
            .filter(serviceId -> CommonUtils.checkNotNull(serviceId).equals(cloudService.getId()))
            .isPresent();
        if (serviceSlaIsCloudService) {
          // TODO change this
          if (cloudService.isAwsComputeProviderService()
              && slaPlacementPolicy instanceof CredentialsAwareSlaPlacementPolicy) {
            placementPolicies.set(0,
                new AwsSlaPlacementPolicy((CredentialsAwareSlaPlacementPolicy) slaPlacementPolicy));
          }
        } else {
          addServiceToDiscard(servicesToDiscard, cloudService);
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
      cloudProvider.getCmbdProviderServicesByType(Type.COMPUTE)
          .stream()
          .filter(computeService -> servicesToDiscard.contains(computeService))
          .forEach(computeServiceToDiscard -> cloudProvider.getCmdbProviderServices()
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
