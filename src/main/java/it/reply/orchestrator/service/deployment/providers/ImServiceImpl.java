/*
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

package it.reply.orchestrator.service.deployment.providers;

import alien4cloud.tosca.model.ArchiveRoot;

import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.MoreCollectors;
import com.google.common.collect.Multimap;

import es.upv.i3m.grycap.im.InfrastructureManager;
import es.upv.i3m.grycap.im.exceptions.ImClientErrorException;
import es.upv.i3m.grycap.im.exceptions.ImClientException;
import es.upv.i3m.grycap.im.exceptions.ImClientServerErrorException;
import es.upv.i3m.grycap.im.pojo.InfrastructureState;
import es.upv.i3m.grycap.im.pojo.Property;
import es.upv.i3m.grycap.im.pojo.ResponseError;
import es.upv.i3m.grycap.im.pojo.VirtualMachineInfo;
import es.upv.i3m.grycap.im.rest.client.BodyContentType;

import it.reply.orchestrator.annotation.DeploymentProviderQualifier;
import it.reply.orchestrator.config.properties.ImProperties;
import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.cmdb.ComputeService;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.workflow.CloudServicesOrderedIterator;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.exception.service.BusinessWorkflowException;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.function.ThrowingConsumer;
import it.reply.orchestrator.function.ThrowingFunction;
import it.reply.orchestrator.service.IndigoInputsPreProcessorService;
import it.reply.orchestrator.service.ToscaService;
import it.reply.orchestrator.service.IndigoInputsPreProcessorService.RuntimeProperties;
import it.reply.orchestrator.service.deployment.providers.factory.ImClientFactory;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.orchestrator.utils.CommonUtils;
import it.reply.orchestrator.utils.WorkflowConstants.ErrorCode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@DeploymentProviderQualifier(DeploymentProvider.IM)
@EnableConfigurationProperties(ImProperties.class)
@Slf4j
public class ImServiceImpl extends AbstractDeploymentProviderService {

  @Autowired
  private ToscaService toscaService;

  @Autowired
  private ResourceRepository resourceRepository;

  @Autowired
  private IndigoInputsPreProcessorService indigoInputsPreProcessorService;

  @Autowired
  private OidcProperties oidcProperties;

  @Autowired
  private OAuth2TokenService oauth2TokenService;

  @Autowired
  private ImClientFactory imClientFactory;

  protected <R> R executeWithClientForResult(List<CloudProviderEndpoint> cloudProviderEndpoints,
      @Nullable OidcTokenId requestedWithToken,
      ThrowingFunction<InfrastructureManager, R, ImClientException> function)
      throws ImClientException {
    return oauth2TokenService.executeWithClientForResult(requestedWithToken,
        accessToken -> function.apply(imClientFactory.build(cloudProviderEndpoints, accessToken)),
        ex -> ex instanceof ImClientErrorException && isUnauthorized((ImClientErrorException) ex));
  }

  protected void executeWithClient(List<CloudProviderEndpoint> cloudProviderEndpoints,
      @Nullable OidcTokenId requestedWithToken,
      ThrowingConsumer<InfrastructureManager, ImClientException> consumer)
      throws ImClientException {
    executeWithClientForResult(cloudProviderEndpoints, requestedWithToken, consumer.asFunction());
  }

  private static boolean isUnauthorized(ImClientErrorException error) {
    return Optional
        .ofNullable(error.getResponseError())
        .map(ResponseError::getCode)
        .filter(code -> code.equals(HttpStatus.UNAUTHORIZED.value()))
        .isPresent();
  }

  protected ArchiveRoot prepareTemplate(Deployment deployment,
      DeploymentMessage deploymentMessage) {
    Map<String, OneData> odParameters = deploymentMessage.getOneDataParameters();
    RuntimeProperties runtimeProperties = new RuntimeProperties();
    odParameters.forEach((nodeName, odParameter) -> {
      runtimeProperties.put(odParameter.getOnezone(), nodeName, "onezone");
      runtimeProperties.put(odParameter.getToken(), nodeName, "token");
      runtimeProperties
        .put(odParameter.getSelectedOneprovider().getEndpoint(), nodeName, "selected_provider");
      if (odParameter.isServiceSpace()) {
        runtimeProperties.put(odParameter.getSpace(), nodeName, "space");
        runtimeProperties.put(odParameter.getPath(), nodeName, "path");
      }
    });
    Map<String, Object> inputs = deployment.getParameters();
    ArchiveRoot ar = toscaService.parseAndValidateTemplate(deployment.getTemplate(), inputs);
    indigoInputsPreProcessorService.processInputAttributes(ar, inputs, runtimeProperties);
    return ar;
  }

  @Override
  public boolean doDeploy(DeploymentMessage deploymentMessage) {
    Deployment deployment = getDeployment(deploymentMessage);

    resourceRepository
        .findByDeployment_id(deployment.getId())
        .stream()
        .filter(resource -> resource.getState() == NodeStates.INITIAL)
        .forEach(resource -> resource.setState(NodeStates.CREATING));

    // Update status of the deployment
    deployment.setTask(Task.DEPLOYER);

    ArchiveRoot ar = prepareTemplate(deployment, deploymentMessage);

    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();

    String accessToken = null;
    if (oidcProperties.isEnabled()) {
      accessToken = oauth2TokenService.getAccessToken(requestedWithToken);
    }
    toscaService.addElasticClusterParameters(ar, deployment.getId(), accessToken);
    ComputeService computeService = deploymentMessage
        .getCloudServicesOrderedIterator()
        .currentService(ComputeService.class);
    toscaService.contextualizeAndReplaceImages(ar, computeService, DeploymentProvider.IM);
    toscaService.contextualizeAndReplaceFlavors(ar, computeService, DeploymentProvider.IM);

    List<CloudProviderEndpoint> cloudProviderEndpoints =
        deployment.getCloudProviderEndpoint().getAllCloudProviderEndpoint();

    if (toscaService.isHybridDeployment(ar)) {
      toscaService.setHybridDeployment(ar);
    }
    String imCustomizedTemplate = toscaService.getTemplateFromTopology(ar);
    // Deploy on IM
    try {
      String infrastructureId =
          executeWithClientForResult(cloudProviderEndpoints, requestedWithToken,
              client -> client.createInfrastructureAsync(imCustomizedTemplate,
                BodyContentType.TOSCA)).getInfrastructureId();
      LOG.info("InfrastructureId for deployment <{}> is: {}", deploymentMessage.getDeploymentId(),
          infrastructureId);
      deployment.setEndpoint(infrastructureId);
    } catch (ImClientException ex) {
      throw handleImClientException(ex);
    }
    return true;
  }

  @Override
  public boolean isDeployed(DeploymentMessage deploymentMessage) {
    Deployment deployment = getDeployment(deploymentMessage);

    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();

    List<CloudProviderEndpoint> cloudProviderEndpoints =
        deployment.getCloudProviderEndpoint().getAllCloudProviderEndpoint();

    try {

      InfrastructureState infrastructureState = executeWithClientForResult(cloudProviderEndpoints,
          requestedWithToken, client -> client.getInfrastructureState(deployment.getEndpoint()));

      LOG.debug(infrastructureState.getFormattedInfrastructureStateString());

      bindResources(deploymentMessage, infrastructureState);

      switch (infrastructureState.getEnumState()) {
        case CONFIGURED:
          return true;
        case FAILED:
        case UNCONFIGURED:
          Optional<String> additionalErrorInfo = getAdditionalErrorInfo(deploymentMessage);
          StringBuilder sb =
              new StringBuilder(
                  "Some error occurred during the contextualization of the IM infrastructure\n")
                      .append(infrastructureState.getFormattedInfrastructureStateString());
          additionalErrorInfo.ifPresent(s -> sb.append("\n").append(s));

          throw new BusinessWorkflowException(ErrorCode.CLOUD_PROVIDER_ERROR,
              "Error deploying the infrastructure",
              new DeploymentException(sb.toString()));
        default:
          return false;
      }
    } catch (ImClientException exception) {
      throw handleImClientException(exception);
    }
  }

  @Override
  public Optional<String> getAdditionalErrorInfoInternal(DeploymentMessage deploymentMessage) {

    Deployment deployment = getDeployment(deploymentMessage);

    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();

    List<CloudProviderEndpoint> cloudProviderEndpoints =
        deployment.getCloudProviderEndpoint().getAllCloudProviderEndpoint();

    // Try to get the logs of the virtual infrastructure for debug purposes.
    try {
      Property contMsg = executeWithClientForResult(cloudProviderEndpoints, requestedWithToken,
          client -> client.getInfrastructureContMsg(deployment.getEndpoint()));
      if (!Strings.isNullOrEmpty(contMsg.getValue())) {
        return Optional.of(String.format("Contextualization Message is:%n%s", contMsg.getValue()));
      }
    } catch (ImClientException exception) {
      throw handleImClientException(exception);
    }
    return Optional.empty();
  }

  @Override
  public void finalizeDeploy(DeploymentMessage deploymentMessage) {

    Deployment deployment = getDeployment(deploymentMessage);

    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();

    List<CloudProviderEndpoint> cloudProviderEndpoints =
        deployment.getCloudProviderEndpoint().getAllCloudProviderEndpoint();

    try {
      deployment
          .setOutputs(executeWithClientForResult(cloudProviderEndpoints, requestedWithToken,
              client -> client.getInfrastructureOutputs(deployment.getEndpoint()))
                  .getOutputs());
    } catch (ImClientException exception) {
      throw handleImClientException(exception);
    }
    updateOnSuccess(deployment.getId());
  }

  @Override
  public void cleanFailedDeploy(DeploymentMessage deploymentMessage) {
    CloudServicesOrderedIterator iterator = deploymentMessage.getCloudServicesOrderedIterator();
    boolean isLastProvider = !iterator.hasNext();
    boolean isKeepLastAttempt = deploymentMessage.isKeepLastAttempt();
    LOG.info("isLastProvider: {} and isKeepLastAttempt: {}", isLastProvider, isKeepLastAttempt);

    Deployment deployment = getDeployment(deploymentMessage);
    String deploymentEndpoint = deployment.getEndpoint();

    if (deploymentEndpoint == null) {
      LOG.info("Nothing left to clean up from last deployment attempt");
    } else if (isLastProvider && isKeepLastAttempt) {
      LOG.info("Keeping the last deployment attempt");
    } else {
      LOG.info("Deleting the last deployment attempt");

      OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();

      List<CloudProviderEndpoint> cloudProviderEndpoints =
          deployment.getCloudProviderEndpoint().getAllCloudProviderEndpoint();

      try {
        executeWithClient(cloudProviderEndpoints, requestedWithToken,
            client -> client.destroyInfrastructureAsync(deploymentEndpoint));
        deployment.setEndpoint(null);
      } catch (ImClientErrorException exception) {
        if (!exception.getResponseError().is404Error()) {
          throw handleImClientException(exception);
        }
      } catch (ImClientException exception) {
        throw handleImClientException(exception);
      }
    }
  }

  @Override
  public boolean doUpdate(DeploymentMessage deploymentMessage, String template) {

    Deployment deployment = getDeployment(deploymentMessage);

    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
    final CloudProviderEndpoint chosenCloudProviderEndpoint =
        deploymentMessage.getChosenCloudProviderEndpoint();

    ArchiveRoot newAr = prepareTemplate(deployment, deploymentMessage);

    String accessToken = null;
    if (oidcProperties.isEnabled()) {
      accessToken = oauth2TokenService.getAccessToken(requestedWithToken);
    }
    toscaService.addElasticClusterParameters(newAr, deployment.getId(), accessToken);

    // Ordered set of resources to be removed
    Set<Resource> resourcesToRemove = new LinkedHashSet<>();
    Set<String> vmsToRemove = new LinkedHashSet<>();

    try {
      List<CloudProviderEndpoint> cloudProviderEndpoints =
          deployment.getCloudProviderEndpoint().getAllCloudProviderEndpoint();
      InfrastructureState infrastructureState = executeWithClientForResult(cloudProviderEndpoints,
          requestedWithToken, client -> client.getInfrastructureState(deployment.getEndpoint()));
      Set<String> exsistingVms =
          Optional
              .ofNullable(infrastructureState.getVmStates())
              .map(Map::keySet)
              .orElseGet(LinkedHashSet::new);
      deployment
          .getResources()
          .stream()
          .filter(resource -> resource.getIaasId() != null)
          .forEach(resource -> {
            if (!exsistingVms.remove(resource.getIaasId())) {
              resource.setIaasId(null); // exclude it from the IM invocation so we will not get 404
              resource.setState(NodeStates.DELETING);
              resourcesToRemove.add(resource);
            } else {
              if (resource.getState() == NodeStates.DELETING) {
                resourcesToRemove.add(resource);
              }
            }
          });
      vmsToRemove.addAll(exsistingVms); // remaining VMs that we didn't know of their existence
    } catch (ImClientException exception) {
      throw handleImClientException(exception);
    }

    updateResources(deployment, deployment.getStatus());

    boolean newResourcesOnDifferentService = !chosenCloudProviderEndpoint
            .getCpComputeServiceId()
            .equals(deployment.getCloudProviderEndpoint().getCpComputeServiceId());

    if (newResourcesOnDifferentService) {
      toscaService.setHybridUpdateDeployment(newAr);
    }

    Map<String, NodeTemplate> newNodes =
        Optional
            .ofNullable(newAr.getTopology())
            .map(Topology::getNodeTemplates)
            .orElseGet(Collections::emptyMap);

    deployment
        .getResources()
        .stream()
        .collect(Collectors.groupingBy(Resource::getToscaNodeName))
        .forEach((name, resources) -> {
          Optional<NodeTemplate> optionalNewNode = CommonUtils
              .getFromOptionalMap(newNodes, name);
          if (!optionalNewNode.isPresent()) {
            // no node with same name in updated template
            resources.forEach(resource -> {
              resource.setState(NodeStates.DELETING);
              resourcesToRemove.add(resource);
            });
          } else {
            NodeTemplate newNode = optionalNewNode.get();
            String toscaType = newNode.getType();
            Map<Boolean, List<Resource>> resourcesWithRightType = resources
                .stream()
                .collect(Collectors.partitioningBy(
                    resource -> toscaType.equals(resource.getToscaNodeType())));
            resourcesWithRightType
                .get(false)
                .forEach(resource -> {
                  resource.setState(NodeStates.DELETING);
                  resourcesToRemove.add(resource);
                });

            List<String> removalList = toscaService.getRemovalList(newNode);
            toscaService.removeRemovalList(newNode);

            removalList.forEach(resourceId -> {
              Resource resource =
                  resourcesWithRightType
                      .get(true)
                      .stream()
                      .filter(elem -> resourceId.equals(elem.getId()))
                      .collect(
                          MoreCollectors.toOptional())
                      .orElseThrow(() -> new DeploymentException(
                          String.format("Unknown resource with id %s, name %s and type %s",
                              resourceId, name, toscaType)));
              resource.setState(NodeStates.DELETING);
              resourcesToRemove.add(resource);
            });
            long newCount = toscaService.getCount(newNode).orElse(1L);
            List<Resource> remainingResources = resources
                .stream()
                .filter(resource -> resource.getState() != NodeStates.DELETING)
                // null (thus no IaaS resources) first
                .sorted(Comparator.comparing(resource -> resource.getIaasId() != null ? 1 : 0))
                .collect(Collectors.toList());

            for (int i = 0; i < remainingResources.size() - newCount; ++i) {
              Resource resource = remainingResources.get(i);
              resource.setState(NodeStates.DELETING);
              resourcesToRemove.add(resource);
            }
          }
        });

    newNodes.forEach((name, newNode) -> {

      List<Resource> resources = deployment
          .getResources()
          .stream()
          .filter(resource -> name.equals(resource.getToscaNodeName())
              && newNode.getType().equals(resource.getToscaNodeType())
              && resource.getState() != NodeStates.DELETING)
          .collect(Collectors.toList());

      long newCount = toscaService.getCount(newNode).orElse(1L);
      int oldCount = resources.size();
      long diff = newCount - oldCount;
      for (long i = 0; i < diff; i++) {
        Resource resource = new Resource();
        resource.setDeployment(deployment);
        resource.setState(NodeStates.CREATING);
        resource.setToscaNodeName(name);
        resource.setToscaNodeType(newNode.getType());
        if (newResourcesOnDifferentService) {
          resource.setCloudProviderEndpoint(chosenCloudProviderEndpoint);
        }
        resourceRepository.save(resource);
      }
    });

    ComputeService computeService = deploymentMessage
        .getCloudServicesOrderedIterator()
        .currentService(ComputeService.class);

    toscaService.contextualizeAndReplaceImages(newAr, computeService, DeploymentProvider.IM);
    toscaService.contextualizeAndReplaceFlavors(newAr, computeService, DeploymentProvider.IM);

    // FIXME: There's not check if the Template actually changed!
    deployment.setTemplate(toscaService.updateTemplate(template));

    resourcesToRemove
        .stream()
        .map(Resource::getIaasId)
        .filter(Objects::nonNull)
        .forEach(vmsToRemove::add);

    try {
      if (!vmsToRemove.isEmpty()) {
        List<CloudProviderEndpoint> cloudProviderEndpoints =
            deployment.getCloudProviderEndpoint().getAllCloudProviderEndpoint();

        LOG.debug("Deleting VMs {}", vmsToRemove);

        executeWithClient(cloudProviderEndpoints, requestedWithToken, client -> client
            .removeResource(deployment.getEndpoint(), new ArrayList<>(vmsToRemove)));
      } else {
        LOG.debug("No VMs to delete");
      }

      String templateToDeploy = toscaService.getTemplateFromTopology(newAr);
      LOG.debug("Template sent: \n{}", templateToDeploy);

      List<CloudProviderEndpoint> cloudProviderEndpoints =
          deployment.getCloudProviderEndpoint().getAllCloudProviderEndpoint();
      cloudProviderEndpoints.add(0, chosenCloudProviderEndpoint);

      executeWithClientForResult(cloudProviderEndpoints,
          requestedWithToken, client -> client.addResource(deployment.getEndpoint(),
              templateToDeploy, BodyContentType.TOSCA));
    } catch (ImClientException exception) {
      throw handleImClientException(exception);
    }
    return true;
  }

  @Override
  public void cleanFailedUpdate(DeploymentMessage deploymentMessage) {
    // DO NOTHING
  }

  @Override
  public boolean doUndeploy(DeploymentMessage deploymentMessage) {
    Deployment deployment = getDeployment(deploymentMessage);

    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();

    String deploymentEndpoint = deployment.getEndpoint();
    if (deploymentEndpoint != null) {
      deployment.setTask(Task.DEPLOYER);

      List<CloudProviderEndpoint> cloudProviderEndpoints =
          deployment.getCloudProviderEndpoint().getAllCloudProviderEndpoint();

      try {
        executeWithClient(cloudProviderEndpoints, requestedWithToken,
            client -> client.destroyInfrastructureAsync(deploymentEndpoint));

      } catch (ImClientErrorException exception) {
        if (!exception.getResponseError().is404Error()) {
          throw handleImClientException(exception);
        }
      } catch (ImClientException exception) {
        throw handleImClientException(exception);
      }
    }
    return true;
  }

  @Override
  public boolean isUndeployed(DeploymentMessage deploymentMessage) {

    Deployment deployment = getDeployment(deploymentMessage);

    String deploymentEndpoint = deployment.getEndpoint();
    if (deploymentEndpoint == null) {
      return true;
    }

    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
    List<CloudProviderEndpoint> cloudProviderEndpoints =
        deployment.getCloudProviderEndpoint().getAllCloudProviderEndpoint();
    try {
      InfrastructureState infrastructureState =
          executeWithClientForResult(cloudProviderEndpoints, requestedWithToken,
              client -> client.getInfrastructureState(deploymentEndpoint));

      LOG.debug(infrastructureState.getFormattedInfrastructureStateString());
    } catch (ImClientErrorException exception) {
      if (exception.getResponseError().is404Error()) {
        return true;
      } else {
        throw handleImClientException(exception);
      }
    } catch (ImClientException exception) {
      throw handleImClientException(exception);
    }
    return false;
  }

  @Override
  public void doProviderTimeout(DeploymentMessage deploymentMessage) {
    throw new BusinessWorkflowException(ErrorCode.CLOUD_PROVIDER_ERROR,
        "Error executing request to IM",
        new DeploymentException("IM provider timeout during deployment"));
  }

  /**
   * Match the {@link Resource} to IM vms.
   *
   * @param infrastructureState
   *
   */
  private void bindResources(DeploymentMessage deploymentMessage,
      InfrastructureState infrastructureState)
      throws ImClientException {

    Deployment deployment = getDeployment(deploymentMessage);
    String infrastructureId = deployment.getEndpoint();
    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();

    List<CloudProviderEndpoint> cloudProviderEndpoints =
        deployment.getCloudProviderEndpoint().getAllCloudProviderEndpoint();

    // for each URL get the tosca Node Name about the VM
    Multimap<String, String> vmMap = HashMultimap.create();
    for (String vmId : infrastructureState.getVmStates().keySet()) {
      VirtualMachineInfo vmInfo =
          executeWithClientForResult(cloudProviderEndpoints, requestedWithToken,
              client -> client.getVmInfo(infrastructureId, vmId));
      vmInfo
          .getVmProperties()
          .stream()
          .filter(Objects::nonNull)
          .filter(properties -> "system".equals(properties.get("class")))
          .map(properties -> properties.get("id"))
          .filter(Objects::nonNull)
          .map(Object::toString)
          .findAny()
          .ifPresent(toscaNodeName -> vmMap.put(toscaNodeName, vmId));
    }

    Map<Boolean, Set<Resource>> resources =
        resourceRepository
            .findByDeployment_id(deployment.getId())
            .stream()
            .collect(Collectors.partitioningBy(resource -> resource.getIaasId() != null,
                Collectors.toSet()));

    for (Resource bindedResource : resources.get(true)) {
      boolean vmIsPresent =
          vmMap.get(bindedResource.getToscaNodeName()).remove(bindedResource.getIaasId());
      if (!vmIsPresent && bindedResource.getState() != NodeStates.DELETING) {
        // the node isn't supposed to be deleted -> put it again in the pool of bindable resources
        // TODO maybe throw an error? Eventual consistency (for update) should already have been
        // handled
        LOG.warn("Resource <{}> in status {} was binded to the VM <{}> which doesn't exist anymore",
            bindedResource.getId(), bindedResource.getState(), bindedResource.getIaasId());
        bindedResource.setIaasId(null);
        bindedResource.setCloudProviderEndpoint(null);
        resources.get(false).add(bindedResource);
      }
    }

    for (Resource resource : resources.get(false)) {
      Collection<String> vmIds = vmMap.get(resource.getToscaNodeName());
      vmIds.stream().findAny().ifPresent(vmId -> {
        resource.setIaasId(vmId);
        vmIds.remove(vmId);
      });
    }
    if (!vmMap.isEmpty()) {
      LOG.warn("Some VMs of infrastructure <{}> couldn't be binded to a resource: {}",
          infrastructureId, vmMap.entries());
    }
  }

  private RuntimeException handleImClientException(ImClientException ex) {
    if (ex instanceof ImClientServerErrorException) {
      ResponseError responseError = ((ImClientServerErrorException) ex).getResponseError();
      return new BusinessWorkflowException(ErrorCode.CLOUD_PROVIDER_ERROR,
          responseError.getFormattedErrorMessage(),
          ex);
    } else if (ex instanceof ImClientErrorException) {
      ResponseError responseError = ((ImClientErrorException) ex).getResponseError();
      return new DeploymentException(
          "Error executing request to IM\n" + responseError.getFormattedErrorMessage(), ex);
    }
    return new DeploymentException("Error executing request to IM", ex);
  }

  @Override
  protected void updateResources(Deployment deployment, Status status) {
    // // WARNING: In IM we don't have the resource mapping yet, so we update all the resources
    // // FIXME Remove once IM handles single nodes state update!!!! And pay attention to the
    // // AbstractDeploymentProviderService.updateOnError method!
    super.updateResources(deployment, status);
  }

}
