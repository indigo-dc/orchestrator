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

package it.reply.orchestrator.service;

import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingException;

import it.reply.orchestrator.config.WorkflowConfigProducerBean;
import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.OidcEntity;
import it.reply.orchestrator.dal.entity.OidcEntityId;
import it.reply.orchestrator.dal.entity.OidcRefreshToken;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.entity.WorkflowReference;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dal.repository.OidcEntityRepository;
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
import it.reply.orchestrator.utils.CommonUtils;
import it.reply.orchestrator.utils.ToscaConstants;
import it.reply.orchestrator.utils.WorkflowConstants;
import it.reply.workflowmanager.exceptions.WorkflowException;
import it.reply.workflowmanager.orchestrator.bpm.BusinessProcessManager;
import it.reply.workflowmanager.orchestrator.bpm.BusinessProcessManager.RUNTIME_STRATEGY;

import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.kie.api.runtime.process.ProcessInstance;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.social.oauth2.AccessGrant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class DeploymentServiceImpl implements DeploymentService {

  private static final Pattern OWNER_PATTERN = Pattern.compile("([^@]+)@([^@]+)");

  @Autowired
  private DeploymentRepository deploymentRepository;

  @Autowired
  private ResourceRepository resourceRepository;

  @Autowired
  private OidcEntityRepository oidcEntityRepository;

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
  public Page<Deployment> getDeployments(Pageable pageable, String owner) {
    if (owner == null) {
      return deploymentRepository.findAll(pageable);
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
      return deploymentRepository.findByOwner_oidcEntityId(ownerId, pageable);
    }
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

  private Optional<OidcEntity> getOrGenerateRequester() {
    if (oidcProperties.isEnabled()) {
      OidcEntityId requesterId = oauth2TokenService.generateOidcEntityIdFromCurrentAuth();

      OidcEntity requester = oidcEntityRepository
          .findByOidcEntityId(requesterId)
          .orElseGet(oauth2TokenService::generateOidcEntityFromCurrentAuth);
      // exchange token if a refresh token is not yet associated with the user
      if (requester.getRefreshToken() == null) {
        OidcTokenId currentTokenId = oauth2TokenService.generateTokenIdFromCurrentAuth();
        AccessGrant grant = oauth2TokenService.exchangeAccessToken(currentTokenId,
            oauth2TokenService.getOAuth2TokenFromCurrentAuth(), OAuth2TokenService.REQUIRED_SCOPES);

        OidcRefreshToken token = OidcRefreshToken.fromAccessGrant(currentTokenId, grant);

        requester.setRefreshToken(token);

      }
      return Optional.of(requester);
    } else {
      return Optional.empty();
    }
  }

  @Override
  @Transactional
  public Deployment createDeployment(DeploymentRequest request) {
    Map<String, NodeTemplate> nodes;
    Deployment deployment;
    Map<String, OneData> odRequirements = new HashMap<>();
    List<PlacementPolicy> placementPolicies = new ArrayList<>();
    DeploymentType deploymentType;
    boolean isHybrid;

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

      isHybrid = toscaService.isHybridDeployment(parsingResult);

      deployment = deploymentRepository.save(deployment);

      // Create internal resources representation (to store in DB)
      createResources(deployment, nodes);

    } catch (IOException ex) {
      throw new OrchestratorException(ex.getMessage(), ex);
    } catch (ParsingException | ToscaException ex) {
      throw new BadRequestException("Template is invalid: " + ex.getMessage(), ex);
    }

    Map<String, Object> params = new HashMap<>();
    params.put(WorkflowConstants.WF_PARAM_DEPLOYMENT_ID, deployment.getId());
    params.put(WorkflowConstants.WF_PARAM_LOGGER,
        LoggerFactory.getLogger(WorkflowConfigProducerBean.DEPLOY.getProcessId()));

    Optional<OidcEntity> requester = this.getOrGenerateRequester();
    requester.ifPresent(deployment::setOwner);

    // Build deployment message
    DeploymentMessage deploymentMessage = buildDeploymentMessage(deployment, requester);
    deploymentMessage
        .setOneDataRequirements(CommonUtils.notNullOrDefaultValue(odRequirements, new HashMap<>()));
    deploymentMessage.setPlacementPolicies(
        CommonUtils.notNullOrDefaultValue(placementPolicies, new ArrayList<>()));
    deploymentMessage.setDeploymentType(deploymentType);
    deploymentMessage.setHybrid(isHybrid);
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

  protected DeploymentMessage buildDeploymentMessage(Deployment deployment,
      Optional<OidcEntity> requester) {
    DeploymentMessage deploymentMessage = new DeploymentMessage();
    requester.ifPresent(req -> {
      OidcTokenId tokenId = new OidcTokenId();
      tokenId.setIssuer(req.getOidcEntityId().getIssuer());
      tokenId.setJti(req.getRefreshToken().getOriginalTokenId());
      deploymentMessage.setRequestedWithToken(tokenId);
    });
    deploymentMessage.setDeploymentId(deployment.getId());
    deploymentMessage.setChosenCloudProviderEndpoint(deployment.getCloudProviderEndpoint());

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
    Deployment deployment = deploymentRepository.findOne(uuid);
    if (deployment != null) {
      if (deployment.getStatus() == Status.DELETE_COMPLETE
          || deployment.getStatus() == Status.DELETE_IN_PROGRESS) {
        throw new ConflictException(
            String.format("Deployment already in %s state.", deployment.getStatus().toString()));
      } else {
        // Update deployment status
        deployment.setStatus(Status.DELETE_IN_PROGRESS);
        deployment.setStatusReason(null);
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

        Optional<OidcEntity> requester = this.getOrGenerateRequester();

        // Build deployment message
        DeploymentMessage deploymentMessage = buildDeploymentMessage(deployment, requester);
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
    boolean isHybrid;
    List<PlacementPolicy> placementPolicies = new ArrayList<>();
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
          ArchiveRoot parsingResult =
              toscaService.prepareTemplate(request.getTemplate(), deployment.getParameters());
          isHybrid = toscaService.isHybridDeployment(parsingResult);
          placementPolicies = toscaService.extractPlacementPolicies(parsingResult);
        } catch (ParsingException | IOException ex) {
          throw new OrchestratorException(ex);
        }
        deployment.setStatus(Status.UPDATE_IN_PROGRESS);
        deployment.setStatusReason(null);
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

        Optional<OidcEntity> requester = this.getOrGenerateRequester();

        // Build deployment message
        DeploymentMessage deploymentMessage = buildDeploymentMessage(deployment, requester);
        params.put(WorkflowConstants.WF_PARAM_DEPLOYMENT_MESSAGE, deploymentMessage);

        DeploymentType deploymentType = inferDeploymentType(deployment.getDeploymentProvider());
        deploymentMessage.setDeploymentType(deploymentType);

        deploymentMessage.setHybrid(isHybrid);
        deploymentMessage.setPlacementPolicies(placementPolicies);

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
