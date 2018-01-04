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
import it.reply.orchestrator.function.SerializableBiPredicate;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderService;

import org.springframework.stereotype.Component;

@Component
public class PollUndeploy extends AbstractPollingCommand<PollUndeploy> {

  @Override
  protected String getErrorMessagePrefix() {
    return "Error while checking the undeployment status";
  }

  @Override
  protected SerializableBiPredicate<DeploymentMessage, DeploymentProviderService>
      getPollingFunction() {
    return (DeploymentMessage deploymentMessage, DeploymentProviderService service) -> service
        .isUndeployed(deploymentMessage);
  }

}
