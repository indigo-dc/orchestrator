package it.reply.orchestrator.service.deployment.providers;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.exception.service.DeploymentException;

import java.util.function.Function;

public interface DeploymentProviderService {

  public boolean doDeploy(String deploymentUuid);

  public boolean doDeploy(Deployment deployment);

  public boolean isDeployed(String deploymentUuid) throws DeploymentException;

  public boolean isDeployed(Deployment deployment) throws DeploymentException;

  public void finalizeDeploy(String deploymentUuid, boolean deployed);

  public void finalizeDeploy(Deployment deployment, boolean deployed);

  public void doUpdate(String deploymentId, String template);

  public void doUpdate(Deployment deployment, String template);

  public boolean doUndeploy(String deploymentUuid);

  public boolean doUndeploy(Deployment deployment);

  public boolean isUndeployed(String deploymentUuid) throws DeploymentException;

  public boolean isUndeployed(Deployment deployment) throws DeploymentException;

  public void finalizeUndeploy(String deploymentUuid, boolean undeployed);

  public boolean doPoller(final Function<Deployment, Boolean> function, Deployment deployment)
      throws Exception;

  public boolean doPoller(final Function<Resource, Boolean> function, Resource resource);

}
