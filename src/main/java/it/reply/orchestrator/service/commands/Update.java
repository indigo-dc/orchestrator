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

import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderService;
import it.reply.orchestrator.utils.WorkflowConstants;

import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class Update extends BaseDeployCommand<Update> {

  @Override
  protected String getErrorMessagePrefix() {
    return "Error updating deployment";
  }

  @Override
  @Transactional
  public ExecutionResults customExecute(CommandContext ctx,
      DeploymentMessage deploymentMessage) {
    String template = getRequiredParameter(ctx, WorkflowConstants.WF_PARAM_TOSCA_TEMPLATE);

    DeploymentProviderService deploymentProviderService =
        getDeploymentProviderService(deploymentMessage);

    deploymentMessage
        .setCreateComplete(deploymentProviderService.doUpdate(deploymentMessage, template));
    return resultOccurred(true);
  }

}
