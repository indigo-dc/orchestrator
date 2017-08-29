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

import static org.mockito.Mockito.*;

import it.reply.orchestrator.service.CallbackService;
import it.reply.orchestrator.utils.WorkflowConstants;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.UUID;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class NotifyTest {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @InjectMocks
  private Notify notifyCommand;

  @Mock
  private CallbackService callbackService;

  @Test
  @Parameters({ "true", "false" })
  public void doExecuteSuccesfully(boolean serviceResult) throws Exception {
    String deploymentId = UUID.randomUUID().toString();

    CommandContext ctx = TestCommandHelper
        .buildCommandContext()
        .withParam(WorkflowConstants.WF_PARAM_DEPLOYMENT_ID, deploymentId)
        .get();

    when(callbackService.doCallback(deploymentId)).thenReturn(serviceResult);

    ExecutionResults result = notifyCommand.customExecute(ctx);

    TestCommandHelper.assertBaseResults(serviceResult, result);

  }

  @Test
  public void doExecuteWithRuntimeException() throws Exception {
    String deploymentId = UUID.randomUUID().toString();

    CommandContext ctx = TestCommandHelper
        .buildCommandContext()
        .withParam(WorkflowConstants.WF_PARAM_DEPLOYMENT_ID, deploymentId)
        .get();

    when(callbackService.doCallback(deploymentId)).thenThrow(new RuntimeException("some error"));

    ExecutionResults result = notifyCommand.customExecute(ctx);

    TestCommandHelper.assertBaseResults(false, result);

  }

}
