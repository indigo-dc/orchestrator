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

import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.workflow.CloudServiceWf;
import it.reply.orchestrator.utils.WorkflowConstants;
import it.reply.orchestrator.utils.WorkflowConstants.Delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

@Component(Delegate.CLEAN_FAILED_UPDATE)
public class CleanFailedUpdate extends BaseDeployCommand {

  @Override
  protected String getErrorMessagePrefix() {
    return "Error cleaning failed Deployment update";
  }

  @Override
  public void execute(DelegateExecution execution, DeploymentMessage deploymentMessage) {
    CloudServiceWf cloudServiceWf = deploymentMessage.getCloudServicesOrderedIterator().current();
    if (cloudServiceWf.getLastErrorCause() == null) {
      Exception exception = getRequiredParameter(execution, WorkflowConstants.Param.EXCEPTION,
          Exception.class);
      cloudServiceWf.setLastErrorCause(exception.getMessage());
    }
    getDeploymentProviderService(deploymentMessage).cleanFailedUpdate(deploymentMessage);
  }

}
