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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderServiceRegistry;
import it.reply.orchestrator.utils.WorkflowConstants;
import it.reply.utils.misc.polling.ExternallyControlledPoller;
import it.reply.workflowmanager.orchestrator.bpm.OrchestratorContext;
import it.reply.workflowmanager.spring.orchestrator.bpm.OrchestratorContextBean;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.context.support.StaticApplicationContext;

import java.util.concurrent.TimeoutException;

public abstract class BasePollCommandTest<T extends AbstractPollingCommand<T>>
    extends BaseDeployCommandTest<T> {

  private ExternallyControlledPoller<DeploymentMessage, Boolean> statusPoller;

  @Mock
  private StaticApplicationContext applicationContext;

  public BasePollCommandTest(T pollingCommand) {
    super(pollingCommand);
    statusPoller = command.getPoller(Long.MAX_VALUE);
  }

  @Before
  public void setupBasePollCommandTest() {
    @SuppressWarnings("unused") // needed to set up the static field
    OrchestratorContext orchestratorContext = new OrchestratorContextBean(applicationContext);

    when(applicationContext.getBean(DeploymentProviderServiceRegistry.class))
        .thenReturn(deploymentProviderServiceRegistry);
  }

  @Test
  public void testPollingFunctionIsSerializable() {
    assertThat(SerializationUtils.clone(command.getPollingFunction()))
        .isEqualToComparingFieldByFieldRecursively(command.getPollingFunction());
  }

  @Test
  public void testPollerIsSerializable() {
    assertThat(SerializationUtils.clone(command.getPoller(0)))
        .isEqualToComparingFieldByFieldRecursively(command.getPoller(0));
  }

  @Test
  public void testCustomExecutePollingStatusNull() throws Exception {
    DeploymentMessage dm = new DeploymentMessage();
    CommandContext commandContext = TestCommandHelper
        .buildCommandContext()
        .withParam(WorkflowConstants.WF_PARAM_DEPLOYMENT_MESSAGE, dm)
        .get();

    ExecutionResults result = command.customExecute(commandContext);
    verify(command, times(1)).getPoller(anyLong());

    TestCommandHelper.assertBaseResults(true, result);
    assertThat(result.getData(WorkflowConstants.WF_PARAM_POLLING_STATUS)).isNotNull();

  }

  @Test
  public void testCustomExecutePollingStatusNotNull() throws Exception {
    DeploymentMessage dm = new DeploymentMessage();
    CommandContext commandContext = TestCommandHelper
        .buildCommandContext()
        .withParam(WorkflowConstants.WF_PARAM_POLLING_STATUS, statusPoller)
        .withParam(WorkflowConstants.WF_PARAM_DEPLOYMENT_MESSAGE, dm)
        .get();

    ExecutionResults result = command.customExecute(commandContext);
    verify(command, never()).getPoller(anyLong());

    TestCommandHelper.assertBaseResults(true, result);
    assertThat(result.getData(WorkflowConstants.WF_PARAM_POLLING_STATUS)).isNotNull();
  }

  public void testCustomExecuteSuccessful(boolean pollComplete) throws Exception {
    DeploymentMessage dm = new DeploymentMessage();
    CommandContext commandContext = TestCommandHelper
        .buildCommandContext()
        .withParam(WorkflowConstants.WF_PARAM_POLLING_STATUS, statusPoller)
        .withParam(WorkflowConstants.WF_PARAM_DEPLOYMENT_MESSAGE, dm)
        .get();

    when(command.getPollingFunction().test(dm, deploymentProviderService)).thenReturn(pollComplete);

    ExecutionResults result = command.customExecute(commandContext);

    TestCommandHelper.assertBaseResults(true, result);
    assertThat(dm.isPollComplete()).isEqualTo(pollComplete);
  }

  @Test
  public void testCustomExecuteSuccessfulAndComplete() throws Exception {
    testCustomExecuteSuccessful(true);
  }

  @Test
  public void testCustomExecuteSuccessfulAndNotComplete() throws Exception {
    testCustomExecuteSuccessful(false);
  }

  @Test
  public void testCustomExecuteMaxRetryException() throws Exception {
    DeploymentMessage dm = new DeploymentMessage();
    CommandContext commandContext = TestCommandHelper
        .buildCommandContext()
        .withParam(WorkflowConstants.WF_PARAM_POLLING_STATUS, statusPoller)
        .withParam(WorkflowConstants.WF_PARAM_DEPLOYMENT_MESSAGE, dm)
        .get();

    when(command.getPollingFunction().test(dm, deploymentProviderService))
        .thenThrow(new DeploymentException("some error"));

    ExecutionResults result = command.customExecute(commandContext);

    String message = command.getErrorMessagePrefix();
    ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
    verify(deploymentStatusHelper).updateOnError(eq(dm.getDeploymentId()),
        eq(message), captor.capture());
    assertThat(captor.getValue())
        .isInstanceOf(DeploymentException.class)
        .hasMessage("some error");

    TestCommandHelper.assertBaseResults(false, result);

  }

  @Test
  public void testCustomExecuteTimeoutException() throws Exception {
    DeploymentMessage dm = new DeploymentMessage();
    CommandContext commandContext = TestCommandHelper
        .buildCommandContext()
        .withParam(WorkflowConstants.WF_PARAM_POLLING_STATUS, command.getPoller(Long.MIN_VALUE))
        .withParam(WorkflowConstants.WF_PARAM_DEPLOYMENT_MESSAGE, dm)
        .get();

    ExecutionResults result = command.customExecute(commandContext);

    String message = command.getErrorMessagePrefix();
    ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
    verify(deploymentStatusHelper).updateOnError(eq(dm.getDeploymentId()),
        eq(message), captor.capture());
    assertThat(captor.getValue()).isInstanceOf(TimeoutException.class);

    TestCommandHelper.assertBaseResults(false, result);

  }
}
