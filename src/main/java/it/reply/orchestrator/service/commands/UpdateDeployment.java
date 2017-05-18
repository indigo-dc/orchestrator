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

import com.google.common.collect.ImmutableMap;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.ranker.RankedCloudProvider;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.service.CloudProviderEndpointServiceImpl;
import it.reply.orchestrator.service.OneDataService;
import it.reply.orchestrator.service.deployment.providers.DeploymentStatusHelper;
import it.reply.orchestrator.utils.CommonUtils;
import it.reply.orchestrator.utils.WorkflowConstants;
import it.reply.workflowmanager.spring.orchestrator.bpm.ejbcommands.BaseCommand;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.MapUtils;
import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Choose Cloud Provider and update Deployment/Message with the selected one data.
 * 
 * @author l.biava
 *
 */
@Component
@Slf4j
public class UpdateDeployment extends BaseCommand {

  @Autowired
  private OneDataService oneDataService;

  @Autowired
  private DeploymentRepository deploymentRepository;

  @Autowired
  private DeploymentStatusHelper deploymentStatusHelper;

  @Autowired
  private CloudProviderEndpointServiceImpl cloudProviderEndpointServiceImpl;

  @Override
  public ExecutionResults customExecute(CommandContext ctx) throws Exception {

    RankCloudProvidersMessage rankCloudProvidersMessage =
        getParameter(ctx, WorkflowConstants.WF_PARAM_RANK_CLOUD_PROVIDERS_MESSAGE);
    if (rankCloudProvidersMessage == null) {
      throw new IllegalArgumentException(String.format("WF parameter <%s> cannot be null",
          WorkflowConstants.WF_PARAM_RANK_CLOUD_PROVIDERS_MESSAGE));
    }

    DeploymentMessage deploymentMessage =
        getParameter(ctx, WorkflowConstants.WF_PARAM_DEPLOYMENT_MESSAGE);

    ExecutionResults exResults = new ExecutionResults();
    try {
      if (deploymentMessage == null) {
        throw new IllegalArgumentException(String.format("WF parameter <%s> cannot be null",
            WorkflowConstants.WF_PARAM_DEPLOYMENT_MESSAGE));
      }

      Deployment deployment =
          deploymentRepository.findOne(rankCloudProvidersMessage.getDeploymentId());

      // Choose Cloud Provider
      RankedCloudProvider chosenCp = cloudProviderEndpointServiceImpl
          .chooseCloudProvider(deployment, rankCloudProvidersMessage);

      // Set the chosen CP in deploymentMessage
      deploymentMessage.setChosenCloudProvider(
          rankCloudProvidersMessage.getCloudProviders().get(chosenCp.getName()));

      // Update Deployment
      deployment.setCloudProviderName(chosenCp.getName());

      // FIXME Set/update all required selected CP data

      // FIXME Generate CP Endpoint
      CloudProviderEndpoint chosenCloudProviderEndpoint = cloudProviderEndpointServiceImpl
          .getCloudProviderEndpoint(deploymentMessage.getChosenCloudProvider(),
              rankCloudProvidersMessage.getPlacementPolicies());
      deploymentMessage.setChosenCloudProviderEndpoint(chosenCloudProviderEndpoint);
      LOG.debug("Generated Cloud Provider Endpoint is: {}", chosenCloudProviderEndpoint);

      // FIXME Use another method to hold CP Endpoint (i.e. CMDB service ID reference?)
      // Save CPE in Deployment for future use
      deployment.setCloudProviderEndpoint(chosenCloudProviderEndpoint);

      DeploymentProvider deploymentProvider =
          cloudProviderEndpointServiceImpl.getDeploymentProvider(
              deploymentMessage.getDeploymentType(), deploymentMessage.getChosenCloudProvider());
      deployment.setDeploymentProvider(deploymentProvider);

      // FIXME Implement OneData scheduling properly and move in a dedicated command
      generateOneDataParameters(rankCloudProvidersMessage, deploymentMessage);

      exResults.getData().putAll(resultOccurred(true).getData());
      exResults.setData(WorkflowConstants.WF_PARAM_DEPLOYMENT_MESSAGE, deploymentMessage);
    } catch (Exception ex) {
      LOG.error("Error setting cloud selected providers", ex);
      exResults.getData().putAll(resultOccurred(false).getData());

      // Update deployment with error
      // TODO: what if this fails??
      deploymentStatusHelper.updateOnError(rankCloudProvidersMessage.getDeploymentId(), ex);
    }

    return exResults;
  }

  protected void generateOneDataParameters(RankCloudProvidersMessage rankCloudProvidersMessage,
      DeploymentMessage deploymentMessage) {
    // Just copy requirements to parameters (in the future the Orchestrator will need to edit I/O
    // providers, but not for now)
    // deploymentMessage.getOneDataParameters().putAll(deploymentMessage.getOneDataRequirements());

    // No Requirements -> Service space
    if (MapUtils.isEmpty(deploymentMessage.getOneDataRequirements())) {
      deploymentMessage.setOneDataParameters(CommonUtils
          .checkNotNull(ImmutableMap.of("service", generateStubOneData(deploymentMessage))));
      LOG.warn("GENERATING STUB ONE DATA FOR SERVICE"
          + " (remove once OneData parameters generation is completed!)");
    } else {
      LOG.debug("User specified I/O OneData requirements; service space will not be generated.");
      Map<String, OneData> oneDataRequirements = rankCloudProvidersMessage.getOneDataRequirements();
      {
        OneData oneDataInput = oneDataRequirements.get("input");
        if (oneDataInput != null) {
          if (oneDataInput.isSmartScheduling()) {
            oneDataInput.setProviders(oneDataInput.getProviders().stream()
                .filter(info -> Objects.equals(info.getCloudProviderId(),
                    deploymentMessage.getChosenCloudProvider().getId()))
                .collect(Collectors.toList()));
          }
          deploymentMessage.getOneDataParameters().put("input", oneDataInput);

        }
      }
      {
        OneData oneDataOutput = oneDataRequirements.get("output");
        if (oneDataOutput != null) {
          if (oneDataOutput.isSmartScheduling()) {
            oneDataOutput.setProviders(oneDataOutput.getProviders().stream()
                .filter(info -> Objects.equals(info.getCloudProviderId(),
                    deploymentMessage.getChosenCloudProvider().getId()))
                .collect(Collectors.toList()));
          }
          deploymentMessage.getOneDataParameters().put("output", oneDataOutput);
        }
      }
    }
  }

  /**
   * Temporary method to generate default OneData settings.
   * 
   * @return the {@link OneData} settings.
   */
  protected OneData generateStubOneData(DeploymentMessage deploymentMessage) {

    String path = new StringBuilder().append(oneDataService.getServiceSpacePath())
        .append(deploymentMessage.getDeploymentId()).toString();

    LOG.info(
        String.format("Generating OneData settings with parameters: %s",
            Arrays.asList(oneDataService.getServiceSpaceToken(),
                oneDataService.getServiceSpaceName(), path,
                oneDataService.getServiceSpaceProvider())));

    return new OneData(oneDataService.getServiceSpaceToken(), oneDataService.getServiceSpaceName(),
        path, oneDataService.getServiceSpaceProvider());
  }

}
