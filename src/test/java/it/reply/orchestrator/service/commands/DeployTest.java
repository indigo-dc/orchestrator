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

import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.utils.WorkflowConstants;

import org.assertj.core.api.Assertions;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.junit.Test;
import org.mockito.Mockito;

public class DeployTest extends BaseDeployCommandTest<Deploy> {

  public DeployTest() {
    super(new Deploy());
  }

  @Test
  public void testDeployComplete() throws Exception {
    testDeploy(true);
  }

  @Test
  public void testDeployNotComplete() throws Exception {
    testDeploy(false);
  }

  public void testDeploy(boolean complete) throws Exception {
    DeploymentMessage dm = new DeploymentMessage();
    ExecutionEntity execution = new ExecutionEntityBuilder()
        .withMockedVariable(WorkflowConstants.Param.DEPLOYMENT_MESSAGE, dm)
        .build();

    Mockito.when(deploymentProviderService.doDeploy(dm)).thenReturn(complete);

    command.execute(execution);

    Assertions.assertThat(dm.isCreateComplete()).isEqualTo(complete);
  }

}
