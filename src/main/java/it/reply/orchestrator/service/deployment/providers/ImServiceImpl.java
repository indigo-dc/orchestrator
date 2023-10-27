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

package it.reply.orchestrator.service.deployment.providers;

import alien4cloud.tosca.model.ArchiveRoot;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import it.reply.orchestrator.config.properties.OrchestratorProperties;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.cmdb.ComputeService;
import it.reply.orchestrator.dto.deployment.ActionMessage;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.iam.WellKnownResponse;
import it.reply.orchestrator.dto.workflow.CloudServicesOrderedIterator;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.exception.http.BadRequestException;
import it.reply.orchestrator.exception.service.BusinessWorkflowException;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.function.ThrowingConsumer;
import it.reply.orchestrator.function.ThrowingFunction;
import it.reply.orchestrator.service.IndigoInputsPreProcessorService;
import it.reply.orchestrator.service.IndigoInputsPreProcessorService.RuntimeProperties;
import it.reply.orchestrator.service.ToscaService;
import it.reply.orchestrator.service.deployment.providers.factory.ImClientFactory;
import it.reply.orchestrator.service.security.CustomOAuth2TemplateFactory;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.orchestrator.service.IamService;
import it.reply.orchestrator.service.IamServiceException;
import it.reply.orchestrator.utils.CommonUtils;
import it.reply.orchestrator.utils.JwtUtils;
import it.reply.orchestrator.utils.OneDataUtils;
import it.reply.orchestrator.utils.ToscaConstants;
import it.reply.orchestrator.utils.WorkflowConstants.ErrorCode;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;

import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.mitre.oauth2.model.RegisteredClient;
import org.mitre.openid.connect.client.service.impl.StaticClientConfigurationService;

@Service
@DeploymentProviderQualifier(DeploymentProvider.IM)
@EnableConfigurationProperties(ImProperties.class)
@Slf4j
public class ImServiceImpl extends AbstractDeploymentProviderService {

  @Autowired
  public ImServiceImpl(RestTemplateBuilder restTemplateBuilder,
      CustomOAuth2TemplateFactory templateFactory,
      StaticClientConfigurationService staticClientConfigurationService,
      ImClientFactory imClientFactory) {
    this.restTemplate = restTemplateBuilder.build();
    this.templateFactory = templateFactory;
    this.staticClientConfigurationService = staticClientConfigurationService;
    this.imClientFactory = imClientFactory;
  }

  private final RestTemplate restTemplate;

  @Autowired
  private IamService iamService;

  private final CustomOAuth2TemplateFactory templateFactory;

  private final StaticClientConfigurationService staticClientConfigurationService;

  @Autowired
  private ToscaService toscaService;

  @Autowired
  private ResourceRepository resourceRepository;

  @Autowired
  private IndigoInputsPreProcessorService indigoInputsPreProcessorService;

  @Autowired
  private OidcProperties oidcProperties;

  @Autowired
  private OrchestratorProperties orchestratorProperties;

  @Autowired
  private OAuth2TokenService oauth2TokenService;

  private final ImClientFactory imClientFactory;

  private static final String VMINFO = "VirtualMachineInfo";
  public static final String IAM_TOSCA_NODE_TYPE = "tosca.nodes.indigo.iam.client";
  public static final String ISSUER = "issuer";
  public static final String OWNER = "owner";
  private static final String CLIENT_ID = "client_id";

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

  protected ArchiveRoot prepareTemplate(String template, Deployment deployment,
      DeploymentMessage deploymentMessage) {
    RuntimeProperties runtimeProperties = OneDataUtils.getOneDataRuntimeProperties(deploymentMessage);
    Map<String, Object> inputs = deployment.getParameters();
    ArchiveRoot ar = toscaService.parseAndValidateTemplate(template, inputs);
    if (runtimeProperties.getVaules().size() > 0) {
      indigoInputsPreProcessorService.processGetInputAttributes(ar, inputs, runtimeProperties);
    } else {
      indigoInputsPreProcessorService.processGetInput(ar, inputs);
    }
    return ar;
  }

  @Override
  public boolean doDeploy(DeploymentMessage deploymentMessage) {
    Deployment deployment = getDeployment(deploymentMessage);
    String uuid = deployment.getId();

    resourceRepository
        .findByDeployment_id(deployment.getId())
        .stream()
        .filter(resource -> resource.getState() == NodeStates.INITIAL)
        .forEach(resource -> resource.setState(NodeStates.CREATING));

    // Update status of the deployment
    deployment.setTask(Task.DEPLOYER);

    ArchiveRoot ar = prepareTemplate(deployment.getTemplate(), deployment, deploymentMessage);

    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();

    String accessToken = null;
    if (oidcProperties.isEnabled()) {
      accessToken = oauth2TokenService.getAccessToken(requestedWithToken);
    }

    String email = null;
    String issuerUser = null;
    String sub = null;
    if (accessToken != null) {
      try {
        email = JwtUtils.getJwtClaimsSet(JwtUtils.parseJwt(accessToken)).getStringClaim("email");
      } catch (ParseException e) {
        LOG.debug(e.getMessage());
        email = null;
      }
      try {
        issuerUser = JwtUtils.getJwtClaimsSet(JwtUtils.parseJwt(accessToken)).getStringClaim("iss");
      } catch (ParseException e) {
        String errorMessage = String.format("Issuer not found in user's token. %s",
            e.getMessage());
        LOG.error(errorMessage);
        throw new IamServiceException(errorMessage, e);
      }
      try {
        sub = JwtUtils.getJwtClaimsSet(JwtUtils.parseJwt(accessToken)).getStringClaim("sub");
      } catch (ParseException e) {
        String errorMessage = String.format("Sub not found in user's token. %s",
            e.getMessage());
        LOG.error(errorMessage);
        throw new IamServiceException(errorMessage, e);
      }
    }

    Map<Boolean, Set<Resource>> resources = resourceRepository
        .findByDeployment_id(deployment.getId())
        .stream()
        .collect(Collectors.partitioningBy(resource -> resource.getIaasId() != null,
            Collectors.toSet()));

    toscaService.addElasticClusterParameters(ar, deployment.getId(), accessToken);
    ComputeService computeService = deploymentMessage
        .getCloudServicesOrderedIterator()
        .currentService(ComputeService.class);
    toscaService.contextualizeAndReplaceImages(ar, computeService, DeploymentProvider.IM);
    toscaService.contextualizeAndReplaceFlavors(ar, computeService, DeploymentProvider.IM);
    toscaService.contextualizeAndReplaceVolumeTypes(ar, computeService, DeploymentProvider.IM);

    List<CloudProviderEndpoint> cloudProviderEndpoints = deployment.getCloudProviderEndpoint()
        .getAllCloudProviderEndpoint();

    if (toscaService.isHybridDeployment(ar)) {
      toscaService.setHybridDeployment(ar,
          computeService.getPublicNetworkName(),
          computeService.getPrivateNetworkName(),
          computeService.getPrivateNetworkCidr());
    } else {
      toscaService.setNetworkNames(ar,
          computeService.getPublicNetworkName(),
          computeService.getPrivateNetworkName(),
          computeService.getPrivateNetworkProxyHost(),
          computeService.getPrivateNetworkProxyUser());
    }

    // add tags
    toscaService.setDeploymentTags(ar,
        orchestratorProperties.getUrl().toString(),
        deployment.getId(),
        email);

    // Define a map of properties of the TOSCA template related to the
    // IAM_TOSCA_NODE_TYPE
    // nodes as input of the orchestrator and a map of those to be submitted to the
    // IM
    Map<String, Map<String, String>> iamTemplateInput = null;
    Map<String, Map<String, String>> iamTemplateOutput = new HashMap<>();

    // Get information about the clients related to the orchestrator
    Map<String, RegisteredClient> clients = staticClientConfigurationService.getClients();

    // Loop over the deployment resources and create an IAM client for all the
    // IAM_TOSCA_NODE_TYPE nodes requested
    for (Resource resource : resources.get(false)) {
      if (resource.getToscaNodeType().equals(IAM_TOSCA_NODE_TYPE)) {
        String nodeName = resource.getToscaNodeName();
        LOG.info("Found node of type: {}. Node name: {}", IAM_TOSCA_NODE_TYPE, nodeName);
        String scopes;
        String issuerNode;
        String tokenCredentials = null;
        Map<String, String> clientCreated = new HashMap<>();

        // Get properties of IAM_TOSCA_NODE_TYPE nodes from the TOSCA template
        if (iamTemplateInput == null) {
          iamTemplateInput = toscaService.getIamProperties(ar);
        }

        // Set the issuer of the current node
        if (iamTemplateInput.get(nodeName).get(ISSUER) != null) {
          issuerNode = iamTemplateInput.get(nodeName).get(ISSUER);
        } else {
          issuerNode = issuerUser;
        }

        // Check if the issuer is an IAM
        if (!iamService.checkIam(restTemplate, issuerNode)) {
          String errorMessage = String.format("%s is not an IAM. Only IAM providers are supported",
              issuerNode);
          LOG.error(errorMessage);
          iamService.deleteAllClients(restTemplate, resources);
          throw new IamServiceException(errorMessage);
        }

        // Extract the useful information of the IAM issuer from the wellknown endpoint
        WellKnownResponse wellKnownResponse = iamService.getWellKnown(restTemplate, issuerNode);

        // Set the scopes of the requested client
        if (iamTemplateInput.get(nodeName).get("scopes") != null) {
          scopes = iamTemplateInput.get(nodeName).get("scopes");
          List<String> inputList = Lists.newArrayList(scopes.split(" "));
          try {
            inputList.retainAll(wellKnownResponse.getScopesSupported());
          } catch (RuntimeException e) {
            String errorMessage = String.format("Impossible to set IAM scopes of node %s. %s",
                nodeName, e.getMessage());
            LOG.error(errorMessage);
            iamService.deleteAllClients(restTemplate, resources);
            throw new IamServiceException(errorMessage, e);
          }
          scopes = String.join(" ", inputList);
        } else {
          scopes = String.join(" ", wellKnownResponse.getScopesSupported());
        }

        if (scopes.isEmpty()) {
          String errorMessage = "Zero scopes allowed provided are not sufficient to create a client";
          LOG.error(errorMessage);
          iamService.deleteAllClients(restTemplate, resources);
          throw new IamServiceException(errorMessage);
        }

        // Create an IAM client
        try {
          LOG.info("Creating client with the identity provider {}", issuerNode);
          clientCreated = iamService.createClient(restTemplate,
              wellKnownResponse.getRegistrationEndpoint(), uuid, email, scopes);
        } catch (IamServiceException e) {
          iamService.deleteAllClients(restTemplate, resources);
          throw e;
        }

        // Set metadata and set TOSCA template properties of the IAM_TOSCA_NODE_TYPE
        // node
        Map<String, String> resourceMetadata = new HashMap<>();
        resourceMetadata.put(ISSUER, issuerNode);
        resourceMetadata.put(CLIENT_ID, clientCreated.get(CLIENT_ID));
        resourceMetadata.put("registration_access_token",
            clientCreated.get("registration_access_token"));
        resource.setMetadata(resourceMetadata);
        iamTemplateOutput.put(nodeName, resourceMetadata);

        if (!clients.containsKey(issuerNode)) {
          String errorMessage = String.format("There is no orchestrator client belonging to the " +
              "identity provider: %s. Impossible to set the ownership of the client with client_id %s",
              issuerNode, clientCreated.get(CLIENT_ID));
          LOG.warn(errorMessage);
        } else
          try {
            // Get the orchestrator client related to the issuer of the node and extract
            // client_id and client_secret
            RegisteredClient orchestratorClient = clients.get(issuerNode);
            String orchestratorClientId = orchestratorClient.getClientId();
            String orchestratorClientSecret = orchestratorClient.getClientSecret();

            // Request a token with client_credentials with the orchestrator client, when
            // necessary
            if (iamTemplateInput.get(nodeName).get(OWNER) != null ||
                issuerNode.equals(issuerUser)) {
              tokenCredentials = iamService.getTokenClientCredentials(
                  restTemplate, orchestratorClientId, orchestratorClientSecret,
                  iamService.getOrchestratorScopes(), wellKnownResponse.getTokenEndpoint());
            }
            // Assign ownership for the client when possible
            if (iamTemplateInput.get(nodeName).get(OWNER) != null) {
              iamService.assignOwnership(clientCreated.get(CLIENT_ID), issuerNode,
                  iamTemplateInput.get(nodeName).get(OWNER), tokenCredentials);
            }
            if (iamTemplateInput.get(nodeName).get(OWNER) == null &&
                issuerNode.equals(issuerUser)) {
              iamService.assignOwnership(clientCreated.get(CLIENT_ID),
                  issuerNode, sub, tokenCredentials);
            }
          } catch (IamServiceException e) {
            if (tokenCredentials == null) {
              String errorMessage = String.format("Impossible to set the ownership of the client " +
                  "with client_id %s and issuer %s", clientCreated.get(CLIENT_ID), issuerNode);
              LOG.warn(errorMessage);
            }
            // If some error occurred, do not delete all the clients,
            // just do not set the owner of a problem customer
          }
      }
    }

    // Update the template with properties of IAM_TOSCA_NODE_TYPE nodes
    toscaService.setDeploymentClientIam(ar, iamTemplateOutput);

    String imCustomizedTemplate = toscaService.serialize(ar);

    // Deploy on IM
    try {
      String infrastructureId = executeWithClientForResult(cloudProviderEndpoints, requestedWithToken,
          client -> client.createInfrastructureAsync(imCustomizedTemplate,
              BodyContentType.TOSCA))
          .getInfrastructureId();
      LOG.info("InfrastructureId for deployment <{}> is: {}", deploymentMessage.getDeploymentId(),
          infrastructureId);
      deployment.setEndpoint(infrastructureId);
    } catch (ImClientException ex) {
      iamService.deleteAllClients(restTemplate, resources);
      throw handleImClientException(ex);
    }

    return true;
  }

  @Override
  public boolean isDeployed(DeploymentMessage deploymentMessage) {
    Deployment deployment = getDeployment(deploymentMessage);

    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();

    List<CloudProviderEndpoint> cloudProviderEndpoints = deployment.getCloudProviderEndpoint()
        .getAllCloudProviderEndpoint();

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
          StringBuilder sb = new StringBuilder(
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

    List<CloudProviderEndpoint> cloudProviderEndpoints = deployment.getCloudProviderEndpoint()
        .getAllCloudProviderEndpoint();

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
  public Optional<String> getDeploymentLogInternal(DeploymentMessage deploymentMessage) {
    Deployment deployment = getDeployment(deploymentMessage);

    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();

    List<CloudProviderEndpoint> cloudProviderEndpoints = deployment.getCloudProviderEndpoint()
        .getAllCloudProviderEndpoint();

    // Try to get the logs of the virtual infrastructure.
    try {
      Property contMsg = executeWithClientForResult(cloudProviderEndpoints, requestedWithToken,
          client -> client.getInfrastructureContMsg(deployment.getEndpoint()));
      if (!Strings.isNullOrEmpty(contMsg.getValue())) {
        return Optional.of(contMsg.getValue());
      }
    } catch (ImClientException exception) {
      throw handleImClientException(exception);
    }
    return Optional.empty();
  }

  @Override
  public Optional<String> getDeploymentExtendedInfoInternal(DeploymentMessage deploymentMessage) {
    Deployment deployment = getDeployment(deploymentMessage);

    Map<Boolean, Set<Resource>> resources = resourceRepository
        .findByDeployment_id(deployment.getId())
        .stream()
        .collect(Collectors.partitioningBy(resource -> (resource.getIaasId() != null && resource.getMetadata() != null),
            Collectors.toSet()));
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    boolean first = true;
    for (Resource resource : resources.get(true)) {
      Map<String, String> resourceMetadata = resource.getMetadata();
      if (resourceMetadata != null && resourceMetadata.containsKey(VMINFO)) {
        if (!first) {
          sb.append(",");
        }
        first = false;
        sb.append(resourceMetadata.get(VMINFO));
      }
    }
    sb.append("]");
    return Optional.of(sb.toString());
  }

  @Override
  public void finalizeDeploy(DeploymentMessage deploymentMessage) {

    Deployment deployment = getDeployment(deploymentMessage);

    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();

    List<CloudProviderEndpoint> cloudProviderEndpoints = deployment.getCloudProviderEndpoint()
        .getAllCloudProviderEndpoint();

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

      List<CloudProviderEndpoint> cloudProviderEndpoints = deployment.getCloudProviderEndpoint()
          .getAllCloudProviderEndpoint();

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
    final CloudProviderEndpoint chosenCloudProviderEndpoint = deploymentMessage.getChosenCloudProviderEndpoint();

    ArchiveRoot newAr = prepareTemplate(template, deployment, deploymentMessage);

    String accessToken = null;
    if (oidcProperties.isEnabled()) {
      accessToken = oauth2TokenService.getAccessToken(requestedWithToken);
    }
    toscaService.addElasticClusterParameters(newAr, deployment.getId(), accessToken);

    // Ordered set of resources to be removed
    Set<Resource> resourcesToRemove = new LinkedHashSet<>();
    Set<String> vmsToRemove = new LinkedHashSet<>();

    try {
      List<CloudProviderEndpoint> cloudProviderEndpoints = deployment.getCloudProviderEndpoint()
          .getAllCloudProviderEndpoint();
      InfrastructureState infrastructureState = executeWithClientForResult(cloudProviderEndpoints,
          requestedWithToken, client -> client.getInfrastructureState(deployment.getEndpoint()));
      Set<String> exsistingVms = Optional
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

    ComputeService computeService = deploymentMessage
        .getCloudServicesOrderedIterator()
        .firstService(ComputeService.class);

    if (deploymentMessage.isHybrid()) {
      toscaService.setHybridUpdateDeployment(newAr,
          newResourcesOnDifferentService,
          computeService.getPublicNetworkName(),
          computeService.getPrivateNetworkName(),
          computeService.getPrivateNetworkCidr());
    }

    Map<String, NodeTemplate> newNodes = Optional
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
              Resource resource = resourcesWithRightType
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

    toscaService.contextualizeAndReplaceImages(newAr, computeService, DeploymentProvider.IM);
    toscaService.contextualizeAndReplaceFlavors(newAr, computeService, DeploymentProvider.IM);
    toscaService.contextualizeAndReplaceVolumeTypes(newAr, computeService, DeploymentProvider.IM);

    // FIXME: There's not check if the Template actually changed!
    deployment.setTemplate(toscaService.updateTemplate(template));

    resourcesToRemove
        .stream()
        .map(Resource::getIaasId)
        .filter(Objects::nonNull)
        .forEach(vmsToRemove::add);

    try {
      if (!vmsToRemove.isEmpty()) {
        List<CloudProviderEndpoint> cloudProviderEndpoints = deployment.getCloudProviderEndpoint()
            .getAllCloudProviderEndpoint();

        LOG.debug("Deleting VMs {}", vmsToRemove);

        executeWithClient(cloudProviderEndpoints, requestedWithToken, client -> client
            .removeResource(deployment.getEndpoint(), new ArrayList<>(vmsToRemove)));
      } else {
        LOG.debug("No VMs to delete");
      }

      String templateToDeploy = toscaService.serialize(newAr);
      LOG.debug("Template sent: \n{}", templateToDeploy);

      List<CloudProviderEndpoint> cloudProviderEndpoints = deployment.getCloudProviderEndpoint()
          .getAllCloudProviderEndpoint();
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

    Map<Boolean, Set<Resource>> resources = resourceRepository
        .findByDeployment_id(deployment.getId())
        .stream()
        .collect(Collectors.partitioningBy(resource -> resource.getIaasId() != null,
            Collectors.toSet()));

    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();

    String deploymentEndpoint = deployment.getEndpoint();

    if (deploymentEndpoint != null) {
      deployment.setTask(Task.DEPLOYER);

      List<CloudProviderEndpoint> cloudProviderEndpoints = deployment.getCloudProviderEndpoint()
          .getAllCloudProviderEndpoint();

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

    // Delete all IAM clients if there are resources of type IAM_TOSCA_NODE_TYPE
    iamService.deleteAllClients(restTemplate, resources);

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
    List<CloudProviderEndpoint> cloudProviderEndpoints = deployment.getCloudProviderEndpoint()
        .getAllCloudProviderEndpoint();
    try {
      InfrastructureState infrastructureState = executeWithClientForResult(cloudProviderEndpoints, requestedWithToken,
          client -> client.getInfrastructureState(deploymentEndpoint));

      LOG.debug(infrastructureState.getFormattedInfrastructureStateString());

      if (infrastructureState.getState().equalsIgnoreCase("UNKNOWN")) {
        ResponseError error = new ResponseError("Infrastructure state UNKNOWN", 500);
        ImClientErrorException ex = new ImClientErrorException(error);
        throw handleImClientException(ex);
      }

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

    List<CloudProviderEndpoint> cloudProviderEndpoints = deployment.getCloudProviderEndpoint()
        .getAllCloudProviderEndpoint();

    // for each URL get the tosca Node Name about the VM
    Multimap<String, String> vmMap = HashMultimap.create();
    Map<String, VirtualMachineInfo> vmMapInfo = new HashMap<>();
    for (String vmId : infrastructureState.getVmStates().keySet()) {
      VirtualMachineInfo vmInfo = executeWithClientForResult(cloudProviderEndpoints, requestedWithToken,
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
          .ifPresent(toscaNodeName -> {
            vmMap.put(toscaNodeName, vmId);
            vmMapInfo.put(vmId, vmInfo);
          });
    }

    Map<Boolean, Set<Resource>> resources = resourceRepository
        .findByDeployment_id(deployment.getId())
        .stream()
        .collect(Collectors.partitioningBy(resource -> resource.getIaasId() != null,
            Collectors.toSet()));

    for (Resource bindedResource : resources.get(true)) {
      boolean vmIsPresent = vmMap.get(bindedResource.getToscaNodeName()).remove(bindedResource.getIaasId());
      if (!vmIsPresent) {
        if (bindedResource.getState() != NodeStates.DELETING) {
          // the node isn't supposed to be deleted -> put it again in the pool of bindable
          // resources
          // TODO maybe throw an error? Eventual consistency (for update) should already
          // have been
          // handled
          LOG.warn("Resource <{}> in status {} unbinded from VM <{}> which doesn't exist anymore.",
              bindedResource.getId(), bindedResource.getState(), bindedResource.getIaasId());
          bindedResource.setIaasId(null);
          bindedResource.setCloudProviderEndpoint(null);
          resources.get(false).add(bindedResource);
        }
      } else {
        if (vmMapInfo.containsKey(bindedResource.getIaasId())) {
          VirtualMachineInfo vmInfo = vmMapInfo.get(bindedResource.getIaasId());
          writeVmInfoToResource(bindedResource, vmInfo);
        }
      }
    }

    for (Resource resource : resources.get(false)) {
      Collection<String> vmIds = vmMap.get(resource.getToscaNodeName());
      vmIds.stream().findAny().ifPresent(vmId -> {
        resource.setIaasId(vmId);
        writeVmInfoToResource(resource, vmMapInfo.get(vmId));
        vmIds.remove(vmId);
      });
    }
    if (!vmMap.isEmpty()) {
      LOG.warn("Some VMs of infrastructure <{}> couldn't be binded to a resource: {}",
          infrastructureId, vmMap.entries());
    }
  }

  private void writeVmInfoToResource(Resource bindedResource,
      VirtualMachineInfo vmInfo) {
    Map<String, String> resourceMetadata = bindedResource.getMetadata();
    if (resourceMetadata == null) {
      resourceMetadata = new HashMap<>();
      bindedResource.setMetadata(resourceMetadata);
    }
    VirtualMachineInfo vmOldInfo = null;
    if (resourceMetadata.containsKey(VMINFO)) {
      try {
        vmOldInfo = new ObjectMapper().readValue(resourceMetadata.get(VMINFO),
            VirtualMachineInfo.class);
      } catch (IOException e) {
        throw new DeploymentException("Error deserializing VM Info", e);
      }
    }
    if (vmOldInfo == null || !vmInfo.equals(vmOldInfo)) {
      try {
        resourceMetadata.put(VMINFO,
            new ObjectMapper().writeValueAsString(vmInfo));
      } catch (IOException e) {
        throw new DeploymentException("Error serializing VM Info", e);
      }
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
    // // WARNING: In IM we don't have the resource mapping yet, so we update all
    // the resources
    // // FIXME Remove once IM handles single nodes state update!!!! And pay
    // attention to the
    // // AbstractDeploymentProviderService.updateOnError method!
    super.updateResources(deployment, status);
  }

  @Override
  public boolean doAction(ActionMessage deploymentMessage) {
    Deployment deployment = getDeployment(deploymentMessage);

    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();

    String accessToken = null;
    if (oidcProperties.isEnabled()) {
      accessToken = oauth2TokenService.getAccessToken(requestedWithToken);
    }

    Resource resource = resourceRepository.findByIdAndDeployment_id(
        deploymentMessage.getResourceId(),
        deploymentMessage.getDeploymentId()).orElseThrow(
            () -> new IllegalArgumentException(
                String.format("Resource <%s> in deployment <%s> not found",
                    deploymentMessage.getResourceId(), deploymentMessage.getDeploymentId())));

    List<CloudProviderEndpoint> cloudProviderEndpoints = deployment.getCloudProviderEndpoint()
        .getAllCloudProviderEndpoint();

    // Execute action on VM through IM
    String action = deploymentMessage.getAction();
    try {
      switch (action) {
        case "start":
          resource.setState(NodeStates.STARTING);
          LOG.info("Starting VM of deployment <{}>", deploymentMessage.getDeploymentId());
          executeWithClient(cloudProviderEndpoints, requestedWithToken,
              client -> client.startVm(deployment.getEndpoint(), resource.getIaasId()));
          break;
        case "stop":
          resource.setState(NodeStates.STOPPING);
          LOG.info("Stopping VM of deployment <{}>", deploymentMessage.getDeploymentId());
          executeWithClient(cloudProviderEndpoints, requestedWithToken,
              client -> client.stopVm(deployment.getEndpoint(), resource.getIaasId()));
          break;
        default:
          throw new IllegalArgumentException("Invalid action " + action);
      }
    } catch (ImClientException ex) {
      throw handleImClientException(ex);
    }
    resourceRepository.save(resource);
    return true;
  }

  @Override
  public boolean isActionComplete(ActionMessage deploymentMessage) {

    Deployment deployment = getDeployment(deploymentMessage);

    Resource resource = resourceRepository.findByIdAndDeployment_id(
        deploymentMessage.getResourceId(),
        deploymentMessage.getDeploymentId()).orElseThrow(
            () -> new IllegalArgumentException(
                String.format("Resource <%s> in deployment <%s> not found",
                    deploymentMessage.getResourceId(),
                    deploymentMessage.getDeploymentId())));

    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();

    List<CloudProviderEndpoint> cloudProviderEndpoints = deployment.getCloudProviderEndpoint()
        .getAllCloudProviderEndpoint();

    try {

      VirtualMachineInfo vmInfo = executeWithClientForResult(cloudProviderEndpoints, requestedWithToken,
          client -> client.getVmInfo(deployment.getEndpoint(), resource.getIaasId()));

      String state = (String) vmInfo.getVmProperties()
          .stream()
          .filter(Objects::nonNull)
          .filter(properties -> "system".equals(properties.get("class")))
          .map(properties -> properties.get("state")).findAny().get();

      String action = deploymentMessage.getAction();
      boolean complete = false;

      switch (action) {
        case "start":
          if (state.equals("configured")) {
            resource.setState(NodeStates.STARTED);
            complete = true;
          }
          break;
        case "stop":
          if (state.equals("stopped")) {
            resource.setState(NodeStates.STOPPED);
            complete = true;
          }
          break;
        default:
      }
      // update the metadata attached to the resource
      writeVmInfoToResource(resource, vmInfo);
      resourceRepository.save(resource);
      return complete;

    } catch (ImClientException exception) {
      throw handleImClientException(exception);
    }
  }

  @Override
  public void validateAction(ActionMessage deploymentMessage) {
    Resource resource = resourceRepository.findByIdAndDeployment_id(
        deploymentMessage.getResourceId(),
        deploymentMessage.getDeploymentId()).orElseThrow(
            () -> new IllegalArgumentException(
                String.format("Resource <%s> in deployment <%s> not found",
                    deploymentMessage.getResourceId(), deploymentMessage.getDeploymentId())));

    if (!toscaService.isOfToscaType(resource, ToscaConstants.Nodes.Types.COMPUTE)) {
      throw new BadRequestException("Actions not supported on node of type "
          + resource.getToscaNodeType().toString());
    }

    String action = deploymentMessage.getAction();
    switch (action) {
      case "start":
        LOG.debug("Validating request to start VM of deployment <{}>",
            deploymentMessage.getDeploymentId());
        if (!resource.getState().equals(NodeStates.STOPPED)) {
          throw new BadRequestException("Cannot start node in state "
              + resource.getState().toString());
        }
        break;
      case "stop":
        LOG.info("Validating request to stio VM of deployment <{}>",
            deploymentMessage.getDeploymentId());
        if (!resource.getState().equals(NodeStates.STARTED)) {
          throw new BadRequestException("Cannot stop node in state "
              + resource.getState().toString());
        }
        break;
      default:
        throw new IllegalArgumentException("Invalid action " + action);
    }
  }
}
