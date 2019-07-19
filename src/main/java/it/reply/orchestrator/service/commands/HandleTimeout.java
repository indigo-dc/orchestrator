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

import it.reply.orchestrator.exception.service.BusinessWorkflowException;
import it.reply.orchestrator.utils.WorkflowConstants;
import it.reply.orchestrator.utils.WorkflowConstants.ErrorCode;
import it.reply.orchestrator.utils.WorkflowUtil;

import lombok.AllArgsConstructor;
//import lombok.extern.slf4j.Slf4j;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

@Component(WorkflowConstants.Delegate.HANDLE_TIMEOUT)
@AllArgsConstructor
//@Slf4j
public class HandleTimeout extends BaseJavaDelegate {

  @Override
  public void customExecute(DelegateExecution execution) {
    WorkflowUtil.persistAndPropagateError(execution,
        new BusinessWorkflowException(ErrorCode.RUNTIME_ERROR,
            "Timeout: Maximum execution time exceeded."));
  }

  @Override
  protected String getErrorMessagePrefix() {
    return "Error handling process timeout";
  }

}
