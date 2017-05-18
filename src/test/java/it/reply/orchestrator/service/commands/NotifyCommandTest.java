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

import static org.junit.Assert.assertEquals;

import it.reply.orchestrator.command.TestCommandHelper;
import it.reply.orchestrator.service.CallbackService;
import it.reply.orchestrator.utils.WorkflowConstants;
import it.reply.workflowmanager.utils.Constants;

import org.junit.Before;
import org.junit.Test;
import org.kie.api.executor.CommandContext;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

public class NotifyCommandTest {

  @Mock
  private CallbackService callbackService;

  private Notify notifyCommand;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    notifyCommand = new Notify(callbackService);
  }

  @Test
  public void doExecuteSuccesfully() throws Exception {
    String deploymentId = UUID.randomUUID().toString();

    CommandContext ctx = TestCommandHelper.buildCommandContext()
        .withParam(WorkflowConstants.WF_PARAM_DEPLOYMENT_ID, deploymentId)
        .get();

    Mockito.when(callbackService.doCallback(deploymentId)).thenReturn(true);
    boolean result = (boolean) notifyCommand.customExecute(ctx).getData(Constants.RESULT);
    assertEquals(true, result);

  }

  @Test
  public void doExecuteWithoutUrlSuccesfully() throws Exception {
    String deploymentId = UUID.randomUUID().toString();

    CommandContext ctx = TestCommandHelper.buildCommandContext()
        .withParam(WorkflowConstants.WF_PARAM_DEPLOYMENT_ID, deploymentId)
        .get();

    Mockito.when(callbackService.doCallback(deploymentId)).thenThrow(new RuntimeException());
    boolean result = (boolean) notifyCommand.customExecute(ctx).getData(Constants.RESULT);
    assertEquals(false, result);

  }

}
