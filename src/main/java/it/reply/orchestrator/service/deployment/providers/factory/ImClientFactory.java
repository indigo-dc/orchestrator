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

package it.reply.orchestrator.service.deployment.providers.factory;

import com.google.common.base.Preconditions;

import es.upv.i3m.grycap.im.InfrastructureManager;
import es.upv.i3m.grycap.im.auth.credentials.providers.AmazonEc2Credentials;
import es.upv.i3m.grycap.im.auth.credentials.providers.AzureCredentials;
import es.upv.i3m.grycap.im.auth.credentials.providers.ImCredentials;
import es.upv.i3m.grycap.im.auth.credentials.providers.OpenNebulaCredentials;
import es.upv.i3m.grycap.im.auth.credentials.providers.OpenStackAuthVersion;
import es.upv.i3m.grycap.im.auth.credentials.providers.OpenStackCredentials;
import es.upv.i3m.grycap.im.exceptions.ImClientException;

import it.reply.orchestrator.config.properties.ImProperties;
import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.dal.entity.OidcEntityId;
import it.reply.orchestrator.dal.repository.OidcEntityRepository;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.CloudProviderEndpoint.IaaSType;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.utils.CommonUtils;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class ImClientFactory {

  private static final Pattern OS_ENDPOINT_PATTERN =
      Pattern.compile("(https?:\\/\\/[^\\/]+)(?:\\/(?:([^\\/]+)\\/?)?)?");

  private OidcProperties oidcProperties;

  private ImProperties imProperties;

  private OidcEntityRepository oidcEntityRepository;

  protected OpenStackCredentials getOpenStackAuthHeader(CloudProviderEndpoint cloudProviderEndpoint,
      @NonNull String accessToken) {
    String endpoint = cloudProviderEndpoint.getCpEndpoint();
    Matcher matcher = OS_ENDPOINT_PATTERN.matcher(endpoint);
    if (!matcher.matches()) {
      throw new DeploymentException("Wrong OS endpoint format: " + endpoint);
    } else {
      String organization = oidcEntityRepository
          .findByOidcEntityId(OidcEntityId.fromAccesToken(accessToken))
          .orElseThrow(
              () -> new DeploymentException("No user associated to deployment token found"))
          .getOrganization();
      endpoint = matcher.group(1);
      OpenStackCredentials cred = cloudProviderEndpoint
          .getIaasHeaderId()
          .map(OpenStackCredentials::buildCredentials)
          .orElseGet(OpenStackCredentials::buildCredentials)
          .withTenant("oidc")
          .withUsername(organization)
          .withPassword(accessToken)
          .withHost(endpoint);
      cloudProviderEndpoint
          .getRegion()
          .ifPresent(cred::withServiceRegion);
      if ("v2".equals(matcher.group(2))) {
        throw new DeploymentException("Openstack keystone v2 not supported");
      } else {
        cred.withAuthVersion(OpenStackAuthVersion.PASSWORD_3_X_TOKEN);
      }
      return cred;
    }
  }

  protected OpenNebulaCredentials getOpenNebulaAuthHeader(
      CloudProviderEndpoint cloudProviderEndpoint, @NonNull String accessToken) {
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

  protected AzureCredentials getAzureAuthHeader(CloudProviderEndpoint cloudProviderEndpoint) {
    return cloudProviderEndpoint
        .getIaasHeaderId()
        .map(AzureCredentials::buildCredentials)
        .orElseGet(AzureCredentials::buildCredentials)
        .withUsername(cloudProviderEndpoint.getUsername())
        .withPassword(cloudProviderEndpoint.getPassword())
        .withSubscriptionId(cloudProviderEndpoint.getTenant());
  }

  protected String getImAuthHeader(@Nullable String accessToken) {
    Optional<String> imAuthHeader = imProperties.getImAuthHeader();
    if (imAuthHeader.isPresent()) {
      LOG.debug("IM authorization header retrieved from properties file");
      return imAuthHeader.get();
    } else if (oidcProperties.isEnabled()) {
      String header = ImCredentials
          .buildCredentials()
          .withToken(accessToken)
          .serialize();
      LOG.debug("IM authorization header built from access token");
      return header;
    } else {
      throw new OrchestratorException(
          "No Authentication info provided for for Infrastructure Manager "
              + "and OAuth2 authentication is disabled");

    }
  }

  private OpenStackCredentials getOtcAuthHeader(CloudProviderEndpoint cloudProviderEndpoint) {
    String endpoint = cloudProviderEndpoint.getCpEndpoint();
    Matcher endpointMatcher = OS_ENDPOINT_PATTERN.matcher(endpoint);
    if (!endpointMatcher.matches()) {
      throw new DeploymentException("Wrong OS endpoint format: " + endpoint);
    } else {
      String username = CommonUtils.notNullOrDefaultValue(cloudProviderEndpoint.getUsername(), "");
      String password = cloudProviderEndpoint.getPassword();
      String tenant = cloudProviderEndpoint.getTenant();
      // TODO REMOVE and use explicit cloudProviderEndpoint information
      Pattern pattern = Pattern.compile("\\s*(\\w+)\\s+([\\w\\-]+)\\s*");
      Matcher credentialsMatcher = pattern.matcher(username);
      if (credentialsMatcher.matches()) {
        String otcUsername = Preconditions.checkNotNull(credentialsMatcher.group(1),
            "No vaild username provided for Open Telekom Cloud");
        tenant = Preconditions.checkNotNull(credentialsMatcher.group(2),
            "No vaild username provided for Open Telekom Cloud");
        if (!otcUsername.matches("[0-9]+")) {
          // new style username, it domain part not needed
          username = otcUsername;
        }
      }
      /////////////////////////////////////////////////////////////////
      return cloudProviderEndpoint
          .getIaasHeaderId()
          .map(OpenStackCredentials::buildCredentials)
          .orElseGet(OpenStackCredentials::buildCredentials)
          .withDomain(tenant)
          .withUsername(username)
          .withPassword(password)
          .withTenant("eu-de")
          .withAuthVersion(OpenStackAuthVersion.PASSWORD_3_X)
          .withHost(endpointMatcher.group(1))
          .withServiceName("None")
          .withServiceRegion("eu-de");
    }
  }

  private String getIaasAuthHeader(CloudProviderEndpoint cloudProviderEndpoint,
      @Nullable String accessToken) {
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

      if (cloudProviderEndpoint.getUsername() != null) {
        iaasHeader = iaasHeader
            .replaceFirst(Matcher.quoteReplacement("<USERNAME>"),
                cloudProviderEndpoint.getUsername());
      }
      if (cloudProviderEndpoint.getPassword() != null) {
        iaasHeader = iaasHeader
            .replaceFirst(Matcher.quoteReplacement("<PASSWORD>"),
                cloudProviderEndpoint.getPassword());
      }
      if (cloudProviderEndpoint.getTenant() != null) {
        iaasHeader = iaasHeader
            .replaceFirst(Matcher.quoteReplacement("<TENANT>"),
                cloudProviderEndpoint.getTenant());
      }

      LOG.debug("IaaS authorization header for compute service " + computeServiceId
          + " retrieved from properties file");
    } else {
      oidcProperties.runIfSecurityDisabled(() -> {
        throw new OrchestratorException("No Authentication info provided for compute service "
            + computeServiceId + " and OAuth2 authentication is disabled");
      });
      switch (iaasType) {
        case OPENSTACK:
          iaasHeader = getOpenStackAuthHeader(cloudProviderEndpoint,
              CommonUtils.checkNotNull(accessToken)).serialize();
          break;
        case OPENNEBULA:
          iaasHeader = getOpenNebulaAuthHeader(cloudProviderEndpoint,
              CommonUtils.checkNotNull(accessToken)).serialize();
          break;
        case AWS:
          iaasHeader = getAwsAuthHeader(cloudProviderEndpoint).serialize();
          break;
        case OTC:
          iaasHeader = getOtcAuthHeader(cloudProviderEndpoint).serialize();
          break;
        case AZURE:
          iaasHeader = getAzureAuthHeader(cloudProviderEndpoint).serialize();
          break;
        default:
          throw new IllegalArgumentException(
              String.format("Unsupported provider type <%s>", iaasType));
      }
    }
    return iaasHeader;
  }

  /**
   * Generates a Infrastructure Manager client.
   * 
   * @param cloudProviderEndpoints
   *          the cloud providers information on which IM will authenticate
   * @param accessToken
   *          the optional OAuth2 token
   * @return the generated client
   */
  @SneakyThrows(ImClientException.class)
  public InfrastructureManager build(List<CloudProviderEndpoint> cloudProviderEndpoints,
      @Nullable String accessToken) {
    String imAuthHeader = getImAuthHeader(accessToken);
    Stream<String> iaasHeadersStream = cloudProviderEndpoints
        .stream()
        .map(cloudProviderEndpoint -> getIaasAuthHeader(cloudProviderEndpoint, accessToken));

    String imHeader = Stream
        .concat(Stream.of(imAuthHeader), iaasHeadersStream)
        .collect(Collectors.joining("\\n"));
    LOG.trace("IM auth header: {}", imHeader);

    String imUrl = null;
    if (cloudProviderEndpoints.size() == 1) {
      // use im local endpoint only if not hybrid (and if available)
      imUrl = cloudProviderEndpoints.get(0).getImEndpoint();
    }
    if (imUrl == null) {
      imUrl = imProperties.getUrl().toString();
    }

    return new InfrastructureManager(imUrl, imHeader);
  }

}
