/*
 * Copyright © 2015-2018 Santer Reply S.p.A.
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

package it.reply.orchestrator.service;

import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.tosca.model.ArchiveRoot;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.OidcEntity;
import it.reply.orchestrator.dal.entity.OidcEntityId;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.entity.WorkflowReference;
import it.reply.orchestrator.dal.entity.WorkflowReference.Action;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.deployment.PlacementPolicy;
import it.reply.orchestrator.dto.dynafed.Dynafed;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.request.DeploymentRequest;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.DeploymentType;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.exception.http.BadRequestException;
import it.reply.orchestrator.exception.http.ConflictException;
import it.reply.orchestrator.exception.http.ForbiddenException;
import it.reply.orchestrator.exception.http.NotFoundException;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.orchestrator.utils.CommonUtils;
import it.reply.orchestrator.utils.MdcUtils;
import it.reply.orchestrator.utils.ToscaConstants;
import it.reply.orchestrator.utils.WorkflowConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.extern.slf4j.Slf4j;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class DeploymentServiceImpl implements DeploymentService {

  private static final Pattern OWNER_PATTERN = Pattern.compile("([^@]+)@([^@]+)");

  @Autowired
  private DeploymentRepository deploymentRepository;

  @Autowired
  private ResourceRepository resourceRepository;

  @Autowired
  private ToscaService toscaService;

  @Autowired
  private RuntimeService wfService;

  @Autowired
  private OAuth2TokenService oauth2TokenService;

  @Autowired
  private OidcProperties oidcProperties;

  @Autowired
  private ObjectMapper objectMapper;

  @Override
  @Transactional(readOnly = true)
  public Page<Deployment> getDeployments(Pageable pageable, String owner) {
    if (owner == null) {
      if (oidcProperties.isEnabled()) {
        OidcEntity requester = oauth2TokenService.generateOidcEntityFromCurrentAuth();
        return deploymentRepository.findAll(requester, pageable);
      } else {
        return deploymentRepository.findAll(pageable);
      }
    } else {
      OidcEntityId ownerId;
      if ("me".equals(owner)) {
        ownerId = oauth2TokenService.generateOidcEntityIdFromCurrentAuth();
      } else {
        Matcher matcher = OWNER_PATTERN.matcher(owner);
        if (matcher.matches()) {
          ownerId = new OidcEntityId();
          ownerId.setSubject(matcher.group(1));
          ownerId.setIssuer(matcher.group(2));
        } else {
          throw new BadRequestException("Value " + owner + " for param createdBy is illegal");
        }
      }
      if (oidcProperties.isEnabled()) {
        OidcEntity requester = oauth2TokenService.generateOidcEntityFromCurrentAuth();
        return deploymentRepository.findAllByOwner(requester, ownerId, pageable);
      } else {
        return deploymentRepository.findAllByOwner(ownerId, pageable);
      }
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Deployment getDeployment(String uuid) {

    Deployment deployment = null;
    if (oidcProperties.isEnabled()) {
      OidcEntity requester = oauth2TokenService.generateOidcEntityFromCurrentAuth();
      deployment = deploymentRepository.findOne(requester, uuid);
    } else {
      deployment = deploymentRepository.findOne(uuid);
    }
    if (deployment != null) {
      MdcUtils.setDeploymentId(deployment.getId());
      return deployment;
    } else {
      throw new NotFoundException("The deployment <" + uuid + "> doesn't exist");
    }
  }

  private void throwIfNotOwned(Deployment deployment) {
    if (oidcProperties.isEnabled()) {
      OidcEntityId requesterId = oauth2TokenService.generateOidcEntityIdFromCurrentAuth();
      OidcEntity owner = deployment.getOwner();
      if (owner != null && !requesterId.equals(owner.getOidcEntityId())) {
        throw new ForbiddenException(
            "Only the owner of the deployment can perform this operation");
      }
    }
  }

  @Override
  @Transactional
  public Deployment createDeployment(DeploymentRequest request) {
    Deployment deployment = new Deployment();
    deployment.setStatus(Status.CREATE_IN_PROGRESS);
    deployment.setTask(Task.NONE);
    deployment.setTemplate(request.getTemplate());
    deployment.setParameters(request.getParameters());
    deployment.setCallback(request.getCallback());
    deployment = deploymentRepository.save(deployment);
    MdcUtils.setDeploymentId(deployment.getId());
    LOG.debug("Creating deployment with template\n{}", request.getTemplate());
    // Parse once, validate structure and user's inputs, replace user's input
    ArchiveRoot parsingResult =
        toscaService.prepareTemplate(request.getTemplate(), request.getParameters());
    Map<String, NodeTemplate> nodes = parsingResult.getTopology().getNodeTemplates();

    // Create internal resources representation (to store in DB)
    createResources(deployment, nodes);

    if (oidcProperties.isEnabled()) {
      deployment.setOwner(oauth2TokenService.getOrGenerateOidcEntityFromCurrentAuth());
    }

    DeploymentType deploymentType = inferDeploymentType(nodes);

    // Build deployment message
    DeploymentMessage deploymentMessage = buildDeploymentMessage(deployment, deploymentType);

    Map<String, OneData> odRequirements = toscaService
        .extractOneDataRequirements(parsingResult, request.getParameters());
    deploymentMessage.setOneDataRequirements(odRequirements);

    Map<String, Dynafed> dyanfedRequirements = toscaService
        .extractDyanfedRequirements(parsingResult, request.getParameters());
    deploymentMessage.setDynafedRequirements(dyanfedRequirements);

    deploymentMessage.setTimeoutInMins(request.getTimeoutMins());

    deploymentMessage.setMaxProvidersRetry(request.getMaxProvidersRetry());
    deploymentMessage.setKeepLastAttempt(request.isKeepLastAttempt());

    Map<String, PlacementPolicy> placementPolicies = toscaService
        .extractPlacementPolicies(parsingResult);
    deploymentMessage.setPlacementPolicies(placementPolicies);
    deploymentMessage.setDeploymentType(deploymentType);

    boolean isHybrid = toscaService.isHybridDeployment(parsingResult);
    deploymentMessage.setHybrid(isHybrid);

    ProcessInstance pi = wfService
        .createProcessInstanceBuilder()
        .variable(WorkflowConstants.Param.DEPLOYMENT_ID, deployment.getId())
        .variable(WorkflowConstants.Param.REQUEST_ID, MdcUtils.getRequestId())
        .variable(WorkflowConstants.Param.DEPLOYMENT_MESSAGE,
            objectMapper.valueToTree(deploymentMessage))
        .processDefinitionKey(WorkflowConstants.Process.DEPLOY)
        .businessKey(MdcUtils.toBusinessKey())
        .start();

    deployment.addWorkflowReferences(
        new WorkflowReference(pi.getId(), MdcUtils.getRequestId(), Action.CREATE));
    deployment = deploymentRepository.save(deployment);
    return deployment;

  }

  protected DeploymentMessage buildDeploymentMessage(Deployment deployment,
      DeploymentType deploymentType) {
    DeploymentMessage deploymentMessage = new DeploymentMessage();
    if (oidcProperties.isEnabled()) {
      deploymentMessage.setRequestedWithToken(oauth2TokenService.exchangeCurrentAccessToken());
    }
    deploymentMessage.setDeploymentId(deployment.getId());
    deploymentMessage.setDeploymentType(deploymentType);
    return deploymentMessage;
  }

  private DeploymentType inferDeploymentType(Map<String, NodeTemplate> nodes) {
    for (NodeTemplate node : nodes.values()) {
      if (toscaService.isOfToscaType(node, ToscaConstants.Nodes.CHRONOS)) {
        return DeploymentType.CHRONOS;
      } else if (toscaService.isOfToscaType(node, ToscaConstants.Nodes.MARATHON)) {
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
    Deployment deployment = getDeployment(uuid);
    MdcUtils.setDeploymentId(deployment.getId());
    throwIfNotOwned(deployment);

    if (deployment.getStatus() == Status.DELETE_COMPLETE
        || deployment.getStatus() == Status.DELETE_IN_PROGRESS) {
      throw new ConflictException(
          String.format("Deployment already in %s state.", deployment.getStatus().toString()));
    }

    // Update deployment status
    deployment.setStatus(Status.DELETE_IN_PROGRESS);
    deployment.setStatusReason(null);
    deployment.setTask(Task.NONE);

    wfService
        .createExecutionQuery()
        .onlyProcessInstanceExecutions()
        .variableValueEquals(WorkflowConstants.Param.DEPLOYMENT_ID, deployment.getId())
        .list()
        .forEach(execution -> wfService.deleteProcessInstance(execution.getProcessInstanceId(),
            "Process deleted by user with request " + MdcUtils.getRequestId()));

    if (deployment.getDeploymentProvider() == null) {
      // no deployment provider -> no resources created
      // TODO handle it in a better way (e.g. a stub provider)
      deploymentRepository.delete(deployment);
      return;
    }
    DeploymentType deploymentType = inferDeploymentType(deployment.getDeploymentProvider());

    // Build deployment message
    DeploymentMessage deploymentMessage = buildDeploymentMessage(deployment, deploymentType);

    ProcessInstance pi = wfService
        .createProcessInstanceBuilder()
        .variable(WorkflowConstants.Param.DEPLOYMENT_ID, deployment.getId())
        .variable(WorkflowConstants.Param.REQUEST_ID, MdcUtils.getRequestId())
        .variable(WorkflowConstants.Param.DEPLOYMENT_MESSAGE,
            objectMapper.valueToTree(deploymentMessage))
        .processDefinitionKey(WorkflowConstants.Process.UNDEPLOY)
        .businessKey(MdcUtils.toBusinessKey())
        .start();

    deployment.addWorkflowReferences(
        new WorkflowReference(pi.getId(), MdcUtils.getRequestId(), Action.DELETE));
  }

  @Override
  @Transactional
  public void updateDeployment(String id, DeploymentRequest request) {
    Deployment deployment = getDeployment(id);
    MdcUtils.setDeploymentId(deployment.getId());
    LOG.debug("Updating deployment with template\n{}", id, request.getTemplate());
    throwIfNotOwned(deployment);

    if (deployment.getDeploymentProvider() == DeploymentProvider.CHRONOS
        || deployment.getDeploymentProvider() == DeploymentProvider.MARATHON) {
      throw new BadRequestException(String.format("%s deployments cannot be updated.",
          deployment.getDeploymentProvider().toString()));
    }

    if (!(deployment.getStatus() == Status.CREATE_COMPLETE
        || deployment.getStatus() == Status.UPDATE_COMPLETE
        || deployment.getStatus() == Status.UPDATE_FAILED)) {
      throw new ConflictException(String.format("Cannot update a deployment in %s state",
          deployment.getStatus().toString()));
    }

    deployment.setStatus(Status.UPDATE_IN_PROGRESS);
    deployment.setStatusReason(null);
    deployment.setTask(Task.NONE);
    deployment.getParameters().putAll(request.getParameters());
    if (request.getCallback() != null) {
      deployment.setCallback(request.getCallback());
    }
    deployment = deploymentRepository.save(deployment);

    DeploymentType deploymentType = inferDeploymentType(deployment.getDeploymentProvider());

    // Build deployment message
    DeploymentMessage deploymentMessage = buildDeploymentMessage(deployment, deploymentType);

    // Check if the new template is valid: parse, validate structure and user's inputs,
    // replace user's inputs
    ArchiveRoot parsingResult =
        toscaService.prepareTemplate(request.getTemplate(), deployment.getParameters());

    boolean isHybrid = toscaService.isHybridDeployment(parsingResult);
    deploymentMessage.setHybrid(isHybrid);

    Map<String, PlacementPolicy> placementPolicies =
        toscaService.extractPlacementPolicies(parsingResult);
    deploymentMessage.setPlacementPolicies(placementPolicies);

    Map<String, OneData> odRequirements = toscaService
        .extractOneDataRequirements(parsingResult, request.getParameters());
    deploymentMessage.setOneDataRequirements(odRequirements);

    Map<String, Dynafed> dyanfedRequirements = toscaService
        .extractDyanfedRequirements(parsingResult, request.getParameters());
    deploymentMessage.setDynafedRequirements(dyanfedRequirements);

    deploymentMessage.setTimeoutInMins(request.getTimeoutMins());

    deploymentMessage.setMaxProvidersRetry(request.getMaxProvidersRetry());
    deploymentMessage.setKeepLastAttempt(request.isKeepLastAttempt());

    ProcessInstance pi = wfService
        .createProcessInstanceBuilder()
        .variable(WorkflowConstants.Param.DEPLOYMENT_ID, deployment.getId())
        .variable(WorkflowConstants.Param.REQUEST_ID, MdcUtils.getRequestId())
        .variable(WorkflowConstants.Param.TOSCA_TEMPLATE, request.getTemplate())
        .variable(WorkflowConstants.Param.DEPLOYMENT_MESSAGE,
            objectMapper.valueToTree(deploymentMessage))
        .processDefinitionKey(WorkflowConstants.Process.UPDATE)
        .businessKey(MdcUtils.toBusinessKey())
        .start();

    deployment.addWorkflowReferences(
        new WorkflowReference(pi.getId(), MdcUtils.getRequestId(), Action.UPDATE));

  }

  private void createResources(Deployment deployment, Map<String, NodeTemplate> nodes) {

    // calculate graph
    DirectedMultigraph<NodeTemplate, RelationshipTemplate> graph =
        toscaService.buildNodeGraph(nodes, true);

    // calculate topology
    TopologicalOrderIterator<NodeTemplate, RelationshipTemplate> nodeIterator =
        new TopologicalOrderIterator<>(graph);

    // Map with all the resources created for each node
    Map<NodeTemplate, Set<Resource>> resourcesMap = new HashMap<>();

    CommonUtils.iteratorToStream(nodeIterator).forEachOrdered(node -> {
      Set<RelationshipTemplate> relationships = graph.incomingEdgesOf(node);

      // Get all the parents
      List<NodeTemplate> parentNodes =
          relationships.stream().map(graph::getEdgeSource).collect(Collectors.toList());

      int nodeCount = toscaService.getCount(node).orElse(1);
      Set<Resource> resources = IntStream
          .range(0, nodeCount)
          .mapToObj(i -> {

            Resource tmpResource = new Resource();
            tmpResource.setDeployment(deployment);
            tmpResource.setState(NodeStates.INITIAL);
            tmpResource.setToscaNodeName(node.getName());
            tmpResource.setToscaNodeType(node.getType());

            Resource resource = resourceRepository.save(tmpResource);

            // bind parents resources with child resource
            parentNodes.forEach(
                parentNode -> resourcesMap.get(parentNode).forEach(resource::addRequiredResource));
            return resource;
          })
          .collect(Collectors.toSet());
      // add all the resources created for this node
      resourcesMap.put(node, resources);
    });
  }
}
