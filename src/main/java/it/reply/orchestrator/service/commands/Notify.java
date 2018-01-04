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

import it.reply.orchestrator.service.CallbackService;
import it.reply.orchestrator.utils.WorkflowConstants;
import it.reply.workflowmanager.spring.orchestrator.bpm.ejbcommands.BaseCommand;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class Notify extends BaseCommand<Notify> {

  private CallbackService callbackService;

  @Override
  public ExecutionResults customExecute(CommandContext ctx) {
    String deploymentId = getRequiredParameter(ctx, WorkflowConstants.WF_PARAM_DEPLOYMENT_ID);
    try {
      boolean result = callbackService.doCallback(deploymentId);
      return resultOccurred(result);
    } catch (RuntimeException ex) {
      LOG.error("Error executing callback for deployment {}", deploymentId, ex);
      return resultOccurred(false);
    }
  }

}
