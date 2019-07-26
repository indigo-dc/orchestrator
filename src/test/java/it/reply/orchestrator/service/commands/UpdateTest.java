/*
 * Copyright © 2015-2019 Santer Reply S.p.A.
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
public class UpdateTest extends BaseDeployCommandTest<Update> {

  public UpdateTest() {
    super(new Update());
  }

  @Test
  @Parameters({"true", "false"})
  public void testUpdate(boolean complete) throws Exception {
    DeploymentMessage dm = new DeploymentMessage();
    String updateTemplate = "template";
    ExecutionEntity execution = new ExecutionEntityBuilder()
        .withMockedVariable(WorkflowConstants.Param.TOSCA_TEMPLATE, updateTemplate)
        .withMockedVariable(WorkflowConstants.Param.DEPLOYMENT_MESSAGE, dm)
        .build();

    when(deploymentProviderService.doUpdate(dm, updateTemplate))
        .thenReturn(complete);

    command.execute(execution);

    assertThat(dm.isCreateComplete())
        .isEqualTo(complete);
  }

}
