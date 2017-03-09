package it.reply.orchestrator.service;

/*
 * Copyright © 2015-2017 Santer Reply S.p.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
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
import it.reply.orchestrator.enums.DeploymentType;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.exception.http.BadRequestException;
import it.reply.orchestrator.exception.http.ConflictException;
import it.reply.orchestrator.exception.http.NotFoundException;
import it.reply.orchestrator.exception.service.ToscaException;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.workflowmanager.exceptions.WorkflowException;
import it.reply.workflowmanager.orchestrator.bpm.BusinessProcessManager;
import it.reply.workflowmanager.orchestrator.bpm.BusinessProcessManager.RUNTIME_STRATEGY;

import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.kie.api.runtime.process.ProcessInstance;
import org.slf4j.Logger;
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DeploymentServiceImpl implements DeploymentService {

  private static final Logger LOG = LoggerFactory.getLogger(DeploymentServiceImpl.class);

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
  @Transactional(readOnly = true)
  public Page<Deployment> getDeployments(Pageable pageable) {
    return deploymentRepository.findAll(pageable);
  }

  @Override
  @Transactional(readOnly = true)
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
    Map<String, OneData> odRequirements = Maps.newHashMap();
    List<PlacementPolicy> placementPolicies = Lists.newArrayList();
    DeploymentType deploymentType;

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
      deploymentType = inferDeploymentType(nodes);

      if (deploymentType == DeploymentType.CHRONOS) {
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

    // Build deployment message
    DeploymentMessage deploymentMessage = buildDeploymentMessage(deployment);
    deploymentMessage.setOneDataRequirements(odRequirements);
    deploymentMessage.setPlacementPolicies(placementPolicies);
    deploymentMessage.setDeploymentType(deploymentType);
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
    deploymentMessage.setChosenCloudProviderEndpoint(deployment.getCloudProviderEndpoint());

    return deploymentMessage;

  }

  private static DeploymentType inferDeploymentType(Map<String, NodeTemplate> nodes) {
    for (Map.Entry<String, NodeTemplate> node : nodes.entrySet()) {
      if (node.getValue().getType().contains("Chronos")) {
        return DeploymentType.CHRONOS;
      } else if (node.getValue().getType().contains("Marathon")) {
        return DeploymentType.MARATHON;
      }
    }
    return DeploymentType.TOSCA;
  }

  private static DeploymentType inferDeploymentType(DeploymentProvider deploymentProvider) {
    switch (deploymentProvider) {
      case CHRONOS:
        return DeploymentType.CHRONOS;
      case MARATHON:
        return DeploymentType.MARATHON;
      case HEAT:
      case IM:
      default:
        return DeploymentType.TOSCA;
    }
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

        // No deployment IaaS reference -> nothing to delete
        if (deployment.getEndpoint() == null) {
          deploymentRepository.delete(deployment);
          return;
        }

        // Build deployment message
        DeploymentMessage deploymentMessage = buildDeploymentMessage(deployment);
        DeploymentType deploymentType = inferDeploymentType(deployment.getDeploymentProvider());
        deploymentMessage.setDeploymentType(deploymentType);
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

      if (deployment.getDeploymentProvider() == DeploymentProvider.CHRONOS
          || deployment.getDeploymentProvider() == DeploymentProvider.MARATHON) {
        throw new BadRequestException(String.format("%s deployments cannot be updated.",
            deployment.getDeploymentProvider().toString()));
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

        DeploymentType deploymentType = inferDeploymentType(deployment.getDeploymentProvider());
        deploymentMessage.setDeploymentType(deploymentType);
        
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

    // calculate graph
    DirectedMultigraph<NodeTemplate, RelationshipTemplate> graph =
        toscaService.buildNodeGraph(nodes, true);

    // calculate topology
    TopologicalOrderIterator<NodeTemplate, RelationshipTemplate> nodeIterator =
        new TopologicalOrderIterator<>(graph);

    // Map with all the resources created for each node
    Map<NodeTemplate, Set<Resource>> resourcesMap = Maps.newHashMap();

    while (nodeIterator.hasNext()) {
      NodeTemplate node = nodeIterator.next();
      Set<RelationshipTemplate> relationships = graph.incomingEdgesOf(node);

      // Get all the parents
      List<NodeTemplate> parentNodes =
          relationships.stream().map(graph::getEdgeSource).collect(Collectors.toList());

      int nodeCount = toscaService.getCount(node).orElse(1);
      Set<Resource> resources = Sets.newHashSet();
      for (int i = 0; i < nodeCount; ++i) {

        Resource tmpResource = new Resource();
        tmpResource.setDeployment(deployment);
        tmpResource.setState(NodeStates.INITIAL);
        tmpResource.setToscaNodeName(node.getName());
        tmpResource.setToscaNodeType(node.getType());

        final Resource resource = resourceRepository.save(tmpResource);
        resources.add(resource);

        // bind parents resources with child resource
        parentNodes.forEach(parentNode -> resourcesMap.get(parentNode).forEach(parentResource -> {
          parentResource.getRequiredBy().add(resource.getId());
          resource.getRequires().add(parentResource.getId());
        }));
      }
      // add all the resources created for this node
      resourcesMap.put(node, resources);
    }
  }
}
