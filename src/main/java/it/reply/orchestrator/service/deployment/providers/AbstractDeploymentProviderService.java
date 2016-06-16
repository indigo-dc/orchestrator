package it.reply.orchestrator.service.deployment.providers;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Status;

import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractDeploymentProviderService implements DeploymentProviderService {

  @Autowired
  private DeploymentRepository deploymentRepository;

  @Autowired
  private DeploymentStatusHelper deploymentStatusHelper;

  protected DeploymentRepository getDeploymentRepository() {
    return deploymentRepository;
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
