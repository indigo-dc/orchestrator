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

import it.reply.orchestrator.exception.service.WorkflowException;
import it.reply.orchestrator.utils.MdcUtils;

import lombok.extern.slf4j.Slf4j;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

import java.util.Optional;

@Slf4j
public abstract class BaseJavaDelegate implements JavaDelegate {

  public static final String BUSINESS_ERROR_CODE = "BusinessException";

  @Override
  public final void execute(DelegateExecution execution) {
    String taskName = Optional
        .ofNullable(execution.getCurrentFlowElement())
        .map(FlowElement::getName)
        .orElseGet(() -> getClass().getSimpleName());
    String businessKey = execution.getProcessInstanceBusinessKey();
    MdcUtils.fromBusinessKey(businessKey);
    try {
      LOG.info("{} - STARTED", taskName);
      customExecute(execution);
      LOG.info("{} - ENDED", taskName);
    } catch (FlowableException ex) {
      // Re-throw
      throw ex;
    } catch (RuntimeException ex) {
      LOG.error("{} - ENDED WITH ERROR:\n{}", taskName, getErrorMessagePrefix(), ex);
      throw new WorkflowException(BUSINESS_ERROR_CODE, getErrorMessagePrefix(), ex);
    } finally {
      MdcUtils.clean();
    }
  }

  protected abstract String getErrorMessagePrefix();

  protected abstract void customExecute(DelegateExecution execution);

  @SuppressWarnings("unchecked")
  protected <C> Optional<C> getOptionalParameter(DelegateExecution execution,
      String parameterName) {
    try {
      return Optional.ofNullable((C) execution.getVariable(parameterName, false));
    } catch (ClassCastException ex) {
      LOG.error("WF parameter with name <{}> not an instance of the required class", parameterName,
          ex);
      return Optional.empty();
    }
  }

  protected <C> C getRequiredParameter(DelegateExecution execution, String parameterName) {
    return this
        .<C>getOptionalParameter(execution, parameterName)
        .orElseThrow(() -> new IllegalArgumentException(
            "WF parameter with name <" + parameterName + "> not found"));

  }
}
