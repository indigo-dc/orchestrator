package it.reply.orchestrator.service.deployment.providers;

import java.util.function.Function;

import alien4cloud.tosca.model.ArchiveRoot;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.exception.service.DeploymentException;

public interface DeploymentProviderService {

  public void doDeploy(String request);

  public void doDeploy(Deployment deployment);

  public void doUpdate(String deploymentId, String template);

  public boolean doPoller(
      final it.reply.orchestrator.service.deployment.providers.Function<String[], Deployment, Boolean> function,
      String[] params, Deployment deployment) throws Exception;

  public boolean isDeployed(String[] params, Deployment deployment) throws DeploymentException;

  public void doUndeploy(String deploymentUuid);

  public void doUndeploy(Deployment deployment);

  public boolean isUndeployed(String[] params, Deployment deployment) throws DeploymentException;

}
