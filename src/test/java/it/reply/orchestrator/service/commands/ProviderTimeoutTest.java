/*
 * Copyright © 2019 I.N.F.N.
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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.utils.WorkflowConstants;
import junitparams.JUnitParamsRunner;

import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class ProviderTimeoutTest  extends BaseDeployCommandTest<ProviderTimeout> {

  public ProviderTimeoutTest() {
    super(new ProviderTimeout());
  }

  @Test
  public void testProviderTimeoutSuccessful() {
    DeploymentMessage dm = new DeploymentMessage();
    ExecutionEntity execution = new ExecutionEntityBuilder()
        .withMockedVariable(WorkflowConstants.Param.DEPLOYMENT_MESSAGE, dm)
        .build();

    command.execute(execution);

    verify(deploymentProviderService, times(1)).doProviderTimeout(dm);

  }  
}
