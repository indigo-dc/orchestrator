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

import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.utils.WorkflowConstants;
import it.reply.utils.json.JsonUtility;

import org.junit.Before;
import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.mockito.Spy;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

public abstract class BaseRankCloudProvidersCommandTest<T extends BaseRankCloudProvidersCommand<T>>
    extends BaseWorkflowCommandTest<RankCloudProvidersMessage, T> {

  protected MockRestServiceServer mockServer;

  @Spy
  protected RestTemplate restTemplate;

  public BaseRankCloudProvidersCommandTest(T command) {
    super(command);
  }

  @Before
  public void baseSetup() {
    mockServer = MockRestServiceServer.createServer(restTemplate);
  }

  protected ExecutionResults executeCommand(RankCloudProvidersMessage rankCloudProvidersMessage)
      throws Exception {

    CommandContext commandContext = TestCommandHelper
        .buildCommandContext()
        .withParam(WorkflowConstants.WF_PARAM_RANK_CLOUD_PROVIDERS_MESSAGE,
            rankCloudProvidersMessage)
        .get();

    ExecutionResults er = command.customExecute(commandContext);
    mockServer.verify();
    return er;
  }

  protected ExecutionResults executeCommand(String rankCloudProvidersMessage)
      throws Exception {
    return executeCommand(
        JsonUtility.deserializeJson(rankCloudProvidersMessage, RankCloudProvidersMessage.class));
  }

}
