package it.reply.orchestrator.service.deployment.providers;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Function;

public abstract class AbstractDeploymentProviderService implements DeploymentProviderService {

  private static final Logger LOG = LogManager.getLogger(AbstractDeploymentProviderService.class);

  @Autowired
  private DeploymentRepository deploymentRepository;

  @Override
  public boolean doPoller(final Function<Deployment, Boolean> function, Deployment deployment) {
    int maxRetry = 10;
    int sleepInterval = 30000;
    Boolean isDone = false;
    while (maxRetry > 0 && !(isDone = function.apply(deployment))) {
      try {
        Thread.sleep(sleepInterval);
      } catch (InterruptedException ex) {
        ex.printStackTrace();
      }
      maxRetry--;
    }
    return isDone;
  }

  @Override
  public boolean doPoller(final Function<Resource, Boolean> function, Resource resource) {
    int maxRetry = 10;
    int sleepInterval = 30000;
    Boolean isDone = false;
    while (maxRetry > 0 && !(isDone = function.apply(resource))) {
      try {
        Thread.sleep(sleepInterval);
      } catch (InterruptedException ex) {
        ex.printStackTrace();
      }
      maxRetry--;
    }
    return isDone;
  }

  public void updateOnError(String deploymentUuid) {
    updateOnError(deploymentUuid, (String) null);
  }

  public void updateOnError(String deploymentUuid, Throwable throwable) {
    updateOnError(deploymentUuid, throwable.getMessage());
  }

  /**
   * Update the status of the deployment with an error message.
   * 
   * @param deploymentUuid
   *          the deplyment id
   * @param message
   *          the error message
   */
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
        updateResources(deployment, Status.CREATE_FAILED);
        break;
      case DELETE_IN_PROGRESS:
        deployment.setStatus(Status.DELETE_FAILED);
        updateResources(deployment, Status.DELETE_FAILED);
        break;
      case UPDATE_IN_PROGRESS:
        deployment.setStatus(Status.UPDATE_FAILED);
        updateResources(deployment, Status.UPDATE_FAILED);
        break;
      default:
        LOG.error("updateOnError: unsupported deployment status: {}. Setting status to {}",
            deployment.getStatus(), Status.UNKNOWN.toString());
        deployment.setStatus(Status.UNKNOWN);
        updateResources(deployment, Status.UNKNOWN);
        break;
    }
    deployment.setTask(Task.NONE);
    deployment.setStatusReason(message);
    deploymentRepository.save(deployment);
  }

  /**
   * Update the status of a deployment successfully.
   */
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

  private void updateResources(Deployment deployment, Status status) {
    for (Resource r : deployment.getResources()) {
      r.setStatus(status);
    }
  }
}
