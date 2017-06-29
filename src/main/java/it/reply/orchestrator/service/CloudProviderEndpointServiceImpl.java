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

package it.reply.orchestrator.service;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.CloudProviderEndpoint.IaaSType;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.Type;
import it.reply.orchestrator.dto.deployment.AwsSlaPlacementPolicy;
import it.reply.orchestrator.dto.deployment.CredentialsAwareSlaPlacementPolicy;
import it.reply.orchestrator.dto.deployment.PlacementPolicy;
import it.reply.orchestrator.dto.ranker.RankedCloudProvider;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.DeploymentType;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.exception.service.DeploymentException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class CloudProviderEndpointServiceImpl {

  /**
   * Choose a Cloud Provider.
   * 
   * @param deployment
   *          .
   * @param rankCloudProvidersMessage
   *          .
   * @return .
   */
  public RankedCloudProvider chooseCloudProvider(Deployment deployment,
      RankCloudProvidersMessage rankCloudProvidersMessage) {

    final RankedCloudProvider chosenCp =
        rankCloudProvidersMessage
            .getRankedCloudProviders()
            .stream()
            .filter(Objects::nonNull)
            // Choose the one ranked
            .filter(RankedCloudProvider::isRanked)
            // and with the highest rank
            .sorted(Comparator.comparing(RankedCloudProvider::getRank).reversed())
            .findFirst()
            .orElseThrow(() -> {
              String errorMsg = "No Cloud Provider suitable for deploy found";
              LOG.error("{}\n ranked providers list: {}", errorMsg,
                  rankCloudProvidersMessage.getRankedCloudProviders());
              return new DeploymentException(errorMsg);
            });

    LOG.debug("Selected Cloud Provider is: {}", chosenCp);
    return chosenCp;

  }

  /**
   * .
   * 
   * @param chosenCloudProvider
   *          .
   * @return .
   */
  public CloudProviderEndpoint getCloudProviderEndpoint(CloudProvider chosenCloudProvider,
      List<PlacementPolicy> placementPolicies, boolean isHybrid) {

    if (chosenCloudProvider.getCmbdProviderServicesByType(Type.COMPUTE).isEmpty()) {
      throw new IllegalArgumentException(
          "No compute Service Available for Cloud Provider : " + chosenCloudProvider);
    }

    CloudService computeService =
        chosenCloudProvider.getCmbdProviderServicesByType(Type.COMPUTE).get(0);

    String imEndpoint = null;
    CloudProviderEndpoint cpe = new CloudProviderEndpoint();

    ///////////////////////////////
    // TODO Improve and move somewhere else
    placementPolicies.stream()
        .filter(CredentialsAwareSlaPlacementPolicy.class::isInstance)
        .map(CredentialsAwareSlaPlacementPolicy.class::cast)
        .findFirst()
        .ifPresent(policy -> {
          cpe.setUsername(policy.getUsername());
          cpe.setPassword(policy.getPassword());
        });
    ///////////////////////////////

    IaaSType iaasType;
    if (computeService.isOpenStackComputeProviderService()) {
      iaasType = IaaSType.OPENSTACK;
    } else if (computeService.isOpenNebulaComputeProviderService()) {
      iaasType = IaaSType.OPENNEBULA;
    } else if (computeService.isOpenNebulaToscaProviderService()) {
      iaasType = IaaSType.OPENNEBULA;
      imEndpoint = computeService.getData().getEndpoint();
    } else if (computeService.isOcciComputeProviderService()) {
      iaasType = IaaSType.OCCI;
    } else if (computeService.isAwsComputeProviderService()) {
      iaasType = IaaSType.AWS;
      // TODO support multiple policies
      // TODO do a match between sla and service id
      AwsSlaPlacementPolicy placementPolicy = placementPolicies.stream()
          .filter(AwsSlaPlacementPolicy.class::isInstance)
          .map(AwsSlaPlacementPolicy.class::cast)
          .findFirst()
          .orElseThrow(() -> new OrchestratorException("No AWS credentials provided"));
      cpe.setUsername(placementPolicy.getAccessKey());
      cpe.setPassword(placementPolicy.getSecretKey());
    } else {
      throw new IllegalArgumentException("Unknown Cloud Provider type: " + computeService);
    }

    cpe.setCpEndpoint(computeService.getData().getEndpoint());
    cpe.setCpComputeServiceId(computeService.getId());
    cpe.setIaasType(iaasType);

    if (isHybrid) {
      // generate and set IM iaasHeaderId
      cpe.setIaasHeaderId(computeService.getId());
      // default to PaaS Level IM
      cpe.setImEndpoint(null);
    } else {
      cpe.setImEndpoint(imEndpoint);
    }
    return cpe;
  }

  /**
   * Infer the deployment provider from the deployment type and the cloud provider.
   * 
   * @param deploymentType
   *          the deployment type
   * @param cloudProvider
   *          the cloud provider
   * @return the deployment provider
   */
  public DeploymentProvider getDeploymentProvider(DeploymentType deploymentType,
      CloudProvider cloudProvider) {
    switch (deploymentType) {
      case CHRONOS:
        return DeploymentProvider.CHRONOS;
      case MARATHON:
        return DeploymentProvider.MARATHON;
      case TOSCA:
        return DeploymentProvider.IM;
      default:
        throw new DeploymentException("Unknown DeploymentType: " + deploymentType.toString());
    }
  }

}
