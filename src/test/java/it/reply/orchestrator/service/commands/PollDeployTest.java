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

import java.util.HashMap;
import java.util.Map;
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
import org.springframework.web.client.RestTemplate;

import it.reply.orchestrator.controller.ControllerTestUtils;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.WorkflowConstants;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderService;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderServiceRegistry;
import it.reply.orchestrator.service.deployment.providers.ImServiceImpl;
import it.reply.utils.misc.polling.AbstractPollingBehaviour;
import it.reply.utils.misc.polling.ExternallyControlledPoller;
import it.reply.utils.misc.polling.PollingBehaviour;
import it.reply.utils.misc.polling.PollingException;
import it.reply.utils.misc.polling.ExternallyControlledPoller.PollingStatus;
import it.reply.workflowmanager.spring.orchestrator.bpm.OrchestratorContextBean;
import it.reply.workflowmanager.utils.Constants;

public class PollDeployTest {

  @InjectMocks
  private PollDeploy pollDeploy = new PollDeploy();

  @Mock
  private RestTemplate restTemplate;

  @Mock
  private ExternallyControlledPoller<DeploymentMessage, Status> statusPoller;

  @Mock
  private DeploymentProviderService deploymentProviderService;

  @Mock
  private DeploymentProviderServiceRegistry deploymentProviderServiceRegistry;
  
  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testCustomExecuteFalse() {

    // WF_PARAM_POLLING_STATUS null
    CommandContext commandContext = new CommandContext();
    DeploymentMessage dm = generateDeployDm();

    WorkItemImpl workItem = new WorkItemImpl();
    workItem.setParameter(WorkflowConstants.WF_PARAM_POLLING_STATUS, null);

    commandContext.setData(Constants.WORKITEM, workItem);

    ExecutionResults expectedResult = new ExecutionResults();
    expectedResult.setData(Constants.RESULT_STATUS, "OK");
    expectedResult.setData(Constants.OK_RESULT, false);
    expectedResult.setData(WorkflowConstants.WF_PARAM_POLLING_STATUS, null);
    ExecutionResults customExecute = pollDeploy.customExecute(commandContext, dm);

    Assert.assertEquals(expectedResult.getData(Constants.RESULT_STATUS),
        customExecute.getData(Constants.RESULT_STATUS));
    Assert.assertEquals(expectedResult.getData(Constants.OK_RESULT),
        customExecute.getData(Constants.OK_RESULT));
    Assert.assertNotEquals(null, customExecute.getData(WorkflowConstants.WF_PARAM_POLLING_STATUS));

    // WF_PARAM_POLLING_STATUS not null
    workItem = new WorkItemImpl();
    workItem.setParameter(WorkflowConstants.WF_PARAM_POLLING_STATUS, getPoolBehavior());

    commandContext.setData(Constants.WORKITEM, workItem);

    expectedResult = new ExecutionResults();
    expectedResult.setData(Constants.RESULT_STATUS, "OK");
    expectedResult.setData(Constants.OK_RESULT, false);
    expectedResult.setData(WorkflowConstants.WF_PARAM_POLLING_STATUS, getPoolBehavior());
    customExecute = pollDeploy.customExecute(commandContext, dm);

    Assert.assertEquals(expectedResult.getData(Constants.RESULT_STATUS),
        customExecute.getData(Constants.RESULT_STATUS));
    Assert.assertEquals(expectedResult.getData(Constants.OK_RESULT),
        customExecute.getData(Constants.OK_RESULT));
    Assert.assertNotEquals(null, customExecute.getData(WorkflowConstants.WF_PARAM_POLLING_STATUS));

  }

  @Test
  public void testCustomExecuteTrue() {
    CommandContext commandContext = new CommandContext();
    DeploymentMessage dm = generateDeployDm();

    WorkItemImpl workItem = new WorkItemImpl();
    workItem.setParameter(WorkflowConstants.WF_PARAM_POLLING_STATUS, statusPoller);

    commandContext.setData(Constants.WORKITEM, workItem);

    ExecutionResults expectedResult = new ExecutionResults();
    expectedResult.setData(Constants.RESULT_STATUS, "OK");
    expectedResult.setData(Constants.OK_RESULT, true);
    expectedResult.setData(WorkflowConstants.WF_PARAM_POLLING_STATUS, statusPoller);

    Mockito.when(statusPoller.getPollStatus()).thenReturn(PollingStatus.ENDED);
    Mockito.when(deploymentProviderServiceRegistry
        .getDeploymentProviderService(dm.getDeployment())).thenReturn(deploymentProviderService);
    Mockito.doNothing().when(deploymentProviderService)
        .finalizeDeploy(Mockito.anyObject(), Mockito.anyBoolean());
    ExecutionResults customExecute = pollDeploy.customExecute(commandContext, dm);

    Assert.assertEquals(expectedResult.getData(Constants.RESULT_STATUS),
        customExecute.getData(Constants.RESULT_STATUS));
    Assert.assertEquals(expectedResult.getData(Constants.OK_RESULT),
        customExecute.getData(Constants.OK_RESULT));
    Assert.assertEquals(statusPoller, customExecute.getData(WorkflowConstants.WF_PARAM_POLLING_STATUS));

    // generate polling exception

    Mockito.when(statusPoller.doPollEvent(Mockito.anyObject())).thenThrow(new PollingException());
    customExecute = pollDeploy.customExecute(commandContext, dm);

    Assert.assertEquals(expectedResult.getData(Constants.RESULT_STATUS),
        customExecute.getData(Constants.RESULT_STATUS));
    Assert.assertEquals(expectedResult.getData(Constants.OK_RESULT),
        customExecute.getData(Constants.OK_RESULT));
    Assert.assertEquals(statusPoller, customExecute.getData(WorkflowConstants.WF_PARAM_POLLING_STATUS));
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

  public ExternallyControlledPoller<DeploymentMessage, Status> getPoolBehavior() {
    PollingBehaviour<DeploymentMessage, Status> pollBehavior =
        new AbstractPollingBehaviour<DeploymentMessage, Status>(30 * 60 * 1000) {

          private static final long serialVersionUID = -5994059867039967783L;

          @Override
          public Status doPolling(DeploymentMessage deploymentId) throws PollingException {
            try {
              ImServiceImpl imService = OrchestratorContextBean.getBean(ImServiceImpl.class);
              if (imService.isDeployed(deploymentId)) {
                return Status.CREATE_COMPLETE;
              } else {
                return Status.CREATE_IN_PROGRESS;
              }
            } catch (DeploymentException de) {
              return Status.CREATE_FAILED;
            } catch (Exception ex) {
              throw new PollingException("Polling for deploy - error occured: " + ex.getMessage(),
                  ex);
            }
          }

          @Override
          public boolean pollExit(Status pollResult) {
            return pollResult != null && pollResult != Status.CREATE_IN_PROGRESS;
          }

          @Override
          public boolean pollSuccessful(DeploymentMessage params, Status pollResult) {
            return pollResult != null && pollResult == Status.CREATE_COMPLETE;
          }

        };

    return new ExternallyControlledPoller<DeploymentMessage, Status>(pollBehavior, 3);
  }

}
