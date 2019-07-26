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

package it.reply.orchestrator.service.commands;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.utils.WorkflowConstants;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseRankCloudProvidersCommand
    extends BaseWorkflowCommand<RankCloudProvidersMessage> {

  @Autowired
  private ObjectMapper objectMapper;

  @Override
  protected RankCloudProvidersMessage getMessage(DelegateExecution execution) {
    return getRequiredParameter(execution, WorkflowConstants.Param.RANK_CLOUD_PROVIDERS_MESSAGE,
        RankCloudProvidersMessage.class);
  }

  @Override
  protected void setMessage(RankCloudProvidersMessage message, DelegateExecution execution) {
    execution
        .setVariable(WorkflowConstants.Param.RANK_CLOUD_PROVIDERS_MESSAGE,
            objectMapper.valueToTree(message),
            false);
  }
}
