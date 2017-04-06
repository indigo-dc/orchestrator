package it.reply.orchestrator.service.commands;

/*
 * Copyright © 2015-2017 Santer Reply S.p.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.util.UUID;

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

import it.reply.orchestrator.controller.ControllerTestUtils;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderService;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderServiceRegistry;
import it.reply.orchestrator.util.TestUtil;
import it.reply.utils.misc.polling.ExternallyControlledPoller;
import it.reply.utils.misc.polling.PollingException;
import it.reply.workflowmanager.utils.Constants;

public class PoolUndeployTest {

  @InjectMocks
  PollUndeploy pollUndeploy = new PollUndeploy();

  @Mock
  private DeploymentProviderService deploymentProviderService;

  @Mock
  private DeploymentProviderServiceRegistry deploymentProviderServiceRegistry;

  @Mock
  private ExternallyControlledPoller<DeploymentMessage, Boolean> pollingStatus;

  public static final String WF_PARAM_POLLING_STATUS = "pollingStatus";

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testCustomExecuteFalse() {

    CommandContext commandContext = new CommandContext();
    DeploymentMessage dm = TestUtil.generateDeployDm();

    WorkItemImpl workItem = new WorkItemImpl();
    workItem.setParameter(WF_PARAM_POLLING_STATUS, null);

    commandContext.setData(Constants.WORKITEM, workItem);

    ExecutionResults expectedResult = new ExecutionResults();
    expectedResult.setData(Constants.RESULT_STATUS, "OK");
    expectedResult.setData(Constants.OK_RESULT, false);
    expectedResult.setData(WF_PARAM_POLLING_STATUS, null);
    ExecutionResults customExecute = pollUndeploy.customExecute(commandContext, dm);

    Assert.assertEquals(expectedResult.getData(Constants.RESULT_STATUS),
        customExecute.getData(Constants.RESULT_STATUS));
    Assert.assertEquals(expectedResult.getData(Constants.OK_RESULT),
        customExecute.getData(Constants.OK_RESULT));
    Assert.assertNotEquals(null, customExecute.getData(WF_PARAM_POLLING_STATUS));
  }

  @Test
  public void testCustomExecuteTrue() {

    CommandContext commandContext = new CommandContext();
    DeploymentMessage dm = TestUtil.generateDeployDm();

    WorkItemImpl workItem = new WorkItemImpl();
    workItem.setParameter(WF_PARAM_POLLING_STATUS, pollingStatus);

    commandContext.setData(Constants.WORKITEM, workItem);

    ExecutionResults expectedResult = new ExecutionResults();
    expectedResult.setData(Constants.RESULT_STATUS, "OK");
    expectedResult.setData(Constants.OK_RESULT, true);
    expectedResult.setData(WF_PARAM_POLLING_STATUS, pollingStatus);

    Mockito.when(pollingStatus.doPollEvent(Mockito.any(DeploymentMessage.class))).thenReturn(true);
    Mockito.when(deploymentProviderServiceRegistry.getDeploymentProviderService(dm.getDeployment()))
        .thenReturn(deploymentProviderService);
    Mockito.doNothing().when(deploymentProviderService).finalizeUndeploy(Mockito.anyObject(),
        Mockito.anyBoolean());
    ExecutionResults customExecute = pollUndeploy.customExecute(commandContext, dm);

    Assert.assertEquals(expectedResult.getData(Constants.RESULT_STATUS),
        customExecute.getData(Constants.RESULT_STATUS));
    Assert.assertEquals(expectedResult.getData(Constants.OK_RESULT),
        customExecute.getData(Constants.OK_RESULT));
    Assert.assertEquals(pollingStatus, customExecute.getData(WF_PARAM_POLLING_STATUS));

  }

  @Test
  public void testCustomExecutePollingException() {

    CommandContext commandContext = new CommandContext();
    DeploymentMessage dm = TestUtil.generateDeployDm();

    WorkItemImpl workItem = new WorkItemImpl();
    workItem.setParameter(WF_PARAM_POLLING_STATUS, pollingStatus);

    commandContext.setData(Constants.WORKITEM, workItem);

    ExecutionResults expectedResult = new ExecutionResults();
    expectedResult.setData(Constants.RESULT_STATUS, "OK");
    expectedResult.setData(Constants.OK_RESULT, true);
    expectedResult.setData(WF_PARAM_POLLING_STATUS, pollingStatus);

    Mockito.when(pollingStatus.doPollEvent(Mockito.any(DeploymentMessage.class))).thenReturn(true);
    Mockito.when(deploymentProviderServiceRegistry.getDeploymentProviderService(dm.getDeployment()))
        .thenReturn(deploymentProviderService);
    Mockito.doNothing().when(deploymentProviderService).finalizeUndeploy(Mockito.anyObject(),
        Mockito.anyBoolean());

    Mockito.when(pollingStatus.doPollEvent(Mockito.any(DeploymentMessage.class)))
        .thenThrow(new PollingException());
    Mockito.when(deploymentProviderServiceRegistry.getDeploymentProviderService(dm.getDeployment()))
        .thenReturn(deploymentProviderService);
    ExecutionResults customExecute = pollUndeploy.customExecute(commandContext, dm);

    Assert.assertEquals(expectedResult.getData(Constants.RESULT_STATUS),
        customExecute.getData(Constants.RESULT_STATUS));
    Assert.assertEquals(expectedResult.getData(Constants.OK_RESULT),
        customExecute.getData(Constants.OK_RESULT));
    Assert.assertNotEquals(null, customExecute.getData(WF_PARAM_POLLING_STATUS));
  }



}
