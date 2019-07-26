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

package it.reply.orchestrator.service.deployment.providers;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;

import java.util.Iterator;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class DeploymentStatusHelperImpl implements DeploymentStatusHelper {

  private DeploymentRepository deploymentRepository;

  @Override
  public void updateOnError(String deploymentUuid, String message, Throwable throwable) {
    updateOnError(deploymentUuid, String.format("%s: %s", message, throwable.getMessage()));
  }

  @Override
  public void updateOnError(String deploymentUuid, Throwable throwable) {
    updateOnError(deploymentUuid, throwable.getMessage());
  }

  @Override
  public void updateOnError(String deploymentUuid, String message) {
    Deployment deployment = deploymentRepository.findOne(deploymentUuid);
    if (deployment == null) {
      LOG.error("Unable to set deployment <{}> on error with message {} as it has been deleted",
          deploymentUuid, message);
      return;
    }
    switch (deployment.getStatus()) {
      case CREATE_FAILED:
      case UPDATE_FAILED:
      case DELETE_FAILED:
        LOG.warn("Deployment < {} > was already in {} state.", deploymentUuid,
            deployment.getStatus());
        break;
      case CREATE_IN_PROGRESS:
      case CREATE_COMPLETE:
        deployment.setStatus(Status.CREATE_FAILED);
        break;
      case DELETE_IN_PROGRESS:
      case DELETE_COMPLETE:
        deployment.setStatus(Status.DELETE_FAILED);
        break;
      case UPDATE_IN_PROGRESS:
      case UPDATE_COMPLETE:
        deployment.setStatus(Status.UPDATE_FAILED);
        break;
      default:
        LOG.error("updateOnError: unsupported deployment status: {}. Setting status to {}",
            deployment.getStatus(), Status.UNKNOWN);
        deployment.setStatus(Status.UNKNOWN);
        break;
    }
    updateResources(deployment, deployment.getStatus());
    if (message != null) {
      deployment.setStatusReason(message);
    }
    deploymentRepository.save(deployment);
  }

  @Override
  public void updateOnSuccess(String deploymentUuid) {
    Deployment deployment = deploymentRepository.findOne(deploymentUuid);
    //// TODO will it be ever removed?
    if (deployment == null) {
      return;
    }
    if (deployment.getStatus() == Status.DELETE_IN_PROGRESS) {
      deploymentRepository.delete(deployment);
      return;
    }
    ///////////////////////////////
    switch (deployment.getStatus()) {
      case CREATE_COMPLETE:
      case DELETE_COMPLETE:
      case UPDATE_COMPLETE:
        LOG.warn("Deployment < {} > was already in {} state.", deploymentUuid,
            deployment.getStatus());
        break;
      case CREATE_IN_PROGRESS:
        deployment.setStatus(Status.CREATE_COMPLETE);
        break;
      case UPDATE_IN_PROGRESS:
        deployment.setStatus(Status.UPDATE_COMPLETE);
        break;
      default:
        LOG.error("updateOnSuccess: unsupported deployment status: {}. Setting status to {}",
            deployment.getStatus(), Status.UNKNOWN.toString());
        deployment.setStatus(Status.UNKNOWN);
        break;
    }
    updateResources(deployment, deployment.getStatus());
    deployment.setTask(Task.NONE);
    deployment.setStatusReason(null);
    deploymentRepository.save(deployment);
  }

  @Override
  public void updateResources(Deployment deployment, Status status) {
    Iterator<Resource> resourceIt = deployment.getResources().iterator();
    while (resourceIt.hasNext()) {
      Resource resource = resourceIt.next();
      switch (status) {
        case CREATE_COMPLETE:
          resource.setState(NodeStates.STARTED);
          break;
        case UPDATE_COMPLETE:
          if (resource.getState() == NodeStates.DELETING) {
            resourceIt.remove();
          } else {
            resource.setState(NodeStates.STARTED);
          }
          break;
        case DELETE_COMPLETE:
          resourceIt.remove();
          break;
        case CREATE_FAILED:
        case DELETE_FAILED:
          resource.setState(NodeStates.ERROR);
          break;
        case UPDATE_FAILED:
          if (resource.getState() != NodeStates.STARTED) {
            resource.setState(NodeStates.ERROR);
          }
          break;
        case CREATE_IN_PROGRESS:
          resource.setState(NodeStates.CREATING);
          break;
        case UPDATE_IN_PROGRESS:
          switch (resource.getState()) {
            case CREATING:
            case DELETING:
            case STARTED:
              break;
            default:
              resource.setState(NodeStates.CONFIGURING);
              break;
          }
          break;
        case DELETE_IN_PROGRESS:
          resource.setState(NodeStates.DELETING);
          break;
        case UNKNOWN:
        default:
          resource.setState(NodeStates.ERROR);
          break;
      }
    }
  }
}
