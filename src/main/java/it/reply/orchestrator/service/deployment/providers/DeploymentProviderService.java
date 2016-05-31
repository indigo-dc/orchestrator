package it.reply.orchestrator.service.deployment.providers;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.exception.service.DeploymentException;

import java.util.function.Function;

public interface DeploymentProviderService {

  /**
   * See {@link #doDeploy(Deployment)}
   * 
   * @param deploymentUuid
   * @return
   */
  public boolean doDeploy(String deploymentUuid);

  /**
   * Executes the deployment of the given <code>deployment</code>, which typically means
   * instantiating the required resources on the underlying system (i.e. IaaS, other service
   * clusters, etc). <br/>
   * It should handle every error internally (i.e. not throwing exceptions) and just return a
   * boolean indicating the result or the failure. <br/>
   * It should also handle the deployment status update internally.
   * 
   * @param deployment
   * @return
   */
  public boolean doDeploy(Deployment deployment);

  public boolean isDeployed(String deploymentUuid) throws DeploymentException;

  /**
   * 
   * @param deployment
   * @return
   * @throws DeploymentException
   *           if the deployment fails.
   */
  public boolean isDeployed(Deployment deployment) throws DeploymentException;

  public void finalizeDeploy(String deploymentUuid, boolean deployed);

  public void finalizeDeploy(Deployment deployment, boolean deployed);

  public boolean doUpdate(String deploymentId, String template);

  public boolean doUpdate(Deployment deployment, String template);

  public boolean doUndeploy(String deploymentUuid);

  public boolean doUndeploy(Deployment deployment);

  public boolean isUndeployed(String deploymentUuid) throws DeploymentException;

  public boolean isUndeployed(Deployment deployment) throws DeploymentException;

  public void finalizeUndeploy(String deploymentUuid, boolean undeployed);

  public void finalizeUndeploy(Deployment deployment, boolean undeployed);

  /**
   * @deprecated Use WF-implemented poller instead (just use isDeployed for the updated deployment
   *             status)
   * @param function
   * @param deployment
   * @return
   * @throws Exception
   */
  @Deprecated
  public boolean doPoller(final Function<Deployment, Boolean> function, Deployment deployment)
      throws Exception;

  /**
   * @deprecated See {@link #doPoller(Function, Deployment)}
   * @param function
   * @param resource
   * @return
   */
  @Deprecated
  public boolean doPoller(final Function<Resource, Boolean> function, Resource resource);

}
