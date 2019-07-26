/*
 * Copyright © 2015-2019 Santer Reply S.p.A.
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

package it.reply.orchestrator.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.exception.service.WorkflowException;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.helper.ErrorPropagation;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("WorkflowUtil")
public class WorkflowUtil {

  @Autowired
  private ObjectMapper objectMapper;

  public JsonNode generateRankCloudProvidersMessage(JsonNode jsonDeploymentMessage)
      throws JsonProcessingException {
    DeploymentMessage dm = objectMapper.treeToValue(jsonDeploymentMessage, DeploymentMessage.class);
    return objectMapper.valueToTree(new RankCloudProvidersMessage(dm));
  }

  /**
   * Persist a {@link WorkflowException} as workflow parameter and propagate it to its handler.
   *
   * @param execution
   *     the current workflow execution
   * @param exception
   *     the exception to hadle
   */
  public static void persistAndPropagateError(DelegateExecution execution,
      WorkflowException exception) {
    ExecutionEntity parentExecution;
    if (execution instanceof ExecutionEntity) {
      parentExecution = (ExecutionEntity) execution;
    } else {
      parentExecution = CommandContextUtil
          .getExecutionEntityManager()
          .findById(execution.getProcessInstanceId());
    }
    while (parentExecution != null) {
      parentExecution = parentExecution.getProcessInstance();
      parentExecution.setVariable(WorkflowConstants.Param.EXCEPTION, exception, false);
      parentExecution = parentExecution.getSuperExecution();
    }
    ErrorPropagation.propagateError(exception.getErrorCode(), execution);
  }
}
