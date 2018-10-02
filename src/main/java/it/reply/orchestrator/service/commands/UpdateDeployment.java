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

import it.reply.orchestrator.config.properties.OneDataProperties;
import it.reply.orchestrator.config.properties.OneDataProperties.ServiceSpaceProperties;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.workflow.CloudProvidersOrderedIterator;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.exception.service.BusinessWorkflowException;
import it.reply.orchestrator.service.CloudProviderEndpointServiceImpl;
import it.reply.orchestrator.utils.WorkflowConstants;
import it.reply.orchestrator.utils.WorkflowConstants.ErrorCode;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.MapUtils;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Choose Cloud Provider and update Deployment/Message with the selected one data.
 * 
 * @author l.biava
 *
 */
@Component(WorkflowConstants.Delegate.UPDATE_DEPLOYMENT)
@Slf4j
public class UpdateDeployment extends BaseDeployCommand {

  @Autowired
  private OneDataProperties oneDataProperties;

  @Autowired
  private CloudProviderEndpointServiceImpl cloudProviderEndpointService;

  @Override
  public void execute(DelegateExecution execution, DeploymentMessage deploymentMessage) {

    RankCloudProvidersMessage rankCloudProvidersMessage =
        getRequiredParameter(execution, WorkflowConstants.Param.RANK_CLOUD_PROVIDERS_MESSAGE,
            RankCloudProvidersMessage.class);

    CloudProvidersOrderedIterator cloudProvidersOrderedIterator = deploymentMessage
        .getCloudProvidersOrderedIterator();
    if (cloudProvidersOrderedIterator == null) {
      cloudProvidersOrderedIterator = cloudProviderEndpointService
          .generateCloudProvidersOrderedIterator(rankCloudProvidersMessage,
              deploymentMessage.getMaxProvidersRetry());
      deploymentMessage.setCloudProvidersOrderedIterator(cloudProvidersOrderedIterator);
    }
    if (!cloudProvidersOrderedIterator.hasNext()) {
      if (cloudProvidersOrderedIterator.getSize() == 0) {
        throw new BusinessWorkflowException(ErrorCode.RUNTIME_ERROR,
            "No cloud providers available to deploy");
      } else {
        throw new BusinessWorkflowException(ErrorCode.RUNTIME_ERROR,
            "Retries on cloud providers exhausted");
      }
    }
    CloudProvider currentCloudProvider = cloudProvidersOrderedIterator.next();
    Deployment deployment = getDeployment(deploymentMessage);

    boolean isUpdate = Status.UPDATE_IN_PROGRESS == deployment.getStatus();

    // Update Deployment
    if (!isUpdate) {
      deployment.setCloudProviderName(currentCloudProvider.getId());
    }
    // FIXME Set/update all required selected CP data

    // FIXME Generate CP Endpoint
    CloudProviderEndpoint chosenCloudProviderEndpoint = cloudProviderEndpointService
        .getCloudProviderEndpoint(currentCloudProvider,
            rankCloudProvidersMessage.getPlacementPolicies(), deploymentMessage.isHybrid());
    deploymentMessage.setChosenCloudProviderEndpoint(chosenCloudProviderEndpoint);
    LOG.debug("Generated Cloud Provider Endpoint is: {}", chosenCloudProviderEndpoint);

    // FIXME Use another method to hold CP Endpoint (i.e. CMDB service ID reference?)
    // Save CPE in Deployment for future use
    if (!isUpdate) { // create
      deployment.setCloudProviderEndpoint(chosenCloudProviderEndpoint);
    } else {
      Optional<String> iaasheaderId = chosenCloudProviderEndpoint.getIaasHeaderId();
      if (deploymentMessage.isHybrid() && iaasheaderId.isPresent()) { // hybrid update
        deployment
            .getCloudProviderEndpoint()
            .getHybridCloudProviderEndpoints()
            .put(iaasheaderId.get(), chosenCloudProviderEndpoint);
      }
    }

    DeploymentProvider deploymentProvider = cloudProviderEndpointService
        .getDeploymentProvider(deploymentMessage.getDeploymentType(), currentCloudProvider);
    deployment.setDeploymentProvider(deploymentProvider);

    // FIXME Implement OneData scheduling properly and move in a dedicated command
    deploymentMessage.setOneDataParameters(generateOneDataParameters(rankCloudProvidersMessage,
        currentCloudProvider, deploymentMessage.getDeploymentId()));
  }

  protected Map<String, OneData> generateOneDataParameters(
      RankCloudProvidersMessage rankCloudProvidersMessage,
      CloudProvider chosenCloudProvider, String deploymentId) {
    // Just copy requirements to parameters (in the future the Orchestrator will need to edit I/O
    // providers, but not for now)
    // deploymentMessage.getOneDataParameters().putAll(deploymentMessage.getOneDataRequirements());
    Map<String, OneData> oneDataParameters = new HashMap<>();
    // No Requirements -> Service space
    if (MapUtils.isEmpty(rankCloudProvidersMessage.getOneDataRequirements())) {
      oneDataParameters.put("service", generateStubOneData(deploymentId));
      LOG.warn("GENERATING STUB ONE DATA FOR SERVICE"
          + " (remove once OneData parameters generation is completed!)");
    } else {
      LOG.debug("User specified I/O OneData requirements; service space will not be generated.");
      Map<String, OneData> oneDataRequirements = rankCloudProvidersMessage.getOneDataRequirements();
      Optional
          .ofNullable(oneDataRequirements.get("input"))
          .ifPresent(
              oneDataRequirement -> {
                OneData oneDataParameter = oneDataRequirement.clone();
                if (oneDataParameter.isSmartScheduling()) {
                  oneDataParameter.setProviders(oneDataParameter.getProviders()
                      .stream()
                      .filter(info -> Objects
                          .equals(info.getCloudProviderId(), chosenCloudProvider.getId()))
                      .collect(Collectors.toList()));
                }
                oneDataParameters.put("input", oneDataParameter);
              });
      Optional
          .ofNullable(oneDataRequirements.get("output"))
          .ifPresent(
              oneDataRequirement -> {
                OneData oneDataParameter = oneDataRequirement.clone();
                if (oneDataParameter.isSmartScheduling()) {
                  oneDataParameter.setProviders(oneDataParameter.getProviders()
                      .stream()
                      .filter(info -> Objects
                          .equals(info.getCloudProviderId(), chosenCloudProvider.getId()))
                      .collect(Collectors.toList()));
                }
                oneDataParameters.put("output", oneDataParameter);
              });
    }
    return oneDataParameters;
  }

  /**
   * Temporary method to generate default OneData settings.
   * 
   * @return the {@link OneData} settings.
   */
  protected OneData generateStubOneData(String deploymentId) {

    ServiceSpaceProperties serviceSpaceProperties = oneDataProperties.getServiceSpace();

    String path = new StringBuilder().append(serviceSpaceProperties.getBaseFolderPath())
        .append(deploymentId)
        .toString();

    OneData stub = OneData.builder()
        .token(serviceSpaceProperties.getToken())
        .space(serviceSpaceProperties.getName())
        .path(path)
        .providersAsString(serviceSpaceProperties.getOneproviderUrl().toString())
        .build();

    LOG.info("Generated OneData settings {}", stub);
    return stub;
  }

  @Override
  protected String getErrorMessagePrefix() {
    return "Error setting selected cloud providers";
  }

}
