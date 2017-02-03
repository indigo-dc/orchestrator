package it.reply.orchestrator.service;

import com.google.common.collect.Lists;

import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingException;

import it.reply.orchestrator.config.WorkflowConfigProducerBean;
import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.entity.WorkflowReference;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.deployment.PlacementPolicy;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.request.DeploymentRequest;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.exception.http.BadRequestException;
import it.reply.orchestrator.exception.http.ConflictException;
import it.reply.orchestrator.exception.http.NotFoundException;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.exception.service.ToscaException;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.workflowmanager.exceptions.WorkflowException;
import it.reply.workflowmanager.orchestrator.bpm.BusinessProcessManager;
import it.reply.workflowmanager.orchestrator.bpm.BusinessProcessManager.RUNTIME_STRATEGY;

import org.kie.api.runtime.process.ProcessInstance;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

  @Autowired
  private OAuth2TokenService oauth2TokenService;

  @Autowired
  private OidcProperties oidcProperties;

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
    boolean isChronosDeployment = false;
    Map<String, OneData> odRequirements = new HashMap<>();
    List<PlacementPolicy> placementPolicies = Lists.newArrayList();

    try {
      // Parse once, validate structure and user's inputs, replace user's input
      ArchiveRoot parsingResult =
          toscaService.prepareTemplate(request.getTemplate(), request.getParameters());

      nodes = parsingResult.getTopology().getNodeTemplates();

      deployment = new Deployment();
      deployment.setStatus(Status.CREATE_IN_PROGRESS);
      deployment.setTask(Task.NONE);
      deployment.setTemplate(request.getTemplate());
      deployment.setParameters(request.getParameters());

      if (request.getCallback() != null) {
        deployment.setCallback(request.getCallback());
      }

      // FIXME: Define function to decide DeploymentProvider (Temporary - just for prototyping)
      isChronosDeployment = isChronosDeployment(nodes);
      deployment.setDeploymentProvider(
          (isChronosDeployment ? DeploymentProvider.CHRONOS : DeploymentProvider.IM));

      if (isChronosDeployment) {
        // Extract OneData requirements from template
        odRequirements =
            toscaService.extractOneDataRequirements(parsingResult, request.getParameters());
      }

      placementPolicies = toscaService.extractPlacementPolicies(parsingResult);

      deployment = deploymentRepository.save(deployment);

      // Create internal resources representation (to store in DB)
      createResources(deployment, nodes);

    } catch (IOException ex) {
      throw new OrchestratorException(ex.getMessage(), ex);
    } catch (ParsingException ex) {
      throw new BadRequestException("Template is invalid: " + ex.getMessage());
    } catch (ToscaException ex) {
      throw new BadRequestException("Template is invalid: " + ex.getMessage());
    }

    Map<String, Object> params = new HashMap<>();
    params.put(WorkflowConstants.WF_PARAM_DEPLOYMENT_ID, deployment.getId());
    params.put(WorkflowConstants.WF_PARAM_LOGGER,
        LoggerFactory.getLogger(WorkflowConfigProducerBean.DEPLOY.getProcessId()));

    // FIXME Put in deployment provider field
    params.put(WorkflowConstants.WF_PARAM_DEPLOYMENT_TYPE,
        (isChronosDeployment ? DEPLOYMENT_TYPE_CHRONOS : DEPLOYMENT_TYPE_TOSCA));

    // Build deployment message
    DeploymentMessage deploymentMessage = buildDeploymentMessage(deployment);
    deploymentMessage.setOneDataRequirements(odRequirements);
    deploymentMessage.setPlacementPolicies(placementPolicies);
    params.put(WorkflowConstants.WF_PARAM_DEPLOYMENT_MESSAGE, deploymentMessage);

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

  protected DeploymentMessage buildDeploymentMessage(Deployment deployment) {
    DeploymentMessage deploymentMessage = new DeploymentMessage();
    if (oidcProperties.isEnabled()) {
      deploymentMessage.setOauth2Token(oauth2TokenService.getOAuth2Token());
    }
    deploymentMessage.setDeploymentId(deployment.getId());
    deploymentMessage.setDeploymentProvider(deployment.getDeploymentProvider());
    deploymentMessage.setChosenCloudProviderEndpoint(deployment.getCloudProviderEndpoint());

    return deploymentMessage;

  }

  /**
   * Temporary method to decide whether a given deployment has to be deployed using Chronos (<b>just
   * for experiments</b>). <br/>
   * Currently, if there is at least one node whose name contains 'Chronos', the deployment is done
   * with Chronos.
   * 
   * @param nodes
   *          the template nodes.
   * @return <tt>true</tt> if Chronos, <tt>false</tt> otherwise.
   */
  private static boolean isChronosDeployment(Map<String, NodeTemplate> nodes) {
    for (Map.Entry<String, NodeTemplate> node : nodes.entrySet()) {
      if (node.getValue().getType().contains("Chronos")) {
        return true;
      }
    }
    return false;
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
        // Update deployment status
        deployment.setStatus(Status.DELETE_IN_PROGRESS);
        deployment.setStatusReason("");
        deployment.setTask(Task.NONE);
        deployment = deploymentRepository.save(deployment);

        // Abort all WF currently active on this deployment
        Iterator<WorkflowReference> wrIt = deployment.getWorkflowReferences().iterator();
        while (wrIt.hasNext()) {
          WorkflowReference wr = wrIt.next();
          wfService.abortProcess(wr.getProcessId(), wr.getRuntimeStrategy());
          wrIt.remove();
        }

        Map<String, Object> params = new HashMap<>();
        params.put(WorkflowConstants.WF_PARAM_DEPLOYMENT_ID, deployment.getId());
        params.put(WorkflowConstants.WF_PARAM_LOGGER,
            LoggerFactory.getLogger(WorkflowConfigProducerBean.UNDEPLOY.getProcessId()));

        // FIXME: Temporary - just for test
        if (deployment.getDeploymentProvider() != null) {
          params.put(WorkflowConstants.WF_PARAM_DEPLOYMENT_TYPE,
              deployment.getDeploymentProvider().name());
        } else {
          if (deployment.getEndpoint() != null) {
            throw new DeploymentException(String.format(
                "Error deleting deploy <%s>: Deployment provider is null but the endpoint is <%s>",
                deployment.getId(), deployment.getEndpoint()));
          } else {
            deploymentRepository.delete(deployment);
            return;
          }
        }

        // Build deployment message
        DeploymentMessage deploymentMessage = buildDeploymentMessage(deployment);
        params.put(WorkflowConstants.WF_PARAM_DEPLOYMENT_MESSAGE, deploymentMessage);

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

      if (deployment.getDeploymentProvider() == DeploymentProvider.CHRONOS) {
        // Chronos deployments cannot be updated
        throw new BadRequestException("Chronos deployments cannot be updated.");
      }

      if (deployment.getStatus() == Status.CREATE_COMPLETE
          || deployment.getStatus() == Status.UPDATE_COMPLETE
          || deployment.getStatus() == Status.UPDATE_FAILED) {
        try {
          // Check if the new template is valid: parse, validate structure and user's inputs,
          // replace user's inputs
          toscaService.prepareTemplate(request.getTemplate(), deployment.getParameters());

        } catch (ParsingException | IOException ex) {
          throw new OrchestratorException(ex);
        }
        deployment.setStatus(Status.UPDATE_IN_PROGRESS);
        deployment.setTask(Task.NONE);

        deployment = deploymentRepository.save(deployment);

        // !! WARNING !! That's an hack to avoid an obscure NonUniqueObjetException on the new
        // WorkflowReference created after the WF start
        deployment.getWorkflowReferences().size();

        Map<String, Object> params = new HashMap<>();
        params.put(WorkflowConstants.WF_PARAM_DEPLOYMENT_ID, deployment.getId());
        params.put(WorkflowConstants.WF_PARAM_TOSCA_TEMPLATE, request.getTemplate());
        params.put(WorkflowConstants.WF_PARAM_LOGGER,
            LoggerFactory.getLogger(WorkflowConfigProducerBean.UPDATE.getProcessId()));

        // Build deployment message
        DeploymentMessage deploymentMessage = buildDeploymentMessage(deployment);
        params.put(WorkflowConstants.WF_PARAM_DEPLOYMENT_MESSAGE, deploymentMessage);

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
