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

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.service.CmdbService;
import it.reply.orchestrator.utils.WorkflowConstants;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(WorkflowConstants.Delegate.GET_CMDB_DATA_UPDATE)
public class GetCmdbDataUpdate extends BaseDeployCommand {

  @Autowired
  private CmdbService cmdbService;

  @Override
  public void execute(DelegateExecution execution, DeploymentMessage deploymentMessage) {
    Deployment deployment = getDeployment(deploymentMessage);
    CloudProvider cp = new CloudProvider(deployment.getCloudProviderName());
    cp.getCmdbProviderServices().put(deployment.getCloudProviderEndpoint().getCpComputeServiceId(),
        null);
    cmdbService.fillCloudProviderInfo(cp);
    deploymentMessage.setChosenCloudProvider(cp);
  }

  @Override
  protected String getErrorMessagePrefix() {
    return "Error retrieving info from CMDB";
  }
}
