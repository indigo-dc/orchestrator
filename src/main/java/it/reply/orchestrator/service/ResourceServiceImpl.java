/*
 * Copyright © 2015-2021 I.N.F.N.
 * Copyright © 2015-2020 Santer Reply S.p.A.
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

import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.context.ToscaContextual;
import alien4cloud.tosca.model.ArchiveRoot;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.entity.WorkflowReference;
import it.reply.orchestrator.dal.entity.WorkflowReference.Action;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.deployment.ActionMessage;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.DeploymentType;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.exception.http.BadRequestException;
import it.reply.orchestrator.exception.http.ConflictException;
import it.reply.orchestrator.exception.http.NotFoundException;
import it.reply.orchestrator.utils.MdcUtils;
import it.reply.orchestrator.utils.WorkflowConstants;

import java.util.List;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.utils.ToscaTypeUtils;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@AllArgsConstructor
public class ResourceServiceImpl implements ResourceService {

  private ResourceRepository resourceRepository;

  private DeploymentService deploymentservice;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private RuntimeService wfService;

  @Autowired
  private ToscaService toscaService;

  @Override
  @Transactional(readOnly = true)
  public Page<Resource> getResources(String deploymentId, Pageable pageable) {
    // check if deploymentExists
    Deployment deployment = deploymentservice.getDeployment(deploymentId);
    MdcUtils.setDeploymentId(deployment.getId());
    return resourceRepository.findByDeployment_id(deploymentId, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  @ToscaContextual
  public List<Resource> getResources(String deploymentId, String type) {
    // check if deployment exists
    Deployment deployment = deploymentservice.getDeployment(deploymentId);
    MdcUtils.setDeploymentId(deployment.getId());
    List<Resource> resources = resourceRepository.findByDeployment_id(deploymentId);
    // parse the tosca template and get context
    ArchiveRoot ar = toscaService.parse(deployment.getTemplate());
    // get the list of template nodes that match or derive from the specified tosca type
    List<String> nodeNames = ar.getTopology().getNodeTemplates().entrySet().stream()
                               .filter(n -> ToscaTypeUtils.isOfType(ToscaContext.get(
                                      NodeType.class, n.getValue().getType()),type))
                               .map(n -> n.getKey()).collect(Collectors.toList());
    // filter the deployment resources that match with the names computed above
    return resources.stream().filter(r -> nodeNames.contains(r.getToscaNodeName()))
                    .collect(Collectors.toList());

  }

  @Override
  @Transactional(readOnly = true)
  public Resource getResource(String uuid, String deploymentId) {
    // check if deploymentExists
    Deployment deployment = deploymentservice.getDeployment(deploymentId);
    MdcUtils.setDeploymentId(deployment.getId());
    return resourceRepository.findByIdAndDeployment_id(uuid, deploymentId)
        .orElseThrow(() -> new NotFoundException(String
            .format("The resource <%s> in deployment <%s> doesn't exist", uuid, deploymentId)));
  }

  @Override
  @Transactional
  public boolean doAction(String deploymentId, String resourceId,
      String action, OidcTokenId requestedWithToken) {
    Deployment deployment = deploymentservice.getDeployment(deploymentId);
    deploymentservice.throwIfNotOwned(deployment);

    resourceRepository.findByIdAndDeployment_id(resourceId, deploymentId)
                        .orElseThrow(() -> new BadRequestException(String.format(
                          "Resource <%s> not found in deployment <%s>", resourceId, deploymentId)));

    MdcUtils.setDeploymentId(deployment.getId());
    LOG.debug("Performing action {} on deployment with id\n{}", action, deploymentId);

    if (deployment.getDeploymentProvider() == DeploymentProvider.CHRONOS
        || deployment.getDeploymentProvider() == DeploymentProvider.MARATHON
        || deployment.getDeploymentProvider() == DeploymentProvider.QCG) {
      throw new BadRequestException(String.format("%s deployment: actions not supported yet.",
          deployment.getDeploymentProvider().toString()));
    }

    if (!(deployment.getStatus() == Status.CREATE_COMPLETE
        || deployment.getStatus() == Status.UPDATE_COMPLETE
        || deployment.getStatus() == Status.UPDATE_FAILED)) {
      throw new ConflictException(String.format("Cannot perform actions on deployment in %s state",
          deployment.getStatus().toString()));
    }

    DeploymentType deploymentType = DeploymentService.inferDeploymentType(
        deployment.getDeploymentProvider());

    // Build Action message
    ActionMessage actionMessage = buildActionMessage(deployment, deploymentType, resourceId, action,
        requestedWithToken);

    String requestId = MdcUtils.getRequestId();

    ProcessInstance pi = wfService
        .createProcessInstanceBuilder()
        .variable(WorkflowConstants.Param.DEPLOYMENT_ID, deployment.getId())
        .variable(WorkflowConstants.Param.REQUEST_ID, MdcUtils.getRequestId())
        .variable(WorkflowConstants.Param.DEPLOYMENT_MESSAGE,
            objectMapper.valueToTree(actionMessage))
        .processDefinitionKey(WorkflowConstants.Process.OPERATE_RESOURCES)
        .businessKey(MdcUtils.toBusinessKey())
        .start();

    deployment.addWorkflowReferences(
        new WorkflowReference(pi.getId(), requestId, Action.EXECUTE));

    return true;

  }

  private ActionMessage buildActionMessage(Deployment deployment, DeploymentType deploymentType,
      String resourceId, String action, OidcTokenId requestedWithToken) {
    ActionMessage actionMessage = new ActionMessage(resourceId);
    actionMessage.setAction(action);
    actionMessage.setRequestedWithToken(requestedWithToken);
    actionMessage.setDeploymentId(deployment.getId());
    actionMessage.setDeploymentType(deploymentType);
    return actionMessage;
  }

}
