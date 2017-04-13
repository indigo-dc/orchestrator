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
import it.reply.orchestrator.service.WorkflowConstants;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderService;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderServiceRegistry;

import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Update extends BaseDeployCommand {

  @Autowired
  private DeploymentProviderServiceRegistry deploymentProviderServiceRegistry;

  @Override
  protected String getErrorMessagePrefix() {
    return "Error updating deployment";
  }

  @Override
  protected ExecutionResults customExecute(CommandContext ctx,
      DeploymentMessage deploymentMessage) {
    String template = getParameter(ctx, WorkflowConstants.WF_PARAM_TOSCA_TEMPLATE);

    DeploymentProviderService deploymentProviderService = deploymentProviderServiceRegistry
        .getDeploymentProviderService(deploymentMessage.getDeployment());

    boolean result = deploymentProviderService.doUpdate(deploymentMessage, template);
    return resultOccurred(result);
  }

}
