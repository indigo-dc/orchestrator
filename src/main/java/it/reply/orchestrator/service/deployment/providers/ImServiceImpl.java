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

package it.reply.orchestrator.service.deployment.providers;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.MoreCollectors;
import com.google.common.collect.Multimap;

import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.tosca.model.ArchiveRoot;

import es.upv.i3m.grycap.im.InfrastructureManager;
import es.upv.i3m.grycap.im.auth.credentials.providers.AmazonEc2Credentials;
import es.upv.i3m.grycap.im.auth.credentials.providers.ImCredentials;
import es.upv.i3m.grycap.im.auth.credentials.providers.OpenNebulaCredentials;
import es.upv.i3m.grycap.im.auth.credentials.providers.OpenStackAuthVersion;
import es.upv.i3m.grycap.im.auth.credentials.providers.OpenStackCredentials;
import es.upv.i3m.grycap.im.exceptions.ImClientErrorException;
import es.upv.i3m.grycap.im.exceptions.ImClientException;
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
import it.reply.orchestrator.dto.CloudProviderEndpoint.IaaSType;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.ToscaService;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.orchestrator.utils.CommonUtils;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@DeploymentProviderQualifier(DeploymentProvider.IM)
@EnableConfigurationProperties(ImProperties.class)
@Slf4j
public class ImServiceImpl extends AbstractDeploymentProviderService {

  private static final Pattern OS_ENDPOINT_PATTERN =
      Pattern.compile("(https?:\\/\\/[^\\/]+)(?:\\/(?:([^\\/]+)\\/?)?)?");

  @Autowired
  private ToscaService toscaService;

  @Autowired
  private ResourceRepository resourceRepository;

  @Autowired
  private OidcProperties oidcProperties;

  @Autowired
  private ImProperties imProperties;

  @Autowired
  private OAuth2TokenService oauth2TokenService;

  private String getAccessToken(@NonNull OidcTokenId id) {
    return oauth2TokenService.getAccessToken(id, OAuth2TokenService.REQUIRED_SCOPES);
  }

  protected OpenStackCredentials getOpenStackAuthHeader(CloudProviderEndpoint cloudProviderEndpoint,
      @NonNull OidcTokenId requestedWithToken) {
    String endpoint = cloudProviderEndpoint.getCpEndpoint();
    Matcher matcher = OS_ENDPOINT_PATTERN.matcher(endpoint);
    if (!matcher.matches()) {
      throw new DeploymentException("Wrong OS endpoint format: " + endpoint);
    } else {
      endpoint = matcher.group(1);
      String accessToken = getAccessToken(requestedWithToken);
      OpenStackCredentials cred = cloudProviderEndpoint
          .getIaasHeaderId()
          .map(OpenStackCredentials::buildCredentials)
          .orElseGet(OpenStackCredentials::buildCredentials)
          .withTenant("oidc")
          .withUsername("indigo-dc")
          .withPassword(accessToken)
          .withHost(endpoint);
      if (Strings.isNullOrEmpty(matcher.group(2)) || "v3".equals(matcher.group(2))) {
        // if no API version is specified or V3 is specified -> 3.x_oidc_access_token
        cred.withAuthVersion(OpenStackAuthVersion.PASSWORD_3_X_TOKEN);
      }
      return cred;
    }
  }

  protected OpenNebulaCredentials getOpenNebulaAuthHeader(
      CloudProviderEndpoint cloudProviderEndpoint, @NonNull OidcTokenId requestedWithToken) {
    String accessToken = getAccessToken(requestedWithToken);
    return cloudProviderEndpoint
        .getIaasHeaderId()
        .map(OpenNebulaCredentials::buildCredentials)
        .orElseGet(OpenNebulaCredentials::buildCredentials)
        .withHost(cloudProviderEndpoint.getCpEndpoint())
        .withToken(accessToken);
  }

  protected AmazonEc2Credentials getAwsAuthHeader(CloudProviderEndpoint cloudProviderEndpoint) {
    return cloudProviderEndpoint
        .getIaasHeaderId()
        .map(AmazonEc2Credentials::buildCredentials)
        .orElseGet(AmazonEc2Credentials::buildCredentials)
        .withUsername(cloudProviderEndpoint.getUsername())
        .withPassword(cloudProviderEndpoint.getPassword());
  }

  protected String getImAuthHeader(@Nullable OidcTokenId requestedWithToken) {
    if (oidcProperties.isEnabled()) {
      String accessToken = getAccessToken(CommonUtils.checkNotNull(requestedWithToken));
      String header = ImCredentials.buildCredentials().withToken(accessToken).serialize();
      LOG.debug("IM authorization header built from access token");
      return header;
    } else {
      String header = imProperties
          .getImAuthHeader()
          .orElseThrow(() -> new OrchestratorException(
              "No Authentication info provided for for Infrastructure Manager "
                  + "and OAuth2 authentication is disabled"));
      LOG.debug("IM authorization header retrieved from properties file");
      return header;
    }
  }

  @Deprecated
  private String handleOtcHeader(CloudProviderEndpoint cloudProviderEndpoint, String iaasHeader) {
    final String iaasHeaderToReturn;
    if (cloudProviderEndpoint.getCpEndpoint() != null
        && cloudProviderEndpoint.getCpEndpoint().contains("otc.t-systems.com")) {
      String username = cloudProviderEndpoint.getUsername();
      String password = cloudProviderEndpoint.getPassword();
      Pattern pattern = Pattern.compile("\\s*(\\w+)\\s+(\\w+)\\s*");
      Matcher matcher = pattern.matcher(username);
      if (matcher.matches()) {
        String otcUsername = Preconditions.checkNotNull(matcher.group(1),
            "No vaild username provided for Open Telekom Cloud");
        String otcDomain = Preconditions.checkNotNull(matcher.group(2),
            "No vaild username provided for Open Telekom Cloud");
        if (otcUsername.matches("[0-9]+")) {
          // old style username, it must keep the domain too
          otcUsername = username;
        }
        iaasHeaderToReturn =
            iaasHeader
                .replaceFirst(Matcher.quoteReplacement("<USERNAME>"), otcUsername)
                .replaceFirst(Matcher.quoteReplacement("<PASSWORD>"), password)
                .replaceFirst(Matcher.quoteReplacement("<TENANT>"), otcDomain);
        LOG.info("Placed OTC credentials in auth header");
      } else {
        throw new DeploymentException("No vaild credentials provided for Open Telekom Cloud");
      }
    } else {
      // do nothing, no a OTC service
      iaasHeaderToReturn = iaasHeader;
    }
    return iaasHeaderToReturn;
  }

  protected InfrastructureManager getClient(List<CloudProviderEndpoint> cloudProviderEndpoints,
      @Nullable OidcTokenId requestedWithToken) {
    String imAuthHeader = getImAuthHeader(requestedWithToken);
    String iaasHeaders = cloudProviderEndpoints
        .stream()
        .map(cloudProviderEndpoint -> getIaasAuthHeader(cloudProviderEndpoint, requestedWithToken))
        .collect(Collectors.joining("\\n"));
    return getIm(cloudProviderEndpoints, imAuthHeader, iaasHeaders);
  }

  private String getIaasAuthHeader(CloudProviderEndpoint cloudProviderEndpoint,
      @Nullable OidcTokenId requestedWithToken) {
    IaaSType iaasType = cloudProviderEndpoint.getIaasType();
    LOG.debug("Generating {} credentials with: {}", iaasType, cloudProviderEndpoint);
    String computeServiceId = cloudProviderEndpoint.getCpComputeServiceId();
    Optional<String> iaasHeaderInProperties = imProperties.getIaasHeader(computeServiceId);
    String iaasHeader;
    if (iaasHeaderInProperties.isPresent()) {
      String iaasHeaderFromProperties = iaasHeaderInProperties.get();
      // substitute id with the subdeployment one (if present)
      iaasHeader = cloudProviderEndpoint
          .getIaasHeaderId()
          .map(subDeploymentId -> iaasHeaderFromProperties.replaceFirst(
              "(.*(?:^|\\s+|;)(?:;\\s*)?id\\s*=\\s*)(\\w+)(.*)", "$1" + subDeploymentId + "$3"))
          .orElse(iaasHeaderFromProperties);
      LOG.debug("IaaS authorization header for compute service " + computeServiceId
          + " retrieved from properties file");
      iaasHeader = handleOtcHeader(cloudProviderEndpoint, iaasHeader);
    } else {
      oidcProperties.runIfSecurityDisabled(() -> {
        throw new OrchestratorException("No Authentication info provided for compute service "
            + computeServiceId + " and OAuth2 authentication is disabled");
      });
      switch (iaasType) {
        case OPENSTACK:
          iaasHeader = getOpenStackAuthHeader(cloudProviderEndpoint,
              CommonUtils.checkNotNull(requestedWithToken)).serialize();
          break;
        case OPENNEBULA:
          iaasHeader = getOpenNebulaAuthHeader(cloudProviderEndpoint,
              CommonUtils.checkNotNull(requestedWithToken)).serialize();
          break;
        case AWS:
          iaasHeader = getAwsAuthHeader(cloudProviderEndpoint).serialize();
          break;
        default:
          throw new IllegalArgumentException(
              String.format("Unsupported provider type <%s>", iaasType));
      }
    }
    return iaasHeader;
  }

  @Deprecated
  @SneakyThrows(ImClientException.class)
  private InfrastructureManager getIm(List<CloudProviderEndpoint> cloudProviderEndpoints,
      String imAuthHeader, String iaasHeaders) {
    final String imUrl;
    if (cloudProviderEndpoints.size() != 1) {
      // multiple endpoints (or no endpoint for some reason) -> return PaaS level IM instance by
      // default
      imUrl = imProperties.getUrl();
    } else {
      imUrl = Optional
          .ofNullable(cloudProviderEndpoints.get(0).getImEndpoint())
          .orElseGet(imProperties::getUrl);
    }
    String imHeader = String.format("%s\\n%s", imAuthHeader, iaasHeaders);
    LOG.trace("IM auth header: {}", imHeader);
    return new InfrastructureManager(imUrl, imHeader);
  }

  @FunctionalInterface
  public interface ThrowingFunction<T, R, E extends Exception> {
    R apply(T param) throws E;

  }

  @FunctionalInterface
  public interface ThrowingConsumer<T, E extends Exception> {
    void accept(T param) throws E;

    /**
     * Converts the ThrowingConsumer in a ThrowingFunction.
     * 
     * @return a ThrowingFunction returning always null
     */
    default ThrowingFunction<T, Void, E> asFunction() {
      return (T param) -> {
        accept(param);
        return (Void) null;
      };
    }
  }

  @FunctionalInterface
  public interface ThrowingSupplier<T, E extends Exception> {
    T get() throws E;

    default <C> ThrowingFunction<C, T, E> asFunction() {
      return (C ignored) -> get();
    }
  }

  protected <R> R executeWithClient(CloudProviderEndpoint cloudProviderEndpoint,
      @Nullable OidcTokenId requestedWithToken,
      ThrowingFunction<InfrastructureManager, R, ImClientException> function)
      throws ImClientException {
    return this.executeWithClient(Lists.newArrayList(cloudProviderEndpoint), requestedWithToken,
        function);
  }

  protected <R> R executeWithClient(List<CloudProviderEndpoint> cloudProviderEndpoints,
      @Nullable OidcTokenId requestedWithToken,
      ThrowingFunction<InfrastructureManager, R, ImClientException> function)
      throws ImClientException {
    InfrastructureManager client = getClient(cloudProviderEndpoints, requestedWithToken);
    try {
      return function.apply(client);
    } catch (ImClientErrorException ex) {
      if (oidcProperties.isEnabled() && Optional
          .ofNullable(ex.getResponseError())
          .map(ResponseError::getCode)
          .filter(code -> code.equals(HttpStatus.UNAUTHORIZED.value()))
          .isPresent()) {
        oauth2TokenService.getRefreshedAccessToken(requestedWithToken,
            OAuth2TokenService.REQUIRED_SCOPES);
        client = getClient(cloudProviderEndpoints, requestedWithToken);
        return function.apply(client);
      } else {
        throw ex;
      }
    }
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

    ArchiveRoot ar =
        toscaService.prepareTemplate(deployment.getTemplate(), deployment.getParameters());

    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
    final CloudProviderEndpoint chosenCloudProviderEndpoint =
        deploymentMessage.getChosenCloudProviderEndpoint();

    String accessToken = null;
    if (oidcProperties.isEnabled()) {
      accessToken = getAccessToken(CommonUtils.checkNotNull(requestedWithToken));
    }
    toscaService.addElasticClusterParameters(ar, deployment.getId(), accessToken);
    toscaService.contextualizeAndReplaceImages(ar, deploymentMessage.getChosenCloudProvider(),
        chosenCloudProviderEndpoint.getCpComputeServiceId(), DeploymentProvider.IM);
    String imCustomizedTemplate = toscaService.getTemplateFromTopology(ar);

    // Deploy on IM
    try {
      String infrastructureId = executeWithClient(chosenCloudProviderEndpoint, requestedWithToken,
          client -> client.createInfrastructure(imCustomizedTemplate, BodyContentType.TOSCA))
              .getInfrastructureId();
      LOG.info("InfrastructureId for deployment <{}> is: {}", deploymentMessage.getDeploymentId(),
          infrastructureId);
      deployment.setEndpoint(infrastructureId);
    } catch (ImClientException ex) {
      throw handleImClientException(ex);
    }
    return true;
  }

  @Override
  public boolean isDeployed(DeploymentMessage deploymentMessage) throws DeploymentException {
    Deployment deployment = getDeployment(deploymentMessage);

    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();

    List<CloudProviderEndpoint> cloudProviderEndpoints =
        getEndpointsList(deployment.getResources(), deployment.getCloudProviderEndpoint());

    try {

      InfrastructureState infrastructureState = executeWithClient(cloudProviderEndpoints,
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
          if (additionalErrorInfo.isPresent()) {
            sb.append("\n").append(additionalErrorInfo.get());
          }
          throw new DeploymentException(sb.toString());
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
        getEndpointsList(deployment.getResources(), deployment.getCloudProviderEndpoint());

    // Try to get the logs of the virtual infrastructure for debug
    // purpose.
    try {
      Property contMsg = executeWithClient(cloudProviderEndpoints, requestedWithToken,
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
    try {
      deployment
          .setOutputs(executeWithClient(deployment.getCloudProviderEndpoint(), requestedWithToken,
              client -> client.getInfrastructureOutputs(deployment.getEndpoint()))
                  .getOutputs());
    } catch (ImClientException exception) {
      throw handleImClientException(exception);
    }
    updateOnSuccess(deployment.getId());
  }

  private List<CloudProviderEndpoint> getEndpointsList(Collection<Resource> resources,
      CloudProviderEndpoint... cloudProviderEndpoints) {
    Stream<Resource> resourcesStream = CommonUtils.iteratorToStream(resources.iterator());

    Stream<CloudProviderEndpoint> cloudProviderEndpointStream = Stream
        .concat(Stream.of(cloudProviderEndpoints),
            resourcesStream.map(Resource::getCloudProviderEndpoint))
        .filter(Objects::nonNull);

    return CommonUtils
        .distinct(cloudProviderEndpointStream, cloudProviderEndpoint -> cloudProviderEndpoint
            .getIaasHeaderId()
            .orElse(null))
        .collect(Collectors.toList());
  }

  @Override
  public boolean doUpdate(DeploymentMessage deploymentMessage, String template) {

    Deployment deployment = getDeployment(deploymentMessage);

    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
    final CloudProviderEndpoint chosenCloudProviderEndpoint =
        deploymentMessage.getChosenCloudProviderEndpoint();

    ArchiveRoot newAr = toscaService.prepareTemplate(template, deployment.getParameters());

    String accessToken = null;
    if (oidcProperties.isEnabled()) {
      accessToken = getAccessToken(CommonUtils.checkNotNull(requestedWithToken));
    }
    toscaService.addElasticClusterParameters(newAr, deployment.getId(), accessToken);

    updateResources(deployment, deployment.getStatus());

    // List of resources to be removed
    Set<Resource> resourcesToRemove = new HashSet<>();

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
            int newCount = toscaService.getCount(newNode).orElse(1);
            List<Resource> remainingResources = resources
                .stream()
                .filter(resource -> resource.getState() != NodeStates.DELETING)
                .collect(Collectors.toList());

            for (int i = 0; i < remainingResources.size() - newCount; ++i) {
              Resource resource = remainingResources.get(i);
              resource.setState(NodeStates.DELETING);
              resourcesToRemove.add(resource);
            }
          }
        });

    boolean newResourcesOnDifferentService = !chosenCloudProviderEndpoint
        .getCpComputeServiceId()
        .equals(deployment.getCloudProviderEndpoint().getCpComputeServiceId());

    newNodes.forEach((name, newNode) -> {

      List<Resource> resources = deployment
          .getResources()
          .stream()
          .filter(resource -> name.equals(resource.getToscaNodeName())
              && newNode.getType().equals(resource.getToscaNodeType())
              && resource.getState() != NodeStates.DELETING)
          .collect(Collectors.toList());

      int newCount = toscaService.getCount(newNode).orElse(1);
      int oldCount = resources.size();
      int diff = newCount - oldCount;
      for (int i = 0; i < diff; i++) {
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
      if (toscaService.isScalable(newNode) && newResourcesOnDifferentService) {
        setHybridNetworkingProperties(newNode);
      }
    });

    toscaService.contextualizeAndReplaceImages(newAr,
        deploymentMessage.getChosenCloudProvider(),
        chosenCloudProviderEndpoint.getCpComputeServiceId(), DeploymentProvider.IM);

    // FIXME: There's not check if the Template actually changed!
    deployment.setTemplate(toscaService.updateTemplate(template));

    List<String> vmIds = resourcesToRemove
        .stream()
        .map(Resource::getIaasId)
        .collect(Collectors.toList());

    try {
      if (!vmIds.isEmpty()) {
        List<CloudProviderEndpoint> cloudProviderEndpoints =
            getEndpointsList(resourcesToRemove, deployment.getCloudProviderEndpoint());

        LOG.debug("Deleting VMs {}", vmIds);

        executeWithClient(cloudProviderEndpoints, requestedWithToken, client -> {
          client.removeResource(deployment.getEndpoint(), vmIds);
          return true;
        });
      }

      String templateToDeploy = toscaService.getTemplateFromTopology(newAr);
      LOG.debug("Template sent: \n{}", templateToDeploy);

      List<CloudProviderEndpoint> cloudProviderEndpoints =
          getEndpointsList(deployment.getResources(), chosenCloudProviderEndpoint,
              deployment.getCloudProviderEndpoint());

      executeWithClient(cloudProviderEndpoints,
          requestedWithToken, client -> client.addResource(deployment.getEndpoint(),
              templateToDeploy, BodyContentType.TOSCA));
    } catch (ImClientException exception) {
      throw handleImClientException(exception);
    }
    return true;
  }

  private void setHybridNetworkingProperties(NodeTemplate node) {
    Map<String, Capability> capabilities =
        Optional.ofNullable(node.getCapabilities()).orElseGet(() -> {
          node.setCapabilities(new HashMap<>());
          return node.getCapabilities();
        });
    // The node doesn't have an OS Capability -> need to add a dummy one to hold a
    // random image for underlying deployment systems
    Capability endpointCapability = capabilities.computeIfAbsent("endpoint", key -> {
      LOG.debug("Generating default endpoint capability for node <{}>", node.getName());
      Capability capability = new Capability();
      capability.setType("tosca.capabilities.indigo.Endpoint");
      return capability;
    });
    Map<String, AbstractPropertyValue> endpointCapabilityProperties =
        Optional.ofNullable(endpointCapability.getProperties()).orElseGet(() -> {
          endpointCapability.setProperties(new HashMap<>());
          return endpointCapability.getProperties();
        });
    ScalarPropertyValue scalarPropertyValue = new ScalarPropertyValue("PUBLIC");
    scalarPropertyValue.setPrintable(true);
    endpointCapabilityProperties.put("network_name", scalarPropertyValue);
    scalarPropertyValue = new ScalarPropertyValue("false");
    scalarPropertyValue.setPrintable(true);
    endpointCapabilityProperties.put("private_ip", scalarPropertyValue);
  }

  @Override
  public boolean doUndeploy(DeploymentMessage deploymentMessage) {
    Deployment deployment = getDeployment(deploymentMessage);

    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
    final CloudProviderEndpoint chosenCloudProviderEndpoint =
        deploymentMessage.getChosenCloudProviderEndpoint();

    String deploymentEndpoint = deployment.getEndpoint();
    if (deploymentEndpoint != null) {
      deployment.setTask(Task.DEPLOYER);

      List<CloudProviderEndpoint> cloudProviderEndpoints =
          getEndpointsList(deployment.getResources(), chosenCloudProviderEndpoint);

      try {
        executeWithClient(cloudProviderEndpoints, requestedWithToken, client -> {
          client.destroyInfrastructure(deploymentEndpoint);
          return true;
        });

      } catch (ImClientErrorException exception) {
        if (!getImResponseError(exception).is404Error()) {
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
        getEndpointsList(deployment.getResources(), deployment.getCloudProviderEndpoint());
    try {
      InfrastructureState infrastructureState =
          executeWithClient(cloudProviderEndpoints, requestedWithToken,
              client -> client.getInfrastructureState(deploymentEndpoint));

      LOG.debug(infrastructureState.getFormattedInfrastructureStateString());
    } catch (ImClientErrorException exception) {
      ResponseError error = getImResponseError(exception);
      if (error.is404Error()) {
        return true;
      } else {
        throw handleImClientException(exception);
      }
    } catch (ImClientException exception) {
      throw handleImClientException(exception);
    }
    return false;
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

    // for each URL get the tosca Node Name about the VM
    Multimap<String, String> vmMap = HashMultimap.create();
    for (String vmId : infrastructureState.getVmStates().keySet()) {
      VirtualMachineInfo vmInfo =
          executeWithClient(deployment.getCloudProviderEndpoint(), requestedWithToken,
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
        bindedResource.setId(null);
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

  private ResponseError getImResponseError(ImClientErrorException exception) {
    return exception.getResponseError();
  }

  private RuntimeException handleImClientException(ImClientException ex) {
    if (ex instanceof ImClientErrorException) {
      ResponseError responseError = getImResponseError((ImClientErrorException) ex);
      return new DeploymentException(responseError.getFormattedErrorMessage(), ex);
    } else {
      return new DeploymentException(ex);
    }
  }

  @Override
  protected void updateResources(Deployment deployment, Status status) {
    // // WARNING: In IM we don't have the resource mapping yet, so we update all the resources
    // // FIXME Remove once IM handles single nodes state update!!!! And pay attention to the
    // // AbstractDeploymentProviderService.updateOnError method!
    super.updateResources(deployment, status);
  }

}
