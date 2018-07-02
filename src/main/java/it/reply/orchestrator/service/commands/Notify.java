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

import lombok.extern.slf4j.Slf4j;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(WorkflowConstants.Delegate.NOTIFY)
@Slf4j
public class Notify extends BaseJavaDelegate {

  @Autowired
  private CallbackService callbackService;

  @Override
  public void customExecute(DelegateExecution execution) {
    try {
      String deploymentId = getRequiredParameter(execution, WorkflowConstants.Param.DEPLOYMENT_ID,
          String.class);
      callbackService.doCallback(deploymentId);
    } catch (RuntimeException ex) {
      // swallow it, do not put the whole process in error
      LOG.warn(getErrorMessagePrefix(), ex);
    }
  }

  @Override
  protected String getErrorMessagePrefix() {
    return "Error executing callback";
  }

}
