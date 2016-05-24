package it.reply.orchestrator.service;

import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;

import it.reply.orchestrator.config.WorkflowConfigProducerBean;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.entity.WorkflowReference;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.request.DeploymentRequest;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.exception.http.BadRequestException;
import it.reply.orchestrator.exception.http.ConflictException;
import it.reply.orchestrator.exception.http.NotFoundException;
import it.reply.orchestrator.exception.http.OrchestratorApiException;
import it.reply.orchestrator.exception.service.ToscaException;
import it.reply.workflowmanager.exceptions.WorkflowException;
import it.reply.workflowmanager.orchestrator.bpm.BusinessProcessManager;
import it.reply.workflowmanager.orchestrator.bpm.BusinessProcessManager.RUNTIME_STRATEGY;

import org.kie.api.runtime.process.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DeploymentServiceImpl implements DeploymentService {

  @Autowired
  private DeploymentRepository deploymentRepository;

  @Autowired
  private ResourceRepository resourceRepository;

  @Autowired
  private ToscaService toscaService;

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
    Map<String, NodeTemplate> nodes;
    Deployment deployment;

    try {
      // Read the incoming template
      nodes = toscaService.getArchiveRootFromTemplate(request.getTemplate()).getResult()
          .getTopology().getNodeTemplates();
      deployment = new Deployment();
      deployment.setStatus(Status.CREATE_IN_PROGRESS);
      deployment.setTask(Task.NONE);
      deployment.setParameters(request.getParameters().entrySet().stream()
          .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toString())));

      if (request.getCallback() != null) {
        deployment.setCallback(request.getCallback());
      }
      createResources(deployment, nodes);

      deployment = deploymentRepository.save(deployment);
      deployment
          .setTemplate(toscaService.customizeTemplate(request.getTemplate(), deployment.getId()));

    } catch (IOException ex) {
      throw new OrchestratorException(ex.getMessage(), ex);
    } catch (ParsingException ex) {
      throw new BadRequestException("Template is invalid: " + ex.getMessage());
    } catch (ToscaException ex) {
      throw new BadRequestException("Template is invalid: " + ex.getMessage());
    }

    Map<String, Object> params = new HashMap<>();
    params.put("DEPLOYMENT_ID", deployment.getId());
    ProcessInstance pi = null;
    try {
      pi = wfService.startProcess(WorkflowConfigProducerBean.DEPLOY.getProcessId(), params,
          RUNTIME_STRATEGY.PER_PROCESS_INSTANCE);
    } catch (WorkflowException ex) {
      throw new OrchestratorException(ex);
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
        throw new ConflictException(
            String.format("Deployment already in %s state.", deployment.getStatus().toString()));
      } else {
        deployment.setStatus(Status.DELETE_IN_PROGRESS);
        deployment.setStatusReason("");
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
        } catch (WorkflowException ex) {
          throw new OrchestratorException(ex);
        }
        deployment.addWorkflowReferences(
            new WorkflowReference(pi.getId(), RUNTIME_STRATEGY.PER_PROCESS_INSTANCE));
        deployment = deploymentRepository.save(deployment);
      }
    } else {
      throw new NotFoundException("The deployment <" + uuid + "> doesn't exist");
    }
  }

  @Override
  @Transactional
  public void updateDeployment(String id, DeploymentRequest request) {
    Deployment deployment = deploymentRepository.findOne(id);
    if (deployment != null) {
      if (deployment.getStatus() == Status.CREATE_COMPLETE
          || deployment.getStatus() == Status.UPDATE_COMPLETE
          || deployment.getStatus() == Status.UPDATE_FAILED) {
        try {
          // Check if the new template is valid
          ParsingResult<ArchiveRoot> parsingResult =
              toscaService.getArchiveRootFromTemplate(request.getTemplate());

        } catch (ParsingException | IOException ex) {
          throw new OrchestratorException(ex);
        }
        deployment.setStatus(Status.UPDATE_IN_PROGRESS);
        deployment.setTask(Task.NONE);

        Iterator<WorkflowReference> wrIt = deployment.getWorkflowReferences().iterator();

        deployment = deploymentRepository.save(deployment);

        Map<String, Object> params = new HashMap<>();
        params.put("DEPLOYMENT_ID", deployment.getId());
        params.put("TOSCA_TEMPLATE", request.getTemplate());
        ProcessInstance pi = null;
        try {
          pi = wfService.startProcess(WorkflowConfigProducerBean.UPDATE.getProcessId(), params,
              RUNTIME_STRATEGY.PER_PROCESS_INSTANCE);
        } catch (WorkflowException ex) {
          throw new OrchestratorException(ex);
        }
        deployment.addWorkflowReferences(
            new WorkflowReference(pi.getId(), RUNTIME_STRATEGY.PER_PROCESS_INSTANCE));
        deployment = deploymentRepository.save(deployment);
      } else {
        throw new ConflictException(String.format("Cannot update a deployment in %s state",
            deployment.getStatus().toString()));

      }
    } else {
      throw new NotFoundException("The deployment <" + id + "> doesn't exist");
    }
  }

  private void createResources(Deployment deployment, Map<String, NodeTemplate> nodes) {
    Resource resource;
    for (Map.Entry<String, NodeTemplate> entry : nodes.entrySet()) {
      Capability scalable = toscaService.getNodeCapabilityByName(entry.getValue(), "scalable");
      int count = 1;
      if (scalable != null) {
        ScalarPropertyValue scalarPropertyValue =
            (ScalarPropertyValue) scalable.getProperties().get("count");
        if (scalarPropertyValue != null) {
          count = Integer.parseInt(scalarPropertyValue.getValue());
        }
      }
      for (int i = 0; i < count; i++) {
        resource = new Resource();
        resource.setDeployment(deployment);
        resource.setState(NodeStates.CREATING);
        resource.setToscaNodeName(entry.getKey());
        resource.setToscaNodeType(entry.getValue().getType());
        resourceRepository.save(resource);
      }
    }
  }
}
