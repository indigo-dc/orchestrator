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
import com.google.common.collect.Lists;

import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingException;

import es.upv.i3m.grycap.im.InfrastructureManager;
import es.upv.i3m.grycap.im.States;
import es.upv.i3m.grycap.im.auth.credentials.providers.AmazonEc2Credentials;
import es.upv.i3m.grycap.im.auth.credentials.providers.ImCredentials;
import es.upv.i3m.grycap.im.auth.credentials.providers.OpenNebulaCredentials;
import es.upv.i3m.grycap.im.auth.credentials.providers.OpenStackCredentials;
import es.upv.i3m.grycap.im.auth.credentials.providers.OpenstackAuthVersion;
import es.upv.i3m.grycap.im.exceptions.ImClientErrorException;
import es.upv.i3m.grycap.im.exceptions.ImClientException;
import es.upv.i3m.grycap.im.pojo.InfOutputValues;
import es.upv.i3m.grycap.im.pojo.InfrastructureState;
import es.upv.i3m.grycap.im.pojo.InfrastructureUri;
import es.upv.i3m.grycap.im.pojo.InfrastructureUris;
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
import it.reply.orchestrator.dal.repository.DeploymentRepository;
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
import it.reply.orchestrator.exception.service.ToscaException;
import it.reply.orchestrator.service.ToscaService;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.orchestrator.utils.CommonUtils;

import lombok.extern.slf4j.Slf4j;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@DeploymentProviderQualifier(DeploymentProvider.IM)
@EnableConfigurationProperties(ImProperties.class)
@Slf4j
public class ImServiceImpl extends AbstractDeploymentProviderService {

  private static final Pattern VM_ID_PATTERN = Pattern.compile("(\\w+)$");
  private static final Pattern OS_ENDPOINT_PATTERN =
      Pattern.compile("(https?:\\/\\/[^\\/]*)\\/?([^\\/]*)");

  @Autowired
  private ToscaService toscaService;

  @Autowired
  private DeploymentRepository deploymentRepository;

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
      OpenStackCredentials cred = OpenStackCredentials
          .buildCredentials()
          .withTenant("oidc")
          .withUsername("indigo-dc")
          .withPassword(accessToken)
          .withHost(endpoint);
      if (matcher.groupCount() > 1 && "v3".equals(matcher.group(2))) {
        cred.withAuthVersion(OpenstackAuthVersion.PASSWORD_3_X);
      }
      return cred;
    }
  }

  protected OpenNebulaCredentials getOpenNebulaAuthHeader(
      CloudProviderEndpoint cloudProviderEndpoint, @NonNull OidcTokenId requestedWithToken) {
    String accessToken = getAccessToken(requestedWithToken);
    return OpenNebulaCredentials
        .buildCredentials()
        .withHost(cloudProviderEndpoint.getCpEndpoint())
        .withToken(accessToken);
  }

  protected AmazonEc2Credentials getAwsAuthHeader(CloudProviderEndpoint cloudProviderEndpoint) {
    return AmazonEc2Credentials
        .buildCredentials()
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

  protected InfrastructureManager getClient(CloudProviderEndpoint cloudProviderEndpoint,
      @Nullable OidcTokenId requestedWithToken) {
    return getClient(Lists.newArrayList(cloudProviderEndpoint), requestedWithToken);
  }

  protected InfrastructureManager getClient(List<CloudProviderEndpoint> cloudProviderEndpoints,
      @Nullable OidcTokenId requestedWithToken) {
    String imAuthHeader = getImAuthHeader(requestedWithToken);
    String iaasHeaders = cloudProviderEndpoints
        .stream()
        .map(cloudProviderEndpoint -> {
          IaaSType iaasType = cloudProviderEndpoint.getIaasType();
          LOG.debug("Generating {} credentials with: {}", iaasType, cloudProviderEndpoint);
          String computeServiceId = cloudProviderEndpoint.getCpComputeServiceId();
          Optional<String> iaasHeaderInProperties = imProperties.getIaasHeader(computeServiceId);
          String iaasHeader;
          if (iaasHeaderInProperties.isPresent()) {
            iaasHeader = iaasHeaderInProperties.get();
            LOG.debug("IaaS authorization header for IM retrieved from properties file");
            iaasHeader = handleOtcHeader(cloudProviderEndpoint, iaasHeader);
          } else {
            oidcProperties.runIfSecurityDisabled(() -> {
              throw new OrchestratorException("No Authentication info provided for compute service "
                  + computeServiceId + "  and OAuth2 authentication is disabled");
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
        })
        .collect(Collectors.joining("\\n"));

    try {
      String imUrl = getImUrl(cloudProviderEndpoints);
      String imHeader = String.format("%s\\n%s", imAuthHeader, iaasHeaders);
      LOG.debug("IM auth header: {}", imHeader);
      return new InfrastructureManager(imUrl, imHeader);
    } catch (ImClientException ex) {
      // TODO ask for this exception removal
      throw new OrchestratorException(ex);
    }
  }

  @Deprecated
  private String getImUrl(List<CloudProviderEndpoint> cloudProviderEndpoints) {
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
    return imUrl;
  }

  @FunctionalInterface
  public interface ThrowingFunction<T, R, E extends Exception> {
    R apply(T param) throws E;
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
    Deployment deployment = deploymentMessage.getDeployment();
    String deploymentUuid = deployment.getId();
    try {
      resourceRepository
          .findByDeployment_id(deployment.getId())
          .stream()
          .filter(resource -> resource.getState() == NodeStates.INITIAL)
          .forEach(resource -> resource.setState(NodeStates.CREATING));

      // Update status of the deployment
      deployment.setTask(Task.DEPLOYER);
      deployment = deploymentRepository.save(deployment);

      ArchiveRoot ar =
          toscaService.prepareTemplate(deployment.getTemplate(), deployment.getParameters());
      String accessToken = null;
      final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
      final CloudProviderEndpoint chosenCloudProviderEndpoint =
          deploymentMessage.getChosenCloudProviderEndpoint();
      if (oidcProperties.isEnabled()) {
        accessToken = getAccessToken(CommonUtils.checkNotNull(requestedWithToken));
      }
      toscaService.addElasticClusterParameters(ar, deploymentUuid, accessToken);
      toscaService.contextualizeAndReplaceImages(ar, deploymentMessage.getChosenCloudProvider(),
          chosenCloudProviderEndpoint.getCpComputeServiceId(), DeploymentProvider.IM);
      String imCustomizedTemplate = toscaService.getTemplateFromTopology(ar);

      // Deploy on IM
      InfrastructureUri infrastructureUri =
          executeWithClient(chosenCloudProviderEndpoint, requestedWithToken,
              client -> client.createInfrastructure(imCustomizedTemplate, BodyContentType.TOSCA));

      String infrastructureId = infrastructureUri.getInfrastructureId();
      if (infrastructureId != null) {
        deployment.setEndpoint(infrastructureId);
        deployment = deploymentRepository.save(deployment);
        deploymentMessage.setCreateComplete(true);
        return true;
      } else {
        updateOnError(deploymentUuid,
            String.format(
                "Creation of deployment <%s>: Couldn't extract infrastructureId from IM endpoint."
                    + "\nIM endpoint was %s.",
                deploymentUuid, infrastructureUri.getUri()));
        return false;
      }
      // Exception generated when the im produces an error message
    } catch (ImClientErrorException exception) {
      logImErrorResponse(exception);
      ResponseError responseError = getImResponseError(exception);
      updateOnError(deploymentUuid, responseError.getFormattedErrorMessage());
      return false;

    } catch (Exception ex) {
      LOG.error("Error deploying", ex);
      updateOnError(deploymentUuid, ex);
      return false;
    }
  }

  @Override
  public boolean isDeployed(DeploymentMessage deploymentMessage) throws DeploymentException {
    Deployment deployment = deploymentMessage.getDeployment();

    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
    final CloudProviderEndpoint chosenCloudProviderEndpoint =
        deploymentMessage.getChosenCloudProviderEndpoint();

    try {

      InfrastructureState infrastructureState = executeWithClient(chosenCloudProviderEndpoint,
          requestedWithToken, client -> client.getInfrastructureState(deployment.getEndpoint()));

      LOG.debug(infrastructureState.toString());

      States enumState = infrastructureState.getEnumState();
      switch (enumState) {
        case CONFIGURED:
          deploymentMessage.setPollComplete(true);
          return true;
        case FAILED:
        case UNCONFIGURED:
          StringBuilder errorMsg = new StringBuilder()
              .append("Fail to deploy deployment <")
              .append(deployment.getId())
              .append(">\nIM id is: <")
              .append(deployment.getEndpoint())
              .append(">\nIM response is: <")
              .append(infrastructureState.getFormattedInfrastructureStateString())
              .append(">");
          try {
            // Try to get the logs of the virtual infrastructure for debug
            // purpose.
            Property contMsg = executeWithClient(chosenCloudProviderEndpoint, requestedWithToken,
                client -> client.getInfrastructureContMsg(deployment.getEndpoint()));

            if (!Strings.isNullOrEmpty(contMsg.getValue())) {
              errorMsg.append("\nIM contMsg is: ").append(contMsg.getValue());
            }
          } catch (Exception ex) {
            // Do nothing
          }
          DeploymentException ex = new DeploymentException(errorMsg.toString());
          updateOnError(deployment.getId(), ex); // Set failure information in the deployment
          LOG.error(errorMsg.toString());
          throw ex;
        default:
          return false;
      }
    } catch (ImClientException exception) {
      String errorResponse = exception.getMessage();
      if (exception instanceof ImClientErrorException) {
        ImClientErrorException ex = (ImClientErrorException) exception;
        errorResponse = ex.getResponseError().getFormattedErrorMessage();
      }

      String errorMsg = String.format(
          "Fail to deploy deployment <%s>." + "\nIM id is: <%s>" + "\nIM error is: <%s>",
          deployment.getId(), deployment.getEndpoint(), errorResponse);
      try {
        // Try to get the logs of the virtual infrastructure for debug
        // purpose.
        Property contMsg = executeWithClient(chosenCloudProviderEndpoint, requestedWithToken,
            client -> client.getInfrastructureContMsg(deployment.getEndpoint()));
        errorMsg = errorMsg.concat("\nIM contMsg is: " + contMsg.getValue());
      } catch (Exception ex) {
        // Do nothing
      }
      // TODO: refactor this code and use a shared implementation for error handling and logging
      DeploymentException ex = new DeploymentException(errorMsg);
      updateOnError(deployment.getId(), ex); // Set failure information in the deployment
      LOG.error(errorMsg);
      throw ex;
    }
  }

  @Override
  public void finalizeDeploy(DeploymentMessage deploymentMessage, boolean deployed) {

    Deployment deployment = deploymentMessage.getDeployment();
    if (deployed) {
      try {

        final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
        final CloudProviderEndpoint chosenCloudProviderEndpoint =
            deploymentMessage.getChosenCloudProviderEndpoint();

        if (deployment.getOutputs().isEmpty()) {
          InfOutputValues outputValues =
              executeWithClient(chosenCloudProviderEndpoint, requestedWithToken,
                  client -> client.getInfrastructureOutputs(deployment.getEndpoint()));
          deployment.setOutputs(outputValues.getOutputs());
        }
        bindResources(deploymentMessage);

        updateOnSuccess(deployment.getId());

      } catch (ImClientErrorException exception) {
        logImErrorResponse(exception);
        updateOnError(deployment.getId(), exception);

      } catch (Exception ex) {
        LOG.error("Error finalizing deployment", ex);
        updateOnError(deployment.getId(), ex);
      }
    } else {
      updateOnError(deployment.getId());
    }
  }

  @Override
  public boolean doUpdate(DeploymentMessage deploymentMessage, String template) {

    Deployment deployment = deploymentMessage.getDeployment();

    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
    final CloudProviderEndpoint chosenCloudProviderEndpoint =
        deploymentMessage.getChosenCloudProviderEndpoint();

    // Check if count is increased or if there is a removal list, other kinds of update are
    // discarded

    ArchiveRoot oldAr;
    ArchiveRoot newAr;
    try {
      // FIXME Fugly

      // Get TOSCA in-memory repr. of current template
      oldAr = toscaService.prepareTemplate(deployment.getTemplate(), deployment.getParameters());

      // Get TOSCA in-memory repr. of new template
      newAr = toscaService.prepareTemplate(template, deployment.getParameters());

      String accessToken = null;
      if (oidcProperties.isEnabled()) {
        accessToken = getAccessToken(CommonUtils.checkNotNull(requestedWithToken));
      }
      toscaService.addElasticClusterParameters(newAr, deployment.getId(), accessToken);

      toscaService.contextualizeAndReplaceImages(newAr, deploymentMessage.getChosenCloudProvider(),
          chosenCloudProviderEndpoint.getCpComputeServiceId(), DeploymentProvider.IM);
    } catch (ParsingException | IOException | ToscaException ex) {
      throw new OrchestratorException(ex);
    }
    // find Count nodes into new and old template
    Map<String, NodeTemplate> oldNodes = toscaService
        .getScalableNodes(oldAr)
        .stream()
        .collect(Collectors.toMap(node -> node.getName(), node -> node));
    Map<String, NodeTemplate> newNodes = toscaService
        .getScalableNodes(newAr)
        .stream()
        .collect(Collectors.toMap(node -> node.getName(), node -> node));

    try {
      // Create the new template with the nodes to be added
      ArchiveRoot root = newAr;
      Map<String, NodeTemplate> nodes = new HashMap<>();

      // List of vmIds to be removed
      List<String> vmIds = new ArrayList<String>();

      // Find difference between the old template and the new
      for (Map.Entry<String, NodeTemplate> entry : oldNodes.entrySet()) {
        if (newNodes.containsKey(entry.getKey())) {
          int oldCount = toscaService.getCount(entry.getValue()).orElse(-1);
          int newCount = toscaService.getCount(newNodes.get(entry.getKey())).orElse(-1);
          List<String> removalList = toscaService.getRemovalList(newNodes.get(entry.getKey()));
          if (newCount > oldCount && removalList.size() == 0) {
            Resource resource;
            for (int i = 0; i < (newCount - oldCount); i++) {
              resource = new Resource();
              resource.setDeployment(deployment);
              resource.setState(NodeStates.CREATING);
              resource.setToscaNodeName(entry.getKey());
              resource.setToscaNodeType(entry.getValue().getType());
              resourceRepository.save(resource);
            }
            nodes.put(entry.getKey(), newNodes.get(entry.getKey()));

          } else if (newCount < oldCount && removalList.size() == (oldCount - newCount)) {
            // delete a WN.

            // Find the nodes to be removed.
            for (String resourceId : removalList) {
              Resource resource = resourceRepository.findOne(resourceId);
              resource.setState(NodeStates.DELETING);
              resource = resourceRepository.save(resource);
              vmIds.add(resource.getIaasId());
            }
          } else if (newCount == oldCount && removalList.size() == 0) {
            // do nothing
          } else {
            throw new DeploymentException("An error occur during the update. Count is <" + newCount
                + "> but removal_list contains <" + removalList.size() + "> elements in the node: "
                + entry.getKey());
          }
        }
      }

      // Find if there is a new TOSCA node
      for (Map.Entry<String, NodeTemplate> entry : newNodes.entrySet()) {
        if (!oldNodes.containsKey(entry.getKey())) {
          int count = toscaService.getCount(newNodes.get(entry.getKey())).orElse(-1);
          Resource resource;
          for (int i = 0; i < count; i++) {
            resource = new Resource();
            resource.setDeployment(deployment);
            resource.setState(NodeStates.CREATING);
            resource.setToscaNodeName(entry.getKey());
            resource.setToscaNodeType(entry.getValue().getType());
            resourceRepository.save(resource);
          }
          nodes.put(entry.getKey(), newNodes.get(entry.getKey()));
        }
      }

      // Pulisco gli output e aggiungo i nodi da creare
      root.getTopology().setOutputs(null);
      root.getTopology().setNodeTemplates(nodes);
      if (!root.getTopology().isEmpty()) {
        try {
          String templateToDeploy = toscaService.getTemplateFromTopology(root);
          executeWithClient(chosenCloudProviderEndpoint, requestedWithToken, client -> client
              .addResource(deployment.getEndpoint(), templateToDeploy, BodyContentType.TOSCA));
        } catch (ImClientErrorException exception) {
          throw new DeploymentException(
              String.format("An error occur during the update: fail to add new resources.%n%s",
                  getImResponseError(exception).getFormattedErrorMessage()),
              exception);
        }
      }
      // DELETE
      if (vmIds.size() > 0) {
        try {
          executeWithClient(chosenCloudProviderEndpoint, requestedWithToken, client -> {
            client.removeResource(deployment.getEndpoint(), vmIds);
            return true;
          });
        } catch (ImClientErrorException exception) {
          throw new DeploymentException(
              String.format("An error occur during the update: fail to delete resources.%n%s",
                  getImResponseError(exception).getFormattedErrorMessage()),
              exception);
        }
      }
      // FIXME: There's not check if the Template actually changed!
      deployment.setTemplate(toscaService.updateTemplate(template));
      return true;
    } catch (ImClientException | IOException | DeploymentException ex) {
      LOG.error("Error updating", ex);
      updateOnError(deployment.getId(), ex);
      return false;
    }
  }

  @Override
  public boolean doUndeploy(DeploymentMessage deploymentMessage) {
    Deployment deployment = deploymentMessage.getDeployment();
    String deploymentUuid = deployment.getId();

    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
    final CloudProviderEndpoint chosenCloudProviderEndpoint =
        deploymentMessage.getChosenCloudProviderEndpoint();

    try {
      // Update status of the deployment
      deployment.setTask(Task.DEPLOYER);
      deployment = deploymentRepository.save(deployment);
      String deploymentEndpoint = deployment.getEndpoint();

      if (deploymentEndpoint == null) {
        // updateOnSuccess(deploymentUuid);
        deploymentMessage.setDeleteComplete(true);
        return true;
      }

      executeWithClient(chosenCloudProviderEndpoint, requestedWithToken, client -> {
        client.destroyInfrastructure(deploymentEndpoint);
        return true;
      });
      deploymentMessage.setDeleteComplete(true);
      return true;

    } catch (ImClientErrorException exception) {
      logImErrorResponse(exception);
      ResponseError error = getImResponseError(exception);
      if (error.is404Error()) {
        // updateOnSuccess(deploymentUuid);
        return true;

      } else {
        updateOnError(deploymentUuid, error.getFormattedErrorMessage());
        return false;
      }

    } catch (Exception ex) {
      LOG.error("Error undeploying", ex);
      updateOnError(deploymentUuid, ex);
      return false;
    }
  }

  @Override
  public boolean isUndeployed(DeploymentMessage deploymentMessage) {

    Deployment deployment = deploymentMessage.getDeployment();

    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
    final CloudProviderEndpoint chosenCloudProviderEndpoint =
        deploymentMessage.getChosenCloudProviderEndpoint();

    try {

      // TODO verificare
      if (deployment.getEndpoint() == null) {
        return true;
      }
      executeWithClient(chosenCloudProviderEndpoint, requestedWithToken,
          client -> client.getInfrastructureState(deployment.getEndpoint()));

      // If IM throws 404 the undeploy is complete
      // It is not, otherwise
      return false;

    } catch (ImClientErrorException exception) {
      ResponseError error = getImResponseError(exception);
      return error.is404Error();

    } catch (ImClientException ex) {
      // TODO improve exception handling
      LOG.error("Error checking for undeployment", ex);
      return false;
    }
  }

  /**
   * Check if a resource is deleted.
   */
  @Override
  public void finalizeUndeploy(DeploymentMessage deploymentMessage, boolean undeployed) {
    if (undeployed) {
      updateOnSuccess(deploymentMessage.getDeploymentId());
    } else {
      updateOnError(deploymentMessage.getDeploymentId());
    }
  }

  // private boolean isResourceDeleted(Resource resource) {
  // try {
  // Deployment deployment = resource.getDeployment();
  // // Generate IM Client
  // InfrastructureManager im = getClient(deploymentMessage);
  //
  // im.getVmInfo(deployment.getEndpoint(), resource.getIaasId());
  // return false;
  //
  // } catch (ImClientErrorException exception) {
  // ResponseError error = getImResponseError(exception);
  // return error.is404Error();
  //
  // } catch (ImClientException ex) {
  // // TODO improve exception handling
  // LOG.error(ex);
  // return false;
  // }
  // }

  /**
   * Match the {@link Resource} to IM vms.
   * 
   */
  private void bindResources(DeploymentMessage deploymentMessage) throws ImClientException {
    Deployment deployment = deploymentMessage.getDeployment();
    String infrastructureId = deployment.getEndpoint();

    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
    final CloudProviderEndpoint chosenCloudProviderEndpoint =
        deploymentMessage.getChosenCloudProviderEndpoint();

    // Get the URLs of the VMs composing the virtual infrastructure
    // TODO test in case of errors
    InfrastructureUris vmUrls = executeWithClient(chosenCloudProviderEndpoint, requestedWithToken,
        client -> client.getInfrastructureInfo(infrastructureId));

    // for each URL get the information about the VM
    Map<String, VirtualMachineInfo> vmMap = new HashMap<>();
    for (InfrastructureUri vmUri : vmUrls.getUris()) {
      String vmId = extractVmId(vmUri);
      VirtualMachineInfo vmInfo = executeWithClient(chosenCloudProviderEndpoint, requestedWithToken,
          client -> client.getVmInfo(infrastructureId, vmId));
      boolean added = vmMap.putIfAbsent(vmId, vmInfo) == null;
      if (!added) {
        throw new DeploymentException(
            String.format("Duplicated vm id %s found (vm id uri %s)", vmId, vmUri.getUri()));
      }
    }

    // Find the Resource from the DB and bind it with the corresponding VM
    List<Resource> resources = resourceRepository.findByDeployment_id(deployment.getId());

    // Remove from vmMap all the resources already binded
    for (Resource r : resources) {
      if (r.getIaasId() != null) {
        vmMap.remove(r.getIaasId());
      }
    }

    for (Resource r : resources) {
      switch (r.getState()) {
        case CONFIGURING:
        case CREATING:
        case INITIAL:
        case STARTING:
        case ERROR:
          Iterator<Entry<String, VirtualMachineInfo>> it = vmMap.entrySet().iterator();
          while (it.hasNext()) {
            Map.Entry<String, VirtualMachineInfo> entry = it.next();
            if (entry.getValue().toString().contains(r.getToscaNodeName())) {
              r.setIaasId(entry.getKey());
              it.remove();
              break;
            }
          }
          break;
        case DELETING:
          deployment.getResources().remove(r);
          break;
        case CONFIGURED:
        case CREATED:
        case STARTED:
        case STOPPING:
        default:
          break;

      }
    }
  }

  private String extractVmId(InfrastructureUri vmUri) {
    Matcher matcher = VM_ID_PATTERN.matcher(vmUri.getUri());
    if (matcher.find() && !Strings.isNullOrEmpty(matcher.group(0))) {
      return matcher.group(0);
    } else {
      throw new DeploymentException(
          String.format("Unable to retrieve VM id from uri %s", vmUri.getUri()));
    }
  }

  private ResponseError getImResponseError(ImClientErrorException exception) {
    return exception.getResponseError();
  }

  private void logImErrorResponse(ImClientErrorException exception) {
    LOG.error(exception.getResponseError().getFormattedErrorMessage());
  }

  // FIXME Remove once IM handles single nodes state update
  /**
   * Update the status of the deployment with an error message.
   * 
   * @param deploymentUuid
   *          the deployment id
   * @param message
   *          the error message
   */
  @Override
  public void updateOnError(String deploymentUuid, String message) {
    // WARNING: In IM we don't have the resource mapping yet, so we update all the resources
    // FIXME Remove once IM handles single nodes state update!!!! And pay attention to the
    // AbstractDeploymentProviderService.updateOnError method!
    Deployment deployment = deploymentRepository.findOne(deploymentUuid);
    switch (deployment.getStatus()) {
      case CREATE_FAILED:
      case UPDATE_FAILED:
      case DELETE_FAILED:
        LOG.warn("Deployment < {} > was already in {} state.", deploymentUuid,
            deployment.getStatus());
        break;
      case CREATE_IN_PROGRESS:
        deployment.setStatus(Status.CREATE_FAILED);
        updateResources(deployment, Status.CREATE_FAILED);
        break;
      case DELETE_IN_PROGRESS:
        deployment.setStatus(Status.DELETE_FAILED);
        updateResources(deployment, Status.DELETE_FAILED);
        break;
      case UPDATE_IN_PROGRESS:
        deployment.setStatus(Status.UPDATE_FAILED);
        updateResources(deployment, Status.UPDATE_FAILED);
        break;
      default:
        LOG.error("updateOnError: unsupported deployment status: {}. Setting status to {}",
            deployment.getStatus(), Status.UNKNOWN.toString());
        deployment.setStatus(Status.UNKNOWN);
        updateResources(deployment, Status.UNKNOWN);
        break;
    }
    deployment.setTask(Task.NONE);
    // Do not delete a previous statusReason if there's no explicit value! (used when isDeploy
    // reports an error and then the PollDeploy task calls the finalizeDeploy, which also uses this
    // method but does not have any newer statusReason)
    if (message != null) {
      deployment.setStatusReason(message);
    }
    deploymentRepository.save(deployment);
  }
}
