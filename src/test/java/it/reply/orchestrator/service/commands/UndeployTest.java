/*
 * Copyright © 2015-2017 Santer Reply S.p.A.
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

import org.drools.core.process.instance.impl.WorkItemImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderService;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderServiceRegistry;
import it.reply.orchestrator.util.TestUtil;
import it.reply.orchestrator.utils.WorkflowConstants;
import it.reply.workflowmanager.utils.Constants;

public class UndeployTest {

  @InjectMocks
  Undeploy undeploy = new Undeploy();

  @Mock
  private DeploymentProviderService deploymentProviderService;

  @Mock
  private DeploymentProviderServiceRegistry deploymentProviderServiceRegistry;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testCustomExecute() throws Exception {
    DeploymentMessage dm = TestUtil.generateDeployDm();
    dm.setDeleteComplete(true);

    CommandContext commandContext = new CommandContext();

    WorkItemImpl workItem = new WorkItemImpl();
    workItem.setParameter(WorkflowConstants.WF_PARAM_TOSCA_TEMPLATE, "template");
    commandContext.setData(Constants.WORKITEM, workItem);
    Mockito.when(deploymentProviderServiceRegistry
        .getDeploymentProviderService(dm.getDeployment())).thenReturn(deploymentProviderService);
    Mockito.when(deploymentProviderService.doUndeploy(dm)).thenReturn(true);
    Mockito.doNothing().when(deploymentProviderService)
      .finalizeUndeploy(Mockito.anyObject(), Mockito.anyBoolean());

    ExecutionResults expectedResult = new ExecutionResults();
    expectedResult.setData(Constants.RESULT_STATUS, "OK");
    expectedResult.setData(Constants.OK_RESULT, true);

    ExecutionResults customExecute = undeploy.customExecute(commandContext, dm);

    Assert.assertEquals(expectedResult.getData(Constants.RESULT_STATUS),
        customExecute.getData(Constants.RESULT_STATUS));
    Assert.assertEquals(expectedResult.getData(Constants.OK_RESULT),
        customExecute.getData(Constants.OK_RESULT));
    Assert.assertEquals(undeploy.getErrorMessagePrefix(), "Error undeploying");
  }

 
}
