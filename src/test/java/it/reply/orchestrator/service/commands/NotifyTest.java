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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

import it.reply.orchestrator.service.CallbackService;
import it.reply.orchestrator.utils.WorkflowConstants;

import java.util.UUID;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import org.flowable.engine.delegate.DelegateExecution;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(JUnitParamsRunner.class)
public class NotifyTest extends BaseJavaDelegateTest<Notify> {

  @Mock
  private CallbackService callbackService;

  public NotifyTest() {
    super(new Notify());
  }

  @Test
  @Parameters({ "true", "false" })
  public void doExecuteSuccesfully(boolean serviceResult) throws Exception {
    String deploymentId = UUID.randomUUID().toString();

    DelegateExecution delegateExecution = new ExecutionEntityBuilder()
        .withMockedVariable(WorkflowConstants.Param.DEPLOYMENT_ID, deploymentId)
        .build();

    when(callbackService.doCallback(deploymentId))
        .thenReturn(serviceResult);

    assertThatCode(() -> command.execute(delegateExecution))
        .doesNotThrowAnyException();

  }

  @Test
  public void doExecuteWithRuntimeException() throws Exception {
    String deploymentId = UUID.randomUUID().toString();

    DelegateExecution delegateExecution = new ExecutionEntityBuilder()
        .withMockedVariable(WorkflowConstants.Param.DEPLOYMENT_ID, deploymentId)
        .build();

    when(callbackService.doCallback(deploymentId))
        .thenThrow(new RuntimeException("some error"));

    assertThatCode(() -> command.execute(delegateExecution))
        .doesNotThrowAnyException();

  }

  @Test
  public void doExecuteWithMissingParam() throws Exception {

    DelegateExecution delegateExecution = new ExecutionEntityBuilder().build();

    assertThatCode(() -> command.execute(delegateExecution))
        .doesNotThrowAnyException();

  }
}
