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

import alien4cloud.tosca.model.ArchiveRoot;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.cmdb.ChronosServiceData;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.MarathonServiceData;
import it.reply.orchestrator.dto.cmdb.MesosFrameworkServiceData;
import it.reply.orchestrator.dto.cmdb.Type;
import it.reply.orchestrator.dto.deployment.PlacementPolicy;
import it.reply.orchestrator.dto.deployment.SlaPlacementPolicy;
import it.reply.orchestrator.dto.dynafed.Dynafed;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.slam.Service;
import it.reply.orchestrator.dto.slam.Sla;
import it.reply.orchestrator.enums.DeploymentType;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.exception.service.DeploymentException;
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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(WorkflowConstants.Delegate.PREFILTER_CLOUD_PROVIDERS)
@Slf4j
public class PrefilterCloudProviders extends BaseRankCloudProvidersCommand {

  @Autowired
  private ToscaService toscaService;

  @Override
  public void execute(DelegateExecution execution,
      RankCloudProvidersMessage rankCloudProvidersMessage) {
    // TODO Filter cloud providers (i.e. based on OneData)

    Set<CloudProvider> providersToDiscard = new HashSet<>();
    Set<CloudService> servicesToDiscard = new HashSet<>();

    discardOnPlacementPolicies(rankCloudProvidersMessage.getPlacementPolicies(),
        rankCloudProvidersMessage.getCloudProviders().values(),
        rankCloudProvidersMessage.getSlamPreferences().getSla(), servicesToDiscard);

    discardProvidersAndServices(providersToDiscard, servicesToDiscard, rankCloudProvidersMessage);

    rankCloudProvidersMessage
        .getOneDataRequirements()
        .values()
        .forEach(ondedataRequirement -> {
          discardOnOneDataRequirements(ondedataRequirement,
              rankCloudProvidersMessage.getCloudProviders().values(), providersToDiscard,
              servicesToDiscard);
          discardProvidersAndServices(providersToDiscard, servicesToDiscard,
              rankCloudProvidersMessage);
        });

    rankCloudProvidersMessage
        .getDynafedRequirements()
        .values()
        .forEach(dyanfedRequirement -> {
          discardOnDynafedRequirements(dyanfedRequirement,
              rankCloudProvidersMessage.getCloudProviders().values(), providersToDiscard,
              servicesToDiscard);
          discardProvidersAndServices(providersToDiscard, servicesToDiscard,
              rankCloudProvidersMessage);
        });

    Deployment deployment = getDeployment(rankCloudProvidersMessage);
    ArchiveRoot ar = toscaService
        .prepareTemplate(deployment.getTemplate(), deployment.getParameters());

    rankCloudProvidersMessage
        .getCloudProviders()
        .forEach((name, cloudProvider) -> {
          cloudProvider
              .getCmdbProviderServices()
              .forEach((id, cloudProviderService) -> {
                DeploymentType type = rankCloudProvidersMessage.getDeploymentType();
                switch (type) {
                  case TOSCA:
                    if (cloudProviderService.getData() instanceof MesosFrameworkServiceData) {
                      addServiceToDiscard(servicesToDiscard, cloudProviderService);
                    }
                    break;
                  case MARATHON:
                    if (!(cloudProviderService.getData() instanceof MarathonServiceData)) {
                      addServiceToDiscard(servicesToDiscard, cloudProviderService);
                    } else {
                      discardOnMesosGpuRequirement(ar, cloudProviderService, servicesToDiscard);
                    }
                    break;
                  case CHRONOS:
                    if (!(cloudProviderService.getData() instanceof ChronosServiceData)) {
                      addServiceToDiscard(servicesToDiscard, cloudProviderService);
                    } else {
                      discardOnMesosGpuRequirement(ar, cloudProviderService, servicesToDiscard);
                    }
                    break;
                  default:
                    throw new DeploymentException("Unknown Deployment Type: " + type);
                }
              });
        });

    discardProvidersAndServices(providersToDiscard, servicesToDiscard, rankCloudProvidersMessage);

    // Filter out providers that do not support the requested images

    // Filter provider by image contextualization check
    rankCloudProvidersMessage
        .getCloudProviders()
        .forEach((cloudProviderName, cloudProvider) -> {
          cloudProvider
              .getCmbdProviderServicesByType(Type.COMPUTE)
              .forEach(cloudService -> {
                boolean hasMatchingImages = toscaService
                    .contextualizeImages(ar, cloudProvider, cloudService.getId())
                    .get(Boolean.FALSE)
                    .isEmpty();
                if (!hasMatchingImages) {
                  // Failed to match all required images -> discard provider
                  LOG.debug(
                      "Discarded service {} of provider {} {}", cloudService.getId(),
                      cloudProvider.getId(), "because it doesn't match images requirements");
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

    if (placementPolicies.size() > 1) {
      //TODO relax this constraint
      throw new OrchestratorException("Only a single placement policy is supported");
    }

    List<SlaPlacementPolicy> slaPlacementPolicies = placementPolicies
        .values()
        .stream()
        .map(placementPolicy -> {
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
            return slaPlacementPolicy;
          } else {
            throw new OrchestratorException("Only SLA placement policies are supported");
          }
        }).collect(Collectors.toList());

    boolean slaPlacementRequired = !placementPolicies.isEmpty();

    cloudProviders
        .forEach(cloudProvider -> {
          cloudProvider
              .getCmbdProviderServicesByType(Type.COMPUTE)
              .forEach(cloudService -> {
                boolean serviceIsInSlaPolicy = slaPlacementPolicies
                    .stream()
                    .flatMap(policy -> policy.getServicesId().stream())
                    .anyMatch(serviceId -> serviceId.equals(cloudService.getId()));
                boolean credentialsRequired = cloudService.isCredentialsRequired();
                if (!serviceIsInSlaPolicy && (slaPlacementRequired || credentialsRequired)) {
                  LOG.debug(
                      "Discarded service {} of provider {} because it doesn't match SLA policies",
                      cloudService.getId(), cloudProvider.getId());
                  addServiceToDiscard(servicesToDiscard, cloudService);
                }
              });
        });
  }

  protected void discardOnMesosGpuRequirement(ArchiveRoot archiveRoot,
      CloudService mesosFrameworkService,
      Set<CloudService> servicesToDiscard) {
    boolean requiresGpu = toscaService.isMesosGpuRequired(archiveRoot);
    if (requiresGpu && !((MesosFrameworkServiceData) mesosFrameworkService.getData())
        .getProperties().isGpuSupport()) {
      LOG.debug(
          "Discarded Mesos framework service {} of provider {} because it doesn't support GPUs",
          mesosFrameworkService.getId(), mesosFrameworkService.getData().getProviderId());
      addServiceToDiscard(servicesToDiscard, mesosFrameworkService);
    }
  }

  protected void discardOnOneDataRequirements(OneData requirement,
      Collection<CloudProvider> cloudProviders, Set<CloudProvider> providersToDiscard,
      Set<CloudService> servicesToDiscard) {
    if (requirement.isSmartScheduling()) {
      cloudProviders.forEach(cloudProvider -> {
        boolean hasOneProviderSupportingSpace = requirement
            .getOneproviders()
            .stream()
            .anyMatch(providerInfo -> cloudProvider
                .getId()
                .equals(providerInfo.getCloudProviderId()));
        if (!hasOneProviderSupportingSpace) {
          LOG.debug(
              "Discarded provider {} because it doesn't have any oneProvider supporting space {}",
              cloudProvider.getId(), requirement.getSpace());
          addProviderToDiscard(providersToDiscard, servicesToDiscard, cloudProvider);
        }
      });
    }
  }

  protected void discardOnDynafedRequirements(@NonNull Dynafed requirement,
      @NonNull Collection<CloudProvider> cloudProviders,
      @NonNull Set<CloudProvider> providersToDiscard,
      @NonNull Set<CloudService> servicesToDiscard) {

    cloudProviders.forEach(cloudProvider -> {
      boolean supportsAllFiles = requirement
          .getFiles()
          .stream()
          .allMatch(file -> file
              .getResources()
              .stream()
              .anyMatch(resource -> cloudProvider.getId().equals(resource.getCloudProviderId())));
      if (!supportsAllFiles) {
        LOG.debug("Discarded provider {} {}", cloudProvider.getId(),
            "because it doesn't have any storage provider supporting the dynafed requirements");
        addProviderToDiscard(providersToDiscard, servicesToDiscard, cloudProvider);
      }
    });
  }

  protected void addProviderToDiscard(@NonNull Set<CloudProvider> providersToDiscard,
      @NonNull Set<CloudService> servicesToDiscard, @NonNull CloudProvider providerEntry) {
    providersToDiscard.add(providerEntry);
    providerEntry
        .getCmdbProviderServices()
        .forEach((key, value) -> addServiceToDiscard(servicesToDiscard, value));
  }

  protected void addServiceToDiscard(@NonNull Set<CloudService> servicesToDiscard,
      @NonNull CloudService csToDiscard) {
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
        LOG.debug("Discarded provider {} {}", cloudProvider.getId(),
            "because it doesn't have any compute service matching the deployment requirements");
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
