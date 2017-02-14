package it.reply.orchestrator.service;

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

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.CloudProviderEndpoint.IaaSType;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.Type;
import it.reply.orchestrator.dto.deployment.AwsSlaPlacementPolicy;
import it.reply.orchestrator.dto.deployment.PlacementPolicy;
import it.reply.orchestrator.dto.ranker.RankedCloudProvider;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.exception.service.DeploymentException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CloudProviderEndpointServiceImpl {
  private static final Logger LOG = LoggerFactory.getLogger(CloudProviderEndpointServiceImpl.class);

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

    // TODO Check ranker errors (i.e. providers with ranked = false)
    RankedCloudProvider chosenCp = null;
    for (RankedCloudProvider rcp : rankCloudProvidersMessage.getRankedCloudProviders()) {
      // Choose the one with highest rank AND that matches iaasType (TEMPORARY)
      if (chosenCp == null || rcp.getRank() > chosenCp.getRank()) {
        chosenCp = rcp;
      }
    }

    if (chosenCp == null) {
      String errorMsg = "No Cloud Provider suitable for deploy found";
      LOG.error("{}\n ranked providers list: {}", errorMsg,
          rankCloudProvidersMessage.getRankedCloudProviders());
      throw new DeploymentException(errorMsg);
    } else {
      LOG.debug("Selected Cloud Provider is: {}", chosenCp);
    }
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
      List<PlacementPolicy> placementPolicies) {

    if (chosenCloudProvider.getCmbdProviderServicesByType(Type.COMPUTE).isEmpty()) {
      throw new IllegalArgumentException(
          "No compute Service Available for Cloud Provider : " + chosenCloudProvider);
    }

    CloudService computeService =
        chosenCloudProvider.getCmbdProviderServicesByType(Type.COMPUTE).get(0);

    CloudProviderEndpoint cpe = new CloudProviderEndpoint();
    IaaSType iaasType;
    if (computeService.isOpenStackComputeProviderService()) {
      iaasType = IaaSType.OPENSTACK;
    } else if (computeService.isOpenNebulaComputeProviderService()) {
      iaasType = IaaSType.OPENNEBULA;
    } else if (computeService.isOcciComputeProviderService()) {
      iaasType = IaaSType.OCCI;
    } else if (computeService.isAwsComputeProviderService()) {
      iaasType = IaaSType.AWS;
      // TODO support multiple policies
      // TODO do a match between sla and service id
      AwsSlaPlacementPolicy placementPolicy = (AwsSlaPlacementPolicy) placementPolicies.stream()
          .filter(p -> p instanceof AwsSlaPlacementPolicy).findFirst()
          .orElseThrow(() -> new OrchestratorException("No AWS credentials provided"));
      cpe.setUsername(placementPolicy.getAccessKey());
      cpe.setPassword(placementPolicy.getSecretKey());
    } else {
      throw new IllegalArgumentException("Unknown Cloud Provider type: " + computeService);
    }

    cpe.setCpEndpoint(computeService.getData().getEndpoint());
    cpe.setCpComputeServiceId(computeService.getId());
    cpe.setIaasType(iaasType);
    // FIXME Add IM EP, if available

    return cpe;
  }

  // /**
  // * TEMPORARY method to decide whether it is needed to coerce CP choice based on template content
  // * (i.e. currently used to force Mesos cluster deployment on OpenStack).
  // *
  // * @param template
  // * .
  // * @return .
  // */
  // public IaaSType getIaaSTypeFromTosca(String template) {
  //
  // if (template.contains("tosca.nodes.indigo.MesosMaster")) {
  // return IaaSType.OPENSTACK;
  // } else {
  // return IaaSType.OPENNEBULA;
  // }
  // }

  // protected boolean isCompatible(IaaSType requiredType, IaaSType providerType) {
  // if (requiredType == providerType) {
  // return true;
  // } else if (providerType == IaaSType.OPENNEBULA) {
  // return true;
  // } else {
  // return false;
  // }
  // }

  // protected IaaSType getProviderIaaSType(RankCloudProvidersMessage rankCloudProvidersMessage,
  // String providerName) {
  // return getProviderIaaSType(rankCloudProvidersMessage.getCloudProviders().get(providerName));
  // }
  //
  // /**
  // * Get the {@link IaaSType} from the {@link CloudProvider}.
  // *
  // * @param cloudProvider
  // * the cloudProvider
  // * @return the IaaSTYpe
  // */
  // public static IaaSType getProviderIaaSType(CloudProvider cloudProvider) {
  // String serviceType =
  // cloudProvider.getCmbdProviderServiceByType(Type.COMPUTE).getData().getServiceType();
  // if (serviceType.contains("openstack")) {
  // return IaaSType.OPENSTACK;
  // }
  //
  // return IaaSType.OPENNEBULA;
  // }

}
