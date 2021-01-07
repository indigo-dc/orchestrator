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
import it.reply.orchestrator.dal.entity.OidcEntity;
import it.reply.orchestrator.dal.entity.OidcEntityId;
import it.reply.orchestrator.dal.repository.OidcEntityRepository;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.CloudProviderEndpoint.IaaSType;
import it.reply.orchestrator.dto.cmdb.CloudService.SupportedIdp;
import it.reply.orchestrator.dto.security.GenericServiceCredential;
import it.reply.orchestrator.dto.security.GenericServiceCredentialWithTenant;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.deployment.providers.CredentialProviderService;
import it.reply.orchestrator.utils.CommonUtils;

import java.util.List;
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

  private CredentialProviderService credProvServ;

  protected OpenStackCredentials getOpenStackAuthHeader(CloudProviderEndpoint cloudProviderEndpoint,
      @NonNull String accessToken) {
    String endpoint = cloudProviderEndpoint.getCpEndpoint();
    Matcher matcher = OS_ENDPOINT_PATTERN.matcher(endpoint);
    if (!matcher.matches()) {
      throw new DeploymentException("Wrong OS endpoint format: " + endpoint);
    } else {
      if (cloudProviderEndpoint.isIamEnabled()) {
        OidcEntityId oidcEntityId =  OidcEntityId.fromAccesToken(accessToken);
        OidcEntity oidcEntity = oidcEntityRepository
                .findByOidcEntityId(oidcEntityId)
                .orElseThrow(
                    () -> new DeploymentException("No user associated to deployment token found"));
        // Compute the Username field of the IM Authorization Header for Openstack as follows:
        // Use the name of the IDP (that matches the token issuer) as configured in CMDB, if present
        // otherwise use the organization name retrieved from the token.
        String organization = oidcEntity.getOrganization();
        String issuer = oidcEntityId.getIssuer();
        SupportedIdp supportedidp = cloudProviderEndpoint
                       .getSupportedIdps().stream()
                       .filter(idp -> issuer.equals(idp.getIssuer())).findAny()
                       .orElse(new SupportedIdp(organization, issuer));

        endpoint = matcher.group(1);
        OpenStackCredentials cred = cloudProviderEndpoint
            .getIaasHeaderId()
            .map(OpenStackCredentials::buildCredentials)
            .orElseGet(OpenStackCredentials::buildCredentials)
            .withTenant(cloudProviderEndpoint.getIdpProtocol())
            .withUsername(supportedidp.getName())
            .withPassword(accessToken)
            .withHost(endpoint);
        cloudProviderEndpoint
            .getRegion()
            .ifPresent(cred::withServiceRegion);
        cloudProviderEndpoint
            .getTenant()
            .ifPresent(cred::withDomain); // use domain to avoid mapping ambiguity at IaaS level
        if ("v2".equals(matcher.group(2))) {
          throw new DeploymentException("Openstack keystone v2 not supported");
        } else {
          cred.withAuthVersion(OpenStackAuthVersion.PASSWORD_3_X_TOKEN);
        }
        return cred;
      } else {
        GenericServiceCredentialWithTenant imCred =
            credProvServ.credentialProvider(cloudProviderEndpoint.getCpComputeServiceId(),
                accessToken,
                GenericServiceCredentialWithTenant.class);
        endpoint = matcher.group(1);
        OpenStackCredentials cred = cloudProviderEndpoint
            .getIaasHeaderId()
            .map(OpenStackCredentials::buildCredentials)
            .orElseGet(OpenStackCredentials::buildCredentials)
            .withTenant(imCred.getTenant())
            .withUsername(imCred.getUsername())
            .withPassword(imCred.getPassword())
            .withHost(endpoint);
        cloudProviderEndpoint
            .getRegion()
            .ifPresent(cred::withServiceRegion);
        if ("v2".equals(matcher.group(2))) {
          cred.withAuthVersion(OpenStackAuthVersion.PASSWORD_2_0);
        } else {
          cred.withAuthVersion(OpenStackAuthVersion.PASSWORD_3_X);
        }
        return cred;
      }
    }
  }

  protected OpenNebulaCredentials getOpenNebulaAuthHeader(
      CloudProviderEndpoint cloudProviderEndpoint, @NonNull String accessToken) {
    if (cloudProviderEndpoint.isIamEnabled()) {
      return cloudProviderEndpoint
          .getIaasHeaderId()
          .map(OpenNebulaCredentials::buildCredentials)
          .orElseGet(OpenNebulaCredentials::buildCredentials)
          .withHost(cloudProviderEndpoint.getCpEndpoint())
          .withToken(accessToken);
    } else {
      GenericServiceCredential imCred = credProvServ.credentialProvider(
          cloudProviderEndpoint.getCpComputeServiceId(),
          accessToken,
          GenericServiceCredential.class);

      return cloudProviderEndpoint
          .getIaasHeaderId()
          .map(OpenNebulaCredentials::buildCredentials)
          .orElseGet(OpenNebulaCredentials::buildCredentials)
          .withHost(cloudProviderEndpoint.getCpEndpoint())
          .withUsername(imCred.getUsername())
          .withPassword(imCred.getPassword());
    }
  }

  protected AmazonEc2Credentials getAwsAuthHeader(CloudProviderEndpoint cloudProviderEndpoint,
      String accessToken) {
    // Get credential from vault Service
    GenericServiceCredential imCred = credProvServ.credentialProvider(
        cloudProviderEndpoint.getCpComputeServiceId(), accessToken, GenericServiceCredential.class);

    return cloudProviderEndpoint
        .getIaasHeaderId()
        .map(AmazonEc2Credentials::buildCredentials)
        .orElseGet(AmazonEc2Credentials::buildCredentials)
        .withUsername(imCred.getUsername())
        .withPassword(imCred.getPassword());
  }

  protected AzureCredentials getAzureAuthHeader(CloudProviderEndpoint cloudProviderEndpoint,
      String accessToken) {
    GenericServiceCredentialWithTenant imCred =
        credProvServ.credentialProvider(cloudProviderEndpoint.getCpComputeServiceId(), accessToken,
            GenericServiceCredentialWithTenant.class);

    return cloudProviderEndpoint
        .getIaasHeaderId()
        .map(AzureCredentials::buildCredentials)
        .orElseGet(AzureCredentials::buildCredentials)
        .withUsername(imCred.getUsername())
        .withPassword(imCred.getPassword())
        .withSubscriptionId(imCred.getTenant());
  }

  protected String getImAuthHeader(@Nullable String accessToken) {
    if (oidcProperties.isEnabled()) {
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

  private OpenStackCredentials getOtcAuthHeader(CloudProviderEndpoint cloudProviderEndpoint,
      String accessToken) {
    String endpoint = cloudProviderEndpoint.getCpEndpoint();
    Matcher endpointMatcher = OS_ENDPOINT_PATTERN.matcher(endpoint);
    if (!endpointMatcher.matches()) {
      throw new DeploymentException("Wrong OS endpoint format: " + endpoint);
    } else {
      GenericServiceCredentialWithTenant imCred =
          credProvServ.credentialProvider(cloudProviderEndpoint.getCpComputeServiceId(),
              accessToken, GenericServiceCredentialWithTenant.class);
      String username = imCred.getUsername();
      String password = imCred.getPassword();
      String tenant = imCred.getTenant();
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
    String iaasHeader;
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
        iaasHeader = getAwsAuthHeader(cloudProviderEndpoint, accessToken).serialize();
        break;
      case OTC:
        iaasHeader = getOtcAuthHeader(cloudProviderEndpoint, accessToken).serialize();
        break;
      case AZURE:
        iaasHeader = getAzureAuthHeader(cloudProviderEndpoint, accessToken).serialize();
        break;
      default:
        throw new IllegalArgumentException(
            String.format("Unsupported provider type <%s>", iaasType));
    }

    return iaasHeader;
  }

  /**
   * Generates a Infrastructure Manager client.
   *
   * @param cloudProviderEndpoints the cloud providers information on which IM will authenticate
   * @param accessToken the optional OAuth2 token
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
