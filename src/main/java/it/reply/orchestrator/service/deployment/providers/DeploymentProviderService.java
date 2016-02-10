package it.reply.orchestrator.service.deployment.providers;

import java.util.function.Function;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.exception.service.DeploymentException;

public interface DeploymentProviderService {

  public void doDeploy(String request);

  public void doDeploy(Deployment deployment);

  public boolean doPoller(String deploymentUuid, final Function<String, Boolean> function)
      throws Exception;

  public boolean isDeployed(String deploymentUuid) throws DeploymentException;

  public void doUndeploy(String deploymentUuid);

  public void doUndeploy(Deployment deployment);

  public boolean isUndeployed(String deploymentUuid) throws DeploymentException;
}
