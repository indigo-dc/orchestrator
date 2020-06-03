/*
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

import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dal.entity.ReplicationRule;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.exception.service.BusinessWorkflowException;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.RucioService;
import it.reply.orchestrator.utils.WorkflowConstants;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(WorkflowConstants.Delegate.CHECK_TEMP_REPLICATION_RULE)
public class CheckTempReplicationRule extends BaseDeployCommand {

  @Autowired
  private RucioService rucioService;

  @Override
  protected void execute(DelegateExecution execution, DeploymentMessage message) {
    OidcTokenId requestedWithToken = message.getRequestedWithToken();
    String deploymentId = message.getDeploymentId();
    ReplicationRule rule = rucioService.syncTempReplicationRule(requestedWithToken, deploymentId);
    switch (rule.getStatus()) {
      case OK:
        message.setTempReplicationRuleCompleted(true);
        break;
      case STUCK:
        throw new BusinessWorkflowException(WorkflowConstants.ErrorCode.CLOUD_PROVIDER_ERROR,
            "Error while checking temp replication rule",
            new DeploymentException("Temp Replication rule is stuck: " + rule.getStatusReason()));
      default:
        // DO NOTHING
    }
  }

  @Override
  protected String getErrorMessagePrefix() {
    return "Error while checking temp replication rule";
  }
}
