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
import it.reply.orchestrator.utils.WorkflowConstants;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.mockito.Mockito;

public class UndeployTest extends BaseDeployCommandTest<Undeploy> {

  public UndeployTest() {
    super(new Undeploy());
  }

  @Test
  public void testUndeployComplete() throws Exception {
    testUndeploy(true);
  }

  @Test
  public void testUndeployNotComplete() throws Exception {
    testUndeploy(false);
  }

  public void testUndeploy(boolean complete) throws Exception {
    DeploymentMessage dm = new DeploymentMessage();
    CommandContext commandContext = TestCommandHelper
        .buildCommandContext()
        .withParam(WorkflowConstants.WF_PARAM_DEPLOYMENT_MESSAGE, dm)
        .get();

    Mockito.when(deploymentProviderService.doUndeploy(dm)).thenReturn(complete);

    ExecutionResults result = command.customExecute(commandContext);

    TestCommandHelper.assertBaseResults(true, result);
    Assertions.assertThat(dm.isDeleteComplete()).isEqualTo(complete);
  }

}
