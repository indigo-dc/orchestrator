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
import it.reply.orchestrator.utils.JsonUtils;
import it.reply.orchestrator.utils.WorkflowConstants;

import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.mockito.Spy;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@JsonTest
public abstract class BaseRankCloudProvidersCommandTest<T extends BaseRankCloudProvidersCommand>
    extends BaseWorkflowCommandTest<RankCloudProvidersMessage, T> {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

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

  protected ExecutionEntity execute(RankCloudProvidersMessage rankCloudProvidersMessage) {

    ExecutionEntity execution = new ExecutionEntityBuilder()
        .withMockedVariable(WorkflowConstants.Param.RANK_CLOUD_PROVIDERS_MESSAGE,
            rankCloudProvidersMessage)
        .build();

    command.execute(execution);
    mockServer.verify();
    return execution;
  }

  protected ExecutionEntity execute(String rankCloudProvidersMessage) {
    try {
      return execute(
          JsonUtils.deserialize(rankCloudProvidersMessage, RankCloudProvidersMessage.class));
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

}
