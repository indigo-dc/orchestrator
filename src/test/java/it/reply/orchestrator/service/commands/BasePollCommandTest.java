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

import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderServiceRegistry;
import it.reply.orchestrator.util.TestUtil;
import it.reply.orchestrator.utils.WorkflowConstants;
import it.reply.utils.misc.polling.ExternallyControlledPoller;
import it.reply.workflowmanager.orchestrator.bpm.OrchestratorContext;
import it.reply.workflowmanager.spring.orchestrator.bpm.OrchestratorContextBean;
import it.reply.workflowmanager.utils.Constants;

import org.assertj.core.api.Assertions;
import org.drools.core.process.instance.impl.WorkItemImpl;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.context.support.StaticApplicationContext;

public abstract class BasePollCommandTest<T extends AbstractPollingCommand<T>>
    extends BaseDeployCommandTest<T> {

  private ExternallyControlledPoller<DeploymentMessage, Boolean> statusPoller;

  @Mock
  private StaticApplicationContext applicationContext;

  public BasePollCommandTest(T pollingCommand) {
    super(pollingCommand);
    statusPoller =
        AbstractPollingCommand.getPoller(Long.MAX_VALUE, pollingCommand.getPollingFunction());
  }

  @Before
  public void setupBasePollCommandTest() {
    @SuppressWarnings("unused") // needed to set up the static field
    OrchestratorContext orchestratorContext = new OrchestratorContextBean(applicationContext);
    Mockito.when(applicationContext.getBean(DeploymentProviderServiceRegistry.class)).thenReturn(
        deploymentProviderServiceRegistry);
  }

  @Test
  public void testCustomExecuteFalsePollingStatusNull() throws Exception {

    CommandContext commandContext = new CommandContext();
    DeploymentMessage dm = new DeploymentMessage();

    WorkItemImpl workItem = new WorkItemImpl();

    commandContext.setData(Constants.WORKITEM, workItem);

    ExecutionResults expectedResult = TestUtil.generateExpectedResult(true);

    ExecutionResults result = command.customExecute(commandContext, dm);

    TestUtil.assertBaseResults(expectedResult, result);
    Assertions.assertThat(WorkflowConstants.WF_PARAM_POLLING_STATUS).isNotNull();

  }

  @Test
  public void testCustomExecutePollingStatusNotNull() throws Exception {

    CommandContext commandContext = new CommandContext();
    DeploymentMessage dm = new DeploymentMessage();

    WorkItemImpl workItem = new WorkItemImpl();
    workItem.setParameter(WorkflowConstants.WF_PARAM_POLLING_STATUS, statusPoller);

    commandContext.setData(Constants.WORKITEM, workItem);

    ExecutionResults expectedResult = TestUtil.generateExpectedResult(true);
    expectedResult.setData(WorkflowConstants.WF_PARAM_POLLING_STATUS, statusPoller);

    ExecutionResults result = command.customExecute(commandContext, dm);

    TestUtil.assertBaseResults(expectedResult, result);
    Assertions.assertThat(WorkflowConstants.WF_PARAM_POLLING_STATUS).isNotNull();
  }

  @Test
  public void testCustomExecutePollComplete() throws Exception {

    CommandContext commandContext = new CommandContext();
    DeploymentMessage dm = new DeploymentMessage();

    WorkItemImpl workItem = new WorkItemImpl();
    workItem.setParameter(WorkflowConstants.WF_PARAM_POLLING_STATUS, statusPoller);

    commandContext.setData(Constants.WORKITEM, workItem);

    ExecutionResults expectedResult = TestUtil.generateExpectedResult(true);
    expectedResult.setData(WorkflowConstants.WF_PARAM_POLLING_STATUS, statusPoller);

    Mockito
        .when(command.getPollingFunction().apply(dm, deploymentProviderService))
        .thenReturn(true);

    ExecutionResults result = command.customExecute(commandContext, dm);

    TestUtil.assertBaseResults(expectedResult, result);
    Assertions.assertThat(dm.isPollComplete()).isTrue();
  }

  @Test
  public void testCustomExecuteNotPollComplete() throws Exception {

    CommandContext commandContext = new CommandContext();
    DeploymentMessage dm = new DeploymentMessage();

    WorkItemImpl workItem = new WorkItemImpl();
    workItem.setParameter(WorkflowConstants.WF_PARAM_POLLING_STATUS, statusPoller);

    commandContext.setData(Constants.WORKITEM, workItem);

    ExecutionResults expectedResult = TestUtil.generateExpectedResult(true);
    expectedResult.setData(WorkflowConstants.WF_PARAM_POLLING_STATUS, statusPoller);

    Mockito
        .when(command.getPollingFunction().apply(dm, deploymentProviderService))
        .thenReturn(false);

    ExecutionResults result = command.customExecute(commandContext, dm);

    TestUtil.assertBaseResults(expectedResult, result);
    Assertions.assertThat(dm.isPollComplete()).isFalse();
  }

  @Test
  public void testCustomExecuteException() throws Exception {

    CommandContext commandContext = new CommandContext();
    DeploymentMessage dm = new DeploymentMessage();

    WorkItemImpl workItem = new WorkItemImpl();
    workItem.setParameter(WorkflowConstants.WF_PARAM_POLLING_STATUS, statusPoller);

    commandContext.setData(Constants.WORKITEM, workItem);

    Mockito
        .when(command.getPollingFunction().apply(dm, deploymentProviderService))
        .thenThrow(new DeploymentException("some error"));

    Assertions
        .assertThatThrownBy(() -> command.customExecute(commandContext, dm))
        .isInstanceOf(DeploymentException.class)
        .hasMessage("some error");
  }

}
