package it.reply.orchestrator.service.deployment.providers;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;

import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractDeploymentProviderService implements DeploymentProviderService {

  @Autowired
  private DeploymentRepository deploymentRepository;

  @Override
  public boolean doPoller() {
    int maxRetry = 10;
    int sleepInterval = 30000;
    while (maxRetry > 0 && !isDeployed()) {
      try {
        Thread.sleep(sleepInterval);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      maxRetry--;
    }
    return isDeployed();
  }

  public void updateOnError(String deploymentUuid) {
    Deployment deployment = deploymentRepository.findOne(deploymentUuid);
    deployment.setStatus(Status.CREATE_FAILED);
    deployment.setTask(Task.NONE);
    deploymentRepository.save(deployment);
  }

  public void updateOnSuccess(String deploymentUuid) {
    Deployment deployment = deploymentRepository.findOne(deploymentUuid);
    deployment.setStatus(Status.CREATE_COMPLETE);
    deployment.setTask(Task.NONE);
    deploymentRepository.save(deployment);
  }

}
