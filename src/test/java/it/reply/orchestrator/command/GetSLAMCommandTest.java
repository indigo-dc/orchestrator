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

package it.reply.orchestrator.command;

import static org.junit.Assert.assertEquals;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;

import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.service.SlamService;
import it.reply.orchestrator.service.commands.BaseRankCloudProvidersCommand;
import it.reply.orchestrator.service.commands.GetSlam;
import it.reply.orchestrator.workflow.RankCloudProvidersWorkflowTest;

import org.junit.Test;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;

@DatabaseTearDown("/data/database-empty.xml")
public class GetSLAMCommandTest extends BaseRankCloudProviderCommandTest {

  @Autowired
  private GetSlam getSLAMCommand;

  @Autowired
  private SlamService slamService;

  @Override
  protected BaseRankCloudProvidersCommand getCommand() {
    return getSLAMCommand;
  }

  @Test
  @DatabaseSetup("/data/database-init.xml")
  public void doexecuteSuccesfully() throws Exception {

    RankCloudProvidersWorkflowTest.mockSlam(mockServer, slamService.getUrl());

    ExecutionResults er = executeCommand(new RankCloudProvidersMessage(getDeploymentId()));

    assertEquals(true, commandSucceeded(er));
  }

}
