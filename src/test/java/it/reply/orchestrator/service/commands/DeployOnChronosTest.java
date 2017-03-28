package it.reply.orchestrator.service.commands;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.NameNotFoundException;

import it.reply.orchestrator.controller.ControllerTestUtils;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.service.ToscaService;
import it.reply.orchestrator.service.WorkflowConstants;
import it.reply.orchestrator.service.commands.chronos.DeployOnChronos;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderService;
import it.reply.orchestrator.service.deployment.providers.DeploymentStatusHelper;
import it.reply.workflowmanager.utils.Constants;

public class DeployOnChronosTest {

  @InjectMocks
  private DeployOnChronos deployOnChronos;

  @Mock
  private DeploymentRepository deploymentRepository;

  @Mock
  private DeploymentStatusHelper deploymentStatusHelper;

  @Mock
  private ToscaService toscaService;

  @Mock
  private DeploymentProviderService chronosService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testCustomExecuteSuccessResultFalse() throws Exception {
    CommandContext commandContext = new CommandContext();
    DeploymentMessage dm = generateDeployDm();

    WorkItemImpl workItem = new WorkItemImpl();
    workItem.setParameter(WorkflowConstants.WF_PARAM_DEPLOYMENT_MESSAGE, dm);
    commandContext.setData(Constants.WORKITEM, workItem);

    Mockito.doNothing().when(deploymentStatusHelper).updateOnError(Mockito.anyString(),
        Mockito.anyString(), Mockito.anyObject());

    Mockito.doThrow(new NullPointerException()).when(deploymentRepository)
        .findOne(dm.getDeploymentId());

    ExecutionResults expectedResult = new ExecutionResults();
    expectedResult.setData(Constants.RESULT_STATUS, "OK");
    expectedResult.setData(Constants.OK_RESULT, false);
    ExecutionResults customExecute = deployOnChronos.customExecute(commandContext);

    Assert.assertEquals(expectedResult.getData(Constants.RESULT_STATUS),
        customExecute.getData(Constants.RESULT_STATUS));
    Assert.assertEquals(expectedResult.getData(Constants.OK_RESULT),
        customExecute.getData(Constants.OK_RESULT));
  }

  @Test
  public void testCustomExecuteSuccessResultTrue() throws Exception {
    CommandContext commandContext = new CommandContext();
    DeploymentMessage dm = generateDeployDm();

    WorkItemImpl workItem = new WorkItemImpl();
    workItem.setParameter(WorkflowConstants.WF_PARAM_DEPLOYMENT_MESSAGE, dm);
    commandContext.setData(Constants.WORKITEM, workItem);

    Mockito.doNothing().when(deploymentStatusHelper).updateOnError(Mockito.anyString(),
        Mockito.anyString(), Mockito.anyObject());
    Mockito.doReturn(true).when(chronosService).doDeploy(dm);

    ExecutionResults expectedResult = new ExecutionResults();
    expectedResult.setData(Constants.RESULT_STATUS, "OK");
    expectedResult.setData(Constants.OK_RESULT, true);
    ExecutionResults customExecute = deployOnChronos.customExecute(commandContext);

    Assert.assertEquals(expectedResult.getData(Constants.RESULT_STATUS),
        customExecute.getData(Constants.RESULT_STATUS));
    Assert.assertEquals(expectedResult.getData(Constants.OK_RESULT),
        customExecute.getData(Constants.OK_RESULT));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCustomExecuteFailIllegalArgument() throws Exception {
    CommandContext commandContext = new CommandContext();

    WorkItemImpl workItem = new WorkItemImpl();
    workItem.setParameter(WorkflowConstants.WF_PARAM_DEPLOYMENT_MESSAGE, null);
    commandContext.setData(Constants.WORKITEM, workItem);

    Mockito.doNothing().when(deploymentStatusHelper).updateOnError(Mockito.anyString(),
        Mockito.anyString(), Mockito.anyObject());

    deployOnChronos.customExecute(commandContext);
  }



  private DeploymentMessage generateDeployDm() {
    DeploymentMessage dm = new DeploymentMessage();
    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.setStatus(Status.CREATE_IN_PROGRESS);
    dm.setDeployment(deployment);
    dm.setDeploymentId(deployment.getId());
    deployment.getResources().addAll(ControllerTestUtils.createResources(deployment, 2, false));
    deployment.getResources().stream().forEach(r -> r.setState(NodeStates.CREATING));

    CloudProviderEndpoint chosenCloudProviderEndpoint = new CloudProviderEndpoint();
    chosenCloudProviderEndpoint.setCpComputeServiceId(UUID.randomUUID().toString());
    dm.setChosenCloudProviderEndpoint(chosenCloudProviderEndpoint);
    return dm;
  }

}
