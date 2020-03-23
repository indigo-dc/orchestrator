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

package it.reply.orchestrator.service.deployment.providers;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.enums.Status;

public interface DeploymentStatusHelper {

  public void updateOnError(String deploymentUuid, String message, Throwable throwable);

  public void updateOnError(String deploymentUuid, Throwable throwable);

  /**
   * Update the status of the deployment with an error message.
   *
   * @param deploymentUuid
   *          the deployment id
   * @param message
   *          the error message
   */
  public void updateOnError(String deploymentUuid, String message);

  /**
   * Update the status of a deployment successfully.
   */
  public void updateOnSuccess(String deploymentUuid);

  public void updateResources(Deployment deployment, Status status);
}
