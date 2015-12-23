package it.reply.orchestrator.service.deployment.providers;

import java.util.function.Function;

import org.springframework.scheduling.annotation.Async;

import it.reply.orchestrator.exception.service.DeploymentException;

public interface DeploymentProviderService {

  @Async
  public void doDeploy(String request);

  public boolean doPoller(String deploymentUuid, final Function<String, Boolean> function)
      throws Exception;

  public boolean isDeployed(String deploymentUuid) throws DeploymentException;

  @Async
  public void doUndeploy(String deploymentUuid);

  public boolean isUndeployed(String deploymentUuid) throws DeploymentException;
}
