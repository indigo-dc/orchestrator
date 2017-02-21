package it.reply.orchestrator.service.commands;

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
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderService;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderServiceRegistry;
import it.reply.utils.misc.polling.AbstractPollingBehaviour;
import it.reply.utils.misc.polling.ExternallyControlledPoller;
import it.reply.utils.misc.polling.PollingBehaviour;
import it.reply.utils.misc.polling.PollingException;
import it.reply.workflowmanager.spring.orchestrator.bpm.OrchestratorContextBean;

import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PollUndeploy extends BaseDeployCommand {

  @Autowired
  private DeploymentProviderServiceRegistry deploymentProviderServiceRegistry;

  @Override
  protected String getErrorMessagePrefix() {
    return "Error during undeploy status check";
  }

  @Override
  protected ExecutionResults customExecute(CommandContext ctx,
      DeploymentMessage deploymentMessage) {

    ExecutionResults exResults = new ExecutionResults();
    ExternallyControlledPoller<DeploymentMessage, Boolean> pollingStatus =
        getParameter(ctx, "pollingStatus");
    if (pollingStatus == null) {
      pollingStatus = getPoller();
    }
    exResults.setData("pollingStatus", pollingStatus);

    DeploymentProviderService deploymentProviderService = deploymentProviderServiceRegistry
        .getDeploymentProviderService(deploymentMessage.getDeployment());

    try {
      Boolean result = pollingStatus.doPollEvent(deploymentMessage);
      if (result != null && result) {
        deploymentProviderService.finalizeUndeploy(deploymentMessage, result);
        deploymentMessage.setDeployment(null);
        return resultOccurred(true, exResults);
      } else {
        return resultOccurred(false, exResults);
      }
    } catch (PollingException ex) {
      deploymentProviderService.finalizeUndeploy(deploymentMessage, false);
      return resultOccurred(true, exResults);
    }
  }

  private static ExternallyControlledPoller<DeploymentMessage, Boolean> getPoller() {

    long timeoutTime = 30 * 60 * 1000;

    PollingBehaviour<DeploymentMessage, Boolean> pollBehavior =
        new AbstractPollingBehaviour<DeploymentMessage, Boolean>(timeoutTime) {

          private static final long serialVersionUID = -5994059867039967783L;

          @Override
          public Boolean doPolling(DeploymentMessage deploymentMessage) throws PollingException {
            try {
              DeploymentProviderServiceRegistry registry =
                  OrchestratorContextBean.getBean(DeploymentProviderServiceRegistry.class);
              DeploymentProviderService deploymentProviderService =
                  registry.getDeploymentProviderService(deploymentMessage.getDeployment());
              return deploymentProviderService.isUndeployed(deploymentMessage);
            } catch (Exception ex) {
              throw new PollingException("Polling for undeploy - error occured: " + ex.getMessage(),
                  ex);
            }
          }

          @Override
          public boolean pollExit(Boolean pollResult) {
            return pollResult != null && pollResult;
          }

        };

    return new ExternallyControlledPoller<>(pollBehavior, 3);
  }
}
