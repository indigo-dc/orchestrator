package it.reply.orchestrator.service.deployment.providers;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeploymentStatusHelperImpl implements DeploymentStatusHelper {

  private static final Logger LOG =
      LoggerFactory.getLogger(AbstractDeploymentProviderService.class);

  @Autowired
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
    switch (deployment.getStatus()) {
      case CREATE_FAILED:
      case UPDATE_FAILED:
      case DELETE_FAILED:
        LOG.warn("Deployment < {} > was already in {} state.", deploymentUuid,
            deployment.getStatus());
        break;
      case CREATE_IN_PROGRESS:
        deployment.setStatus(Status.CREATE_FAILED);
        break;
      case DELETE_IN_PROGRESS:
        deployment.setStatus(Status.DELETE_FAILED);
        break;
      case UPDATE_IN_PROGRESS:
        deployment.setStatus(Status.UPDATE_FAILED);
        break;
      default:
        LOG.error("updateOnError: unsupported deployment status: {}. Setting status to {}",
            deployment.getStatus(), Status.UNKNOWN.toString());
        deployment.setStatus(Status.UNKNOWN);
        break;
    }
    deployment.setTask(Task.NONE);
    if (message != null) {
      deployment.setStatusReason(message);
    }
    deploymentRepository.save(deployment);
  }

  @Override
  public void updateOnSuccess(String deploymentUuid) {
    Deployment deployment = deploymentRepository.findOne(deploymentUuid);
    if (deployment.getStatus() == Status.DELETE_IN_PROGRESS) {
      deploymentRepository.delete(deployment);
    } else {
      switch (deployment.getStatus()) {
        case CREATE_COMPLETE:
        case DELETE_COMPLETE:
        case UPDATE_COMPLETE:
          LOG.warn("Deployment < {} > was already in {} state.", deploymentUuid,
              deployment.getStatus());
          break;
        case CREATE_IN_PROGRESS:
          deployment.setStatus(Status.CREATE_COMPLETE);
          updateResources(deployment, Status.CREATE_COMPLETE);
          break;
        case UPDATE_IN_PROGRESS:
          deployment.setStatus(Status.UPDATE_COMPLETE);
          updateResources(deployment, Status.UPDATE_COMPLETE);
          break;
        default:
          LOG.error("updateOnSuccess: unsupported deployment status: {}. Setting status to {}",
              deployment.getStatus(), Status.UNKNOWN.toString());
          deployment.setStatus(Status.UNKNOWN);
          updateResources(deployment, Status.UNKNOWN);
          break;
      }
      deployment.setTask(Task.NONE);
      deployment.setStatusReason(null);
      deploymentRepository.save(deployment);
    }
  }

  protected void updateResources(Deployment deployment, Status status) {

    for (Resource resource : deployment.getResources()) {
      if (status.equals(Status.CREATE_COMPLETE) || status.equals(Status.UPDATE_COMPLETE)) {
        switch (resource.getState()) {
          case INITIAL:
          case CREATING:
          case CREATED:
          case CONFIGURING:
          case CONFIGURED:
          case STARTING:
            resource.setState(NodeStates.STARTED);
            break;
          case STARTED:
            break;
          case DELETING:
            // Resource should be deleted into bindresource function
            resource.setState(NodeStates.ERROR);
            break;
          default:
            resource.setState(NodeStates.ERROR);
            break;
        }
      } else {
        switch (resource.getState()) {
          case INITIAL:
          case CREATING:
          case CREATED:
          case CONFIGURING:
          case CONFIGURED:
          case STARTING:
          case STOPPING:
          case DELETING:
            resource.setState(NodeStates.ERROR);
            break;
          default:
            break;
        }
      }
    }
  }
}
