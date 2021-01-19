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

package it.reply.orchestrator.service.security;

import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.config.properties.OidcProperties.IamProperties;
import it.reply.orchestrator.exception.OrchestratorException;

import java.util.Optional;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.mitre.oauth2.model.RegisteredClient;
import org.mitre.openid.connect.client.service.ClientConfigurationService;
import org.mitre.openid.connect.client.service.ServerConfigurationService;
import org.mitre.openid.connect.config.ServerConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class OAuth2ConfigurationsService {

  private OidcProperties oidcProperties;

  private ServerConfigurationService serverConfigurationService;

  private ClientConfigurationService clientConfigurationService;

  @Autowired
  protected OAuth2ConfigurationsService(@NonNull OidcProperties oidcProperties,
      @NonNull ApplicationContext applicationContext) {
    this.oidcProperties = oidcProperties;

    if (oidcProperties.isEnabled()) {
      serverConfigurationService = applicationContext.getBean(ServerConfigurationService.class);
      clientConfigurationService = applicationContext.getBean(ClientConfigurationService.class);
    }
  }

  /**
   * Get a server configuration.
   *
   * @param issuer
   *          the issuer of the server
   * @return the server configuration
   */
  public ServerConfiguration getServerConfiguration(String issuer) {
    oidcProperties.throwIfSecurityDisabled();
    return Optional.ofNullable(serverConfigurationService.getServerConfiguration(issuer))
        .orElseThrow(() -> new OrchestratorException(
            "No server configuration found for IAM with issuer " + issuer));
  }

  /**
   * Get a client configuration.
   *
   * @param serverConfiguration
   *          the server configuration on which the client has been registered
   * @return the client configuration
   */
  public RegisteredClient getClientConfiguration(ServerConfiguration serverConfiguration) {
    oidcProperties.throwIfSecurityDisabled();
    return Optional
        .ofNullable(clientConfigurationService.getClientConfiguration(serverConfiguration))
        .orElseThrow(() -> new OrchestratorException(
            "No client configuration found for IAM with iss" + serverConfiguration.getIssuer()));
  }

  /**
   * Get a client configuration.
   *
   * @param issuer
   *          the issuer of the server on which the client has been registered
   * @return the client configuration
   */
  public RegisteredClient getClientConfiguration(String issuer) {
    oidcProperties.throwIfSecurityDisabled();
    ServerConfiguration serverConfiguration = getServerConfiguration(issuer);
    return getClientConfiguration(serverConfiguration);
  }

  /**
   * Get audience.
   *
   * @param issuer
   *          the issuer of the server on which the client has been registered
   * @return the audience UUID
   */
  public String getAudience(String issuer) {
    oidcProperties.throwIfSecurityDisabled();
    return oidcProperties
        .getIamConfiguration(issuer)
        .map(IamProperties::getAudience)
        .orElseThrow(() -> new OrchestratorException(
            "No audience configuration found for IAM with iss" + issuer));
  }

}
