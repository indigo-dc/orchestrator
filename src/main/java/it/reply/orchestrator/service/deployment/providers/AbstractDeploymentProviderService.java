/*
 * Copyright © 2015-2021 I.N.F.N.
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
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dto.deployment.ActionMessage;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.enums.Status;

import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class AbstractDeploymentProviderService implements DeploymentProviderService {

  @Autowired
  private DeploymentRepository deploymentRepository;

  @Autowired
  private DeploymentStatusHelper deploymentStatusHelper;

  protected Deployment getDeployment(DeploymentMessage deploymentMessage) {
    return deploymentRepository.findOne(deploymentMessage.getDeploymentId());
  }

  protected Deployment getDeployment(ActionMessage deploymentMessage) {
    return deploymentRepository.findOne(deploymentMessage.getDeploymentId());
  }

  public void updateOnError(String deploymentUuid, Throwable throwable) {
    deploymentStatusHelper.updateOnError(deploymentUuid, throwable);
  }

  public void updateOnError(String deploymentUuid, String message, Throwable throwable) {
    deploymentStatusHelper.updateOnError(deploymentUuid, message, throwable);
  }

  /**
   * Update the status of the deployment with an error message.
   *
   * @param deploymentUuid
   *          the deployment id
   * @param message
   *          the error message
   */
  public void updateOnError(String deploymentUuid, String message) {
    deploymentStatusHelper.updateOnError(deploymentUuid, message);
  }

  /**
   * Update the status of a deployment successfully.
   *
   *
   * @param deploymentUuid the deployment UUID
   */
  public void updateOnSuccess(String deploymentUuid) {
    deploymentStatusHelper.updateOnSuccess(deploymentUuid);
  }

  protected void updateResources(Deployment deployment, Status status) {
    deploymentStatusHelper.updateResources(deployment, status);
  }

  @Override
  public void finalizeDeploy(DeploymentMessage deploymentMessage) {
    updateOnSuccess(deploymentMessage.getDeploymentId());
  }

  @Override
  public void finalizeUndeploy(DeploymentMessage deploymentMessage) {
    updateOnSuccess(deploymentMessage.getDeploymentId());
  }

  @Override
  public Optional<String> getAdditionalErrorInfo(DeploymentMessage deploymentMessage) {
    try {
      return getAdditionalErrorInfoInternal(deploymentMessage);
    } catch (RuntimeException ex) {
      LOG.error("Error while retrieving additional error info for deployment {}",
          deploymentMessage.getDeploymentId(), ex);
      return Optional.empty();
    }
  }

  protected abstract Optional<String>
      getAdditionalErrorInfoInternal(DeploymentMessage deploymentMessage);

  @Override
  public Optional<String> getDeploymentLog(DeploymentMessage deploymentMessage) {

    try {
      return getDeploymentLogInternal(deploymentMessage);
    } catch (RuntimeException ex) {
      LOG.error("Error while retrieving infrastructure log for deployment {}",
          deploymentMessage.getDeploymentId(), ex);
      return Optional.empty();
    }
  }

  protected abstract Optional<String>
      getDeploymentLogInternal(DeploymentMessage deploymentMessage);

  @Override
  public Optional<String> getDeploymentExtendedInfo(DeploymentMessage deploymentMessage) {

    try {
      return getDeploymentExtendedInfoInternal(deploymentMessage);
    } catch (RuntimeException ex) {
      LOG.error("Error while retrieving additional info for deployment {}",
          deploymentMessage.getDeploymentId(), ex);
      return Optional.empty();
    }
  }

  protected abstract Optional<String>
      getDeploymentExtendedInfoInternal(DeploymentMessage deploymentMessage);
}
