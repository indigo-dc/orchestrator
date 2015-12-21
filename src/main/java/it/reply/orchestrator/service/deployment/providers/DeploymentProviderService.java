package it.reply.orchestrator.service.deployment.providers;

import org.springframework.scheduling.annotation.Async;

import it.reply.orchestrator.exception.DeploymentException;

public interface DeploymentProviderService {

  @Async
  public void doDeploy(String request);

  boolean doPoller() throws Exception;

  public boolean isDeployed() throws DeploymentException;
}
