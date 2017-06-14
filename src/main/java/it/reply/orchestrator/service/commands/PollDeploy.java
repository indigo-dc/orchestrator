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

import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderService;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderServiceRegistry;
import it.reply.orchestrator.utils.WorkflowConstants;
import it.reply.utils.misc.polling.AbstractPollingBehaviour;
import it.reply.utils.misc.polling.ExternallyControlledPoller;
import it.reply.utils.misc.polling.ExternallyControlledPoller.PollingStatus;
import it.reply.utils.misc.polling.PollingBehaviour;
import it.reply.utils.misc.polling.PollingException;
import it.reply.workflowmanager.spring.orchestrator.bpm.OrchestratorContextBean;

import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PollDeploy extends BaseDeployCommand<PollDeploy> {

  @Autowired
  private DeploymentProviderServiceRegistry deploymentProviderServiceRegistry;

  @Override
  protected ExecutionResults customExecute(CommandContext ctx,
      DeploymentMessage deploymentMessage) {

    ExecutionResults exResults = new ExecutionResults();
    ExternallyControlledPoller<DeploymentMessage, Status> statusPoller =
        getParameter(ctx, WorkflowConstants.WF_PARAM_POLLING_STATUS);
    if (statusPoller == null) {
      statusPoller = getPoller();
    }
    exResults.setData(WorkflowConstants.WF_PARAM_POLLING_STATUS, statusPoller);

    DeploymentProviderService deploymentProviderService = deploymentProviderServiceRegistry
        .getDeploymentProviderService(deploymentMessage.getDeployment());
    try {
      statusPoller.doPollEvent(deploymentMessage);
      if (statusPoller.getPollStatus() == PollingStatus.ENDED) {
        // Polling ended successfully -> Deployment completed -> Finalize (update template)
        deploymentProviderService.finalizeDeploy(deploymentMessage, true);
        return resultOccurred(true, exResults);
      } else {
        // Deployment is not ready yet
        return resultOccurred(false, exResults);
      }
    } catch (PollingException ex) {
      // Polling unsuccessful -> Deploy failed -> Finalize (update template)
      deploymentProviderService.finalizeDeploy(deploymentMessage, false);
      return resultOccurred(true, exResults);
    }
  }

  private static ExternallyControlledPoller<DeploymentMessage, Status> getPoller() {

    long timeoutTime = 3 * 60 * 60 * 1000L;

    PollingBehaviour<DeploymentMessage, Status> pollBehavior =
        new AbstractPollingBehaviour<DeploymentMessage, Status>(timeoutTime) {

          private static final long serialVersionUID = -5994059867039967783L;

          @Override
          public Status doPolling(DeploymentMessage deploymentMessage) throws PollingException {
            try {
              DeploymentProviderServiceRegistry registry =
                  OrchestratorContextBean.getBean(DeploymentProviderServiceRegistry.class);
              DeploymentProviderService deploymentProviderService =
                  registry.getDeploymentProviderService(deploymentMessage.getDeployment());
              if (deploymentProviderService.isDeployed(deploymentMessage)) {
                return Status.CREATE_COMPLETE;
              } else {
                return Status.CREATE_IN_PROGRESS;
              }
            } catch (DeploymentException de) {
              return Status.CREATE_FAILED;
            } catch (Exception ex) {
              throw new PollingException("Polling for deploy - error occured: " + ex.getMessage(),
                  ex);
            }
          }

          @Override
          public boolean pollExit(Status pollResult) {
            return pollResult != null && pollResult != Status.CREATE_IN_PROGRESS;
          }

          @Override
          public boolean pollSuccessful(DeploymentMessage params, Status pollResult) {
            return pollResult != null && pollResult == Status.CREATE_COMPLETE;
          }

        };

    return new ExternallyControlledPoller<>(pollBehavior, 3);
  }

  @Override
  protected String getErrorMessagePrefix() {
    return "Error during deploy status check";
  }
}
