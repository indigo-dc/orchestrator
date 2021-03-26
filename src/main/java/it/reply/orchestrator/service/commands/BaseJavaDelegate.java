/*
 * Copyright © 2015-2021 I.N.F.N.
 * Copyright © 2015-2020 Santer Reply S.p.A.
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.reply.orchestrator.exception.service.BusinessWorkflowException;
import it.reply.orchestrator.exception.service.WorkflowException;
import it.reply.orchestrator.utils.MdcUtils;
import it.reply.orchestrator.utils.WorkflowConstants.ErrorCode;
import it.reply.orchestrator.utils.WorkflowUtil;

import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class BaseJavaDelegate implements JavaDelegate {

  @Autowired
  private ObjectMapper objectMapper;

  @Override
  public final void execute(DelegateExecution execution) {
    String businessKey = execution.getProcessInstanceBusinessKey();
    MdcUtils.fromBusinessKey(businessKey);

    String taskName = Optional
        .ofNullable(execution.getCurrentFlowElement())
        .map(FlowElement::getName)
        .orElseGet(() -> getClass().getSimpleName());

    try {
      LOG.info("Task {} - STARTED", taskName);
      customExecute(execution);
      LOG.info("Task {} - ENDED SUCCESSFULLY", taskName);
    } catch (BusinessWorkflowException ex) {
      LOG.error("Task {} - ENDED WITH ERROR:\n{}", taskName, getErrorMessagePrefix(), ex);
      WorkflowUtil.persistAndPropagateError(execution, ex);
    } catch (FlowableException | WorkflowException ex) {
      LOG.error("Task {} - ENDED WITH ERROR:\n{}", taskName, getErrorMessagePrefix(), ex);
      // Re-throw
      throw ex;
    } catch (RuntimeException ex) {
      LOG.error("Task {} - ENDED WITH ERROR:\n{}", taskName, getErrorMessagePrefix(), ex);
      throw new WorkflowException(ErrorCode.RUNTIME_ERROR, getErrorMessagePrefix(), ex);
    } finally {
      MdcUtils.clean();
    }
  }

  protected abstract String getErrorMessagePrefix();

  protected abstract void customExecute(DelegateExecution execution);

  protected <C> Optional<C> getOptionalParameter(DelegateExecution execution,
      String parameterName, Class<C> clazz) {
    Object variable = execution.getVariable(parameterName, false);
    if (variable == null) {
      LOG.warn("Parameter with name {} is null", parameterName);
      return Optional.empty();
    } else if (clazz.isInstance(variable)) {
      return Optional.of(clazz.cast(variable));
    } else if (JsonNode.class.isInstance(variable)) {
      try {
        return Optional.of(objectMapper.treeToValue(JsonNode.class.cast(variable), clazz));
      } catch (JsonProcessingException ex) {
        LOG.error("JSON parameter with name  {} couldn't be de-serialized", parameterName, ex);
        return Optional.empty();
      }
    } else {
      LOG.warn("Parameter with name {} of type {} can't be de-serialized", parameterName,
          variable.getClass());
      return Optional.empty();
    }
  }

  protected <C> C getRequiredParameter(DelegateExecution execution, String parameterName,
      Class<C> clazz) {
    return this
        .<C>getOptionalParameter(execution, parameterName, clazz)
        .orElseThrow(() -> new IllegalArgumentException(
            "WF parameter with name <" + parameterName + "> not found"));

  }
}
