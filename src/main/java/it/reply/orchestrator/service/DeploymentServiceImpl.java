package it.reply.orchestrator.service;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dto.request.DeploymentRequest;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.exception.http.NotFoundException;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderService;
import it.reply.workflowManager.orchestrator.bpm.BusinessProcessManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeploymentServiceImpl implements DeploymentService {

  @Autowired
  private DeploymentRepository deploymentRepository;

  // TODO no choice of the deploymentProvider
  @Autowired
  private DeploymentProviderService imService;

  @Autowired
  private BusinessProcessManager wfService;

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
      throw new NotFoundException("The deployment <" + uuid + "> doesn't exist");
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
    try {
      wfService.startProcess("defaultPackage.New_Process", null,
          BusinessProcessManager.RUNTIME_STRATEGY.PER_PROCESS_INSTANCE);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return deployment;
  }

  @Override
  public void deleteDeployment(String uuid) {
    Deployment deployment = deploymentRepository.findOne(uuid);
    if (deployment != null) {
      if (deployment.getStatus() == Status.DELETE_COMPLETE
          || deployment.getStatus() == Status.DELETE_IN_PROGRESS) {
        throw new IllegalStateException(
            String.format("Deployment already in %s state.", deployment.getStatus().toString()));
      } else {
        deployment.setStatus(Status.DELETE_IN_PROGRESS);
        deployment.setTask(Task.NONE);
        deployment = deploymentRepository.save(deployment);

        imService.doUndeploy(deployment.getId());
      }
    } else {
      throw new NotFoundException("The deployment <" + uuid + "> doesn't exist");
    }
  }

}
