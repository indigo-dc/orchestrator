package it.reply.orchestrator.service.deployment.providers;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.exception.service.DeploymentException;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Function;

public abstract class AbstractDeploymentProviderService implements DeploymentProviderService {

  @Autowired
  private DeploymentRepository deploymentRepository;

  @Autowired
  private DeploymentStatusHelper deploymentStatusHelper;

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

  protected DeploymentRepository getDeploymentRepository() {
    return deploymentRepository;
  }

  @Override
  public boolean doDeploy(String deploymentUuid) {
    Deployment deployment = deploymentRepository.findOne(deploymentUuid);
    return doDeploy(deployment);
  }

  @Override
  public boolean isDeployed(String deploymentUuid) throws DeploymentException {
    Deployment deployment = deploymentRepository.findOne(deploymentUuid);
    return isDeployed(deployment);
  }

  @Override
  public void finalizeDeploy(String deploymentUuid, boolean deployed) {
    Deployment deployment = deploymentRepository.findOne(deploymentUuid);
    finalizeDeploy(deployment, deployed);
  }

  @Override
  public boolean doUpdate(String deploymentId, String template) {
    Deployment deployment = deploymentRepository.findOne(deploymentId);
    return doUpdate(deployment, template);
  }

  @Override
  public boolean doUndeploy(String deploymentUuid) {
    Deployment deployment = deploymentRepository.findOne(deploymentUuid);
    return doUndeploy(deployment);
  }

  @Override
  public boolean isUndeployed(String deploymentUuid) throws DeploymentException {
    Deployment deployment = deploymentRepository.findOne(deploymentUuid);
    return isUndeployed(deployment);
  }

  @Override
  public void finalizeUndeploy(String deploymentUuid, boolean undeployed) {
    Deployment deployment = deploymentRepository.findOne(deploymentUuid);
    finalizeUndeploy(deployment, undeployed);
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
   *          the deployment id
   * @param message
   *          the error message
   */
  public void updateOnError(String deploymentUuid, String message) {
    deploymentStatusHelper.updateOnError(deploymentUuid, message);
  }

  /**
   * Update the status of a deployment successfully.
   */
  public void updateOnSuccess(String deploymentUuid) {
    deploymentStatusHelper.updateOnSuccess(deploymentUuid);
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
