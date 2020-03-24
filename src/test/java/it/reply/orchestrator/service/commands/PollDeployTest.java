/*
 * Copyright © 2015-2020 Santer Reply S.p.A.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.utils.WorkflowConstants;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class PollDeployTest extends BaseDeployCommandTest<PollDeploy> {

  public PollDeployTest() {
    super(new PollDeploy());
  }

  @Test
  @Parameters({ "true", "false" })
  public void testPollingSuccessful(boolean pollingComplete) {
    DeploymentMessage dm = new DeploymentMessage();
    ExecutionEntity execution = new ExecutionEntityBuilder()
        .withMockedVariable(WorkflowConstants.Param.DEPLOYMENT_MESSAGE, dm)
        .build();

    when(deploymentProviderService.isDeployed(dm))
        .thenReturn(pollingComplete);

    command.execute(execution);

    assertThat(dm.isPollComplete())
        .isEqualTo(pollingComplete);
  }
}
