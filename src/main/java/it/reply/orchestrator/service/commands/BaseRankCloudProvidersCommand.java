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

import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.service.WorkflowConstants;
import it.reply.orchestrator.service.deployment.providers.DeploymentStatusHelper;
import it.reply.workflowmanager.spring.orchestrator.bpm.ejbcommands.BaseCommand;

import lombok.extern.slf4j.Slf4j;

import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base behavior for all RankCloudProvider WF tasks. <br/>
 * This checks input parameters and manages output and errors (specifically, in case of errors, it
 * also updates directly the deployment status on DB).
 * 
 * @author l.biava
 *
 */
@Slf4j
public abstract class BaseRankCloudProvidersCommand extends BaseCommand {

  @Autowired
  protected DeploymentStatusHelper deploymentStatusHelper;

  protected abstract String getErrorMessagePrefix();

  /**
   * <b>This method SHOULD NOT be overridden! It cannot be final for INJECTION purpose!</b> <br/>
   * Use the {@link #customExecute(RankCloudProvidersMessage)} method to implement command logic.
   */
  @Override
  protected ExecutionResults customExecute(CommandContext ctx) throws Exception {
    RankCloudProvidersMessage rankCloudProvidersMessage =
        getParameter(ctx, WorkflowConstants.WF_PARAM_RANK_CLOUD_PROVIDERS_MESSAGE);
    if (rankCloudProvidersMessage == null) {
      throw new IllegalArgumentException(String.format("WF parameter <%s> cannot be null",
          WorkflowConstants.WF_PARAM_RANK_CLOUD_PROVIDERS_MESSAGE));
    }
    ExecutionResults exResults = new ExecutionResults();
    try {
      rankCloudProvidersMessage = customExecute(rankCloudProvidersMessage);
      exResults.setData(WorkflowConstants.WF_PARAM_RANK_CLOUD_PROVIDERS_MESSAGE,
          rankCloudProvidersMessage);
      exResults.getData().putAll(resultOccurred(true).getData());
    } catch (Exception ex) {
      LOG.error(String.format("Error executing %s", this.getClass().getSimpleName()), ex);
      exResults.getData().putAll(resultOccurred(false).getData());

      // Update deployment with error
      // TODO: what if this fails??
      deploymentStatusHelper.updateOnError(rankCloudProvidersMessage.getDeploymentId(),
          getErrorMessagePrefix(), ex);
    }

    return exResults;
  }

  protected abstract RankCloudProvidersMessage customExecute(
      RankCloudProvidersMessage rankCloudProvidersMessage) throws Exception;

}
