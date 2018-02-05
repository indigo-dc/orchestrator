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

import it.reply.orchestrator.dto.deployment.BaseWorkflowMessage;
import it.reply.orchestrator.service.deployment.providers.DeploymentStatusHelper;

import org.junit.Rule;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public abstract class BaseWorkflowCommandTest<M extends BaseWorkflowMessage, T extends BaseWorkflowCommand<M>> {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @InjectMocks
  @Spy
  protected T command;

  @Mock
  protected DeploymentStatusHelper deploymentStatusHelper;

  public BaseWorkflowCommandTest(T command) {
    this.command = command;
  }

}
