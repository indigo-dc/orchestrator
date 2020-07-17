/*
 * Copyright © 2015-2020 Santer Reply S.p.A.
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

package it.reply.orchestrator.service;

import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.CloudProviderEndpoint.CloudProviderEndpointBuilder;
import it.reply.orchestrator.dto.CloudProviderEndpoint.IaaSType;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.cmdb.CloudProvider;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.StorageService;
import it.reply.orchestrator.dto.policies.ToscaPolicy;
import it.reply.orchestrator.dto.ranker.RankedCloudService;
import it.reply.orchestrator.dto.workflow.CloudServiceWf;
import it.reply.orchestrator.dto.workflow.CloudServicesOrderedIterator;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.DeploymentType;
import it.reply.orchestrator.exception.service.DeploymentException;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CloudProviderEndpointServiceImpl {

  /**
   * Generates the {@link CloudServicesOrderedIterator}.
   *
   * @param rankCloudProvidersMessage
   *     the rankCloudProvidersMessage
   * @param maxProvidersRetry
   *     max num of cloud providers on which iterate
   * @return the iterator
   */
  public CloudServicesOrderedIterator generateCloudProvidersOrderedIterator(
      RankCloudProvidersMessage rankCloudProvidersMessage, Integer maxProvidersRetry) {

    Map<String, CloudService> cloudServices = rankCloudProvidersMessage
        .getCloudProviders()
        .values()
        .stream()
        .map(CloudProvider::getServices)
        .map(Map::values)
        .flatMap(Collection::stream)
        .collect(Collectors.toMap(CloudService::getId, Function.identity()));

    Stream<CloudServiceWf> orderedCloudServices =
        rankCloudProvidersMessage
            .getRankedCloudServices()
            .stream()
            // Choose the ones ranked
            .filter(RankedCloudService::isRanked)
            // and with the best rank
            .sorted(Comparator.comparing(RankedCloudService::getRank))
            .map(RankedCloudService::getServiceId)
            .map(cloudServices::get)
            .filter(Objects::nonNull)
            .map(cloudService -> {
              CloudServiceWf serviceWf = new CloudServiceWf(cloudService);
              rankCloudProvidersMessage
                  .getCloudProviders()
                  .get(cloudService.getProviderId())
                  .getServicesOfType(StorageService.class)
                  .stream()
                  .map(StorageService::getRucioRse)
                  .filter(Objects::nonNull)
                  .findFirst()
                  .ifPresent(serviceWf::setRucioRse);
              return serviceWf;
            });
    if (maxProvidersRetry != null) {
      orderedCloudServices = orderedCloudServices.limit(maxProvidersRetry);
    }

    return new CloudServicesOrderedIterator(orderedCloudServices.collect(Collectors.toList()));
  }

  /**
   * Generates the {@link CloudProviderEndpoint}.
   *
   * @param computeService
   *     the chosen {@link CloudService}
   * @param placementPolicies
   *     the placementPolicies
   * @param isHybrid
   *     true if the deployment id hybrid
   * @return the {@link CloudProviderEndpoint}
   */
  public CloudProviderEndpoint getCloudProviderEndpoint(CloudService computeService,
      Map<String, ToscaPolicy> placementPolicies, boolean isHybrid) {

    String imEndpoint = null;
    CloudProviderEndpointBuilder cpe = CloudProviderEndpoint.builder();

    final IaaSType iaasType;
    if (computeService.isOpenStackComputeProviderService()) {
      iaasType = IaaSType.OPENSTACK;
    } else if (computeService.isOpenNebulaComputeProviderService()) {
      iaasType = IaaSType.OPENNEBULA;
    } else if (computeService.isOpenNebulaToscaProviderService()) {
      iaasType = IaaSType.OPENNEBULA;
      imEndpoint = computeService.getEndpoint();
    } else if (computeService.isOcciComputeProviderService()) {
      iaasType = IaaSType.OCCI;
    } else if (computeService.isAwsComputeProviderService()) {
      iaasType = IaaSType.AWS;
    } else if (computeService.isOtcComputeProviderService()) {
      iaasType = IaaSType.OTC;
    } else if (computeService.isAzureComputeProviderService()) {
      iaasType = IaaSType.AZURE;
    } else if (computeService.isChronosComputeProviderService()) {
      iaasType = IaaSType.CHRONOS;
    } else if (computeService.isMarathonComputeProviderService()) {
      iaasType = IaaSType.MARATHON;
    } else if (computeService.isQcgComputeProviderService()) {
      iaasType = IaaSType.QCG;
    } else {
      throw new IllegalArgumentException("Unknown Cloud Provider type: " + computeService);
    }

    cpe.cpEndpoint(computeService.getEndpoint());
    cpe.cpComputeServiceId(computeService.getId());
    cpe.region(computeService.getRegion());
    cpe.iaasType(iaasType);
    cpe.imEndpoint(imEndpoint);
    cpe.iamEnabled(computeService.isIamEnabled());
    cpe.idpProtocol(computeService.getIdpProtocol());

    if (isHybrid) {
      // generate and set IM iaasHeaderId
      cpe.iaasHeaderId(computeService.getId());
      // default to PaaS Level IM
      cpe.imEndpoint(null);
    }

    return cpe.build();
  }

  /**
   * Infer the deployment provider from the deployment type and the cloud provider.
   *
   * @param deploymentType
   *     the deployment type
   * @param cloudService
   *     the cloud service
   * @return the deployment provider
   */
  public DeploymentProvider getDeploymentProvider(DeploymentType deploymentType,
      CloudService cloudService) {
    switch (deploymentType) {
      case CHRONOS:
        return DeploymentProvider.CHRONOS;
      case MARATHON:
        return DeploymentProvider.MARATHON;
      case QCG:
        return DeploymentProvider.QCG;
      case TOSCA:
        return DeploymentProvider.IM;
      default:
        throw new DeploymentException("Unknown DeploymentType: " + deploymentType.toString());
    }
  }

}
