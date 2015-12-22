package it.reply.orchestrator.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.dto.request.DeploymentRequest;
import it.reply.orchestrator.exception.http.NotFoudException;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderService;

@Service
public class DeploymentServiceImpl implements DeploymentService {

  @Autowired
  private DeploymentRepository deploymentRepository;

  // TODO no choice of the deploymentProvider
  @Autowired
  private DeploymentProviderService imService;

  @Override
  public Page<Deployment> getDeployments(Pageable pageable) {
    return deploymentRepository.findAll(pageable);
  }

  @Override
  public Deployment getDeployment(String uuid) {

    Deployment deployment = deploymentRepository.findOne(uuid);
    if (deployment != null) {
      return deployment;
    } else {
      throw new NotFoudException("The Deployment <" + uuid + "> doesn't exist");
    }
  }

  @Override
  public Deployment createDeployment(DeploymentRequest request) {

    Deployment deployment = new Deployment();
    deployment.setStatus(Status.CREATE_IN_PROGRESS);
    deployment.setTask(Task.NONE);
    deployment.setParameters(request.getParameters());
    deployment.setTemplate(request.getTemplate());
    deployment = deploymentRepository.save(deployment);

    imService.doDeploy(deployment.getId());
    return deployment;
  }

  @Override
  public void deleteDeployment(String uuid) {
    Deployment deployment = deploymentRepository.findOne(uuid);
    if (deployment != null) {
      deploymentRepository.delete(deployment);
    } else {
      throw new NotFoudException("The Deployment <" + uuid + "> doesn't exist");
    }
  }

}
