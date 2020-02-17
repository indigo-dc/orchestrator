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

package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.onedata.OneData.OneDataProviderInfo;
import it.reply.orchestrator.dto.workflow.CloudServicesOrderedIterator;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.exception.service.BusinessWorkflowException;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.CloudProviderEndpointServiceImpl;
import it.reply.orchestrator.utils.CommonUtils;
import it.reply.orchestrator.utils.WorkflowConstants;
import it.reply.orchestrator.utils.WorkflowConstants.ErrorCode;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Choose Cloud Provider and update Deployment/Message with the selected one data.
 */
@Component(WorkflowConstants.Delegate.UPDATE_DEPLOYMENT)
@Slf4j
public class UpdateDeployment extends BaseDeployCommand {

  private static final String DELIMITER = "\n" + StringUtils.repeat("-", 64) + "\n";

  @Autowired
  private CloudProviderEndpointServiceImpl cloudProviderEndpointService;

  @Override
  public void execute(DelegateExecution execution, DeploymentMessage deploymentMessage) {

    RankCloudProvidersMessage rankCloudProvidersMessage =
        getRequiredParameter(execution, WorkflowConstants.Param.RANK_CLOUD_PROVIDERS_MESSAGE,
            RankCloudProvidersMessage.class);

    CloudServicesOrderedIterator servicesIt = deploymentMessage.getCloudServicesOrderedIterator();

    if (servicesIt == null) {
      servicesIt = cloudProviderEndpointService
          .generateCloudProvidersOrderedIterator(rankCloudProvidersMessage,
              deploymentMessage.getMaxProvidersRetry());
      deploymentMessage.setCloudServicesOrderedIterator(servicesIt);
    }
    if (!servicesIt.hasNext()) {
      if (servicesIt.getSize() == 0) {
        throw new BusinessWorkflowException(ErrorCode.RUNTIME_ERROR,
            "No cloud providers available to deploy");
      } else {
        servicesIt.reset();
        String causes = CommonUtils
            .iteratorToStream(servicesIt)
            .map(wfService -> new StringBuilder()
                .append("Cloud Provider <")
                .append(wfService.getCloudService().getProviderId())
                .append(">\nCloud Provider Service <")
                .append(wfService.getCloudService().getId())
                .append(">\n\n")
                .append(wfService.getLastErrorCause()))
            .collect(Collectors.joining(DELIMITER, DELIMITER, ""));

        throw new BusinessWorkflowException(ErrorCode.RUNTIME_ERROR,
            "Retries on cloud providers exhausted. Error list is:\n" + causes);
      }
    }
    CloudService currentCloudService = servicesIt.next().getCloudService();
    Deployment deployment = getDeployment(deploymentMessage);

    boolean isUpdate = Status.UPDATE_IN_PROGRESS == deployment.getStatus();

    // Update Deployment
    if (!isUpdate) {
      deployment.setCloudProviderName(currentCloudService.getProviderId());
    }
    // FIXME Set/update all required selected CP data

    // FIXME Generate CP Endpoint
    CloudProviderEndpoint chosenCloudProviderEndpoint = cloudProviderEndpointService
        .getCloudProviderEndpoint(currentCloudService,
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
        .getDeploymentProvider(deploymentMessage.getDeploymentType(), currentCloudService);
    deployment.setDeploymentProvider(deploymentProvider);

    deploymentMessage.setOneDataParameters(generateOneDataParameters(currentCloudService,
        rankCloudProvidersMessage.getOneDataRequirements()));
  }

  protected @NonNull Map<String, OneData> generateOneDataParameters(
      @NonNull CloudService cloudService,
      @NonNull Map<String, OneData> oneDataRequirements) {
    Map<String, OneData> oneDataParameters = new HashMap<>();

    oneDataRequirements
        .forEach((name, oneDataRequirement) -> {
          Optional<OneDataProviderInfo> localOneProvider = oneDataRequirement
              .getOneproviders()
              .stream()
              .filter(oneDataProviderInfo -> cloudService.getProviderId()
                  .equals(oneDataProviderInfo.getCloudProviderId()))
              .findAny();
          final OneDataProviderInfo selectedOneProvider;
          if (localOneProvider.isPresent()) {
            selectedOneProvider = localOneProvider.get();
          } else {
            if (oneDataRequirement.isSmartScheduling()) {
              throw new DeploymentException("No OneProvider available");
            } else {
              selectedOneProvider = oneDataRequirement.getOneproviders().get(0);
            }
          }
          oneDataRequirement.setSelectedOneprovider(selectedOneProvider);
          oneDataParameters.put(name, oneDataRequirement);
        });
    return oneDataParameters;
  }

  @Override
  protected String getErrorMessagePrefix() {
    return "Error setting selected cloud providers";
  }

}
