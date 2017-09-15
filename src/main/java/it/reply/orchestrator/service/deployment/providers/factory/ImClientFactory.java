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

package it.reply.orchestrator.service.deployment.providers.factory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import es.upv.i3m.grycap.im.InfrastructureManager;
import es.upv.i3m.grycap.im.auth.credentials.providers.AmazonEc2Credentials;
import es.upv.i3m.grycap.im.auth.credentials.providers.ImCredentials;
import es.upv.i3m.grycap.im.auth.credentials.providers.OpenNebulaCredentials;
import es.upv.i3m.grycap.im.auth.credentials.providers.OpenStackAuthVersion;
import es.upv.i3m.grycap.im.auth.credentials.providers.OpenStackCredentials;
import es.upv.i3m.grycap.im.exceptions.ImClientException;

import it.reply.orchestrator.config.properties.ImProperties;
import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.CloudProviderEndpoint.IaaSType;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.orchestrator.utils.CommonUtils;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class ImClientFactory {

  private static final Pattern OS_ENDPOINT_PATTERN =
      Pattern.compile("(https?:\\/\\/[^\\/]+)(?:\\/(?:([^\\/]+)\\/?)?)?");

  private OidcProperties oidcProperties;

  private ImProperties imProperties;

  private OAuth2TokenService oauth2TokenService;

  protected OpenStackCredentials getOpenStackAuthHeader(CloudProviderEndpoint cloudProviderEndpoint,
      @NonNull OidcTokenId requestedWithToken) {
    String endpoint = cloudProviderEndpoint.getCpEndpoint();
    Matcher matcher = OS_ENDPOINT_PATTERN.matcher(endpoint);
    if (!matcher.matches()) {
      throw new DeploymentException("Wrong OS endpoint format: " + endpoint);
    } else {
      endpoint = matcher.group(1);
      String accessToken = oauth2TokenService.getAccessToken(requestedWithToken);
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
    String accessToken = oauth2TokenService.getAccessToken(requestedWithToken);
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
      String accessToken =
          oauth2TokenService.getAccessToken(CommonUtils.checkNotNull(requestedWithToken));
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

  private OpenStackCredentials getOtcAuthHeader(CloudProviderEndpoint cloudProviderEndpoint) {
    String endpoint = cloudProviderEndpoint.getCpEndpoint();
    Matcher endpointMatcher = OS_ENDPOINT_PATTERN.matcher(endpoint);
    if (!endpointMatcher.matches()) {
      throw new DeploymentException("Wrong OS endpoint format: " + endpoint);
    } else {
      String username = cloudProviderEndpoint.getUsername();
      String password = cloudProviderEndpoint.getPassword();
      String tenant = cloudProviderEndpoint.getTenant();
      // TODO REMOVE and use explicit cloudProviderEndpoint information
      Pattern pattern = Pattern.compile("\\s*(\\w+)\\s+(\\w+)\\s*");
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
              CommonUtils.checkNotNull(requestedWithToken)).serialize();
          break;
        case OPENNEBULA:
          iaasHeader = getOpenNebulaAuthHeader(cloudProviderEndpoint,
              CommonUtils.checkNotNull(requestedWithToken)).serialize();
          break;
        case AWS:
          iaasHeader = getAwsAuthHeader(cloudProviderEndpoint).serialize();
          break;
        case OTC:
          iaasHeader = getOtcAuthHeader(cloudProviderEndpoint).serialize();
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
      imUrl = imProperties.getUrl().toString();
    } else {
      imUrl = Optional
          .ofNullable(cloudProviderEndpoints.get(0).getImEndpoint())
          .orElseGet(() -> imProperties.getUrl().toString());
    }
    String imHeader = String.format("%s\\n%s", imAuthHeader, iaasHeaders);
    LOG.trace("IM auth header: {}", imHeader);
    return new InfrastructureManager(imUrl, imHeader);
  }

  /**
   * Generates a Infrastructure Manager client.
   * 
   * @param cloudProviderEndpoints
   *          the cloud providers information on which IM will authenticate
   * @param requestedWithToken
   *          the optional OAuth2 token
   * @return the generated client
   */
  public InfrastructureManager build(List<CloudProviderEndpoint> cloudProviderEndpoints,
      @Nullable OidcTokenId requestedWithToken) {
    String imAuthHeader = getImAuthHeader(requestedWithToken);
    String iaasHeaders = cloudProviderEndpoints
        .stream()
        .map(cloudProviderEndpoint -> getIaasAuthHeader(cloudProviderEndpoint, requestedWithToken))
        .collect(Collectors.joining("\\n"));
    return getIm(cloudProviderEndpoints, imAuthHeader, iaasHeaders);
  }

}
