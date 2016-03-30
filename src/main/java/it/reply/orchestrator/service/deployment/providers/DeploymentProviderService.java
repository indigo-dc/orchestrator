package it.reply.orchestrator.service.deployment.providers;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.exception.service.DeploymentException;

import java.util.function.Function;

public interface DeploymentProviderService {

  public void doDeploy(String request);

  public void doDeploy(Deployment deployment);

  public void doUpdate(String deploymentId, String template);

  public boolean doPoller(final Function<String[], Boolean> function, String[] params)
      throws Exception;

  public boolean isDeployed(String[] params) throws DeploymentException;

  public void doUndeploy(String deploymentUuid);

  public void doUndeploy(Deployment deployment);

  public boolean isUndeployed(String[] params) throws DeploymentException;

}
