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

import it.reply.orchestrator.service.CallbackService;
import it.reply.orchestrator.service.WorkflowConstants;
import it.reply.workflowmanager.spring.orchestrator.bpm.ejbcommands.BaseCommand;

import lombok.AllArgsConstructor;

import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor(onConstructor = @__({ @Autowired }))
public class Notify extends BaseCommand {

  private CallbackService callbackService;

  @Override
  protected ExecutionResults customExecute(CommandContext ctx) throws Exception {
    String deploymentId = getParameter(ctx, WorkflowConstants.WF_PARAM_DEPLOYMENT_ID);
    try {
      boolean result = callbackService.doCallback(deploymentId);
      return resultOccurred(result);
    } catch (Exception ex) {
      logger.error("Error tring to executing callback", ex);
      return resultOccurred(false);
    }
  }

}
