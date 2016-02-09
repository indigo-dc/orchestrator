package it.reply.orchestrator.service;

import it.reply.orchestrator.config.WorkflowConfigProducerBean;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.WorkflowReference;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dto.request.DeploymentRequest;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.exception.http.NotFoundException;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderService;
import it.reply.workflowManager.exceptions.WorkflowException;
import it.reply.workflowManager.orchestrator.bpm.BusinessProcessManager;
import it.reply.workflowManager.orchestrator.bpm.BusinessProcessManager.RUNTIME_STRATEGY;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.kie.api.runtime.process.ProcessInstance;
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
  @Transactional
  public Deployment createDeployment(DeploymentRequest request) {
    Deployment deployment = new Deployment();
    deployment.setStatus(Status.CREATE_IN_PROGRESS);
    deployment.setTask(Task.NONE);
    deployment.setParameters(request.getParameters());
    deployment.setTemplate(request.getTemplate());
    if (request.getCallback() != null) {
      deployment.setCallback(request.getCallback());
    }
    deployment = deploymentRepository.save(deployment);

    Map<String, Object> params = new HashMap<>();
    params.put("DEPLOYMENT_ID", deployment.getId());
    ProcessInstance pi = null;
    try {
      pi = wfService.startProcess(WorkflowConfigProducerBean.DEPLOY.getProcessId(), params,
          RUNTIME_STRATEGY.PER_PROCESS_INSTANCE);
    } catch (WorkflowException e) {
      throw new OrchestratorException(e);
    }
    deployment.addWorkflowReferences(
        new WorkflowReference(pi.getId(), RUNTIME_STRATEGY.PER_PROCESS_INSTANCE));
    deployment = deploymentRepository.save(deployment);
    return deployment;
  }

  @Override
  @Transactional
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
        Iterator<WorkflowReference> wrIt = deployment.getWorkflowReferences().iterator();
        while (wrIt.hasNext()) {
          WorkflowReference wr = wrIt.next();
          wfService.abortProcess(wr.getProcessId(), wr.getRuntimeStrategy());
          wrIt.remove();
        }
        deployment = deploymentRepository.save(deployment);

        Map<String, Object> params = new HashMap<>();
        params.put("DEPLOYMENT_ID", deployment.getId());
        ProcessInstance pi = null;
        try {
          pi = wfService.startProcess(WorkflowConfigProducerBean.UNDEPLOY.getProcessId(), params,
              RUNTIME_STRATEGY.PER_PROCESS_INSTANCE);
        } catch (WorkflowException e) {
          throw new OrchestratorException(e);
        }
        deployment.addWorkflowReferences(
            new WorkflowReference(pi.getId(), RUNTIME_STRATEGY.PER_PROCESS_INSTANCE));
        deployment = deploymentRepository.save(deployment);
      }
    } else {
      throw new NotFoundException("The deployment <" + uuid + "> doesn't exist");
    }
  }

}
