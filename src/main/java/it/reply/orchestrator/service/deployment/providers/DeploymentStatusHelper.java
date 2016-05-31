package it.reply.orchestrator.service.deployment.providers;

public interface DeploymentStatusHelper {

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
}
