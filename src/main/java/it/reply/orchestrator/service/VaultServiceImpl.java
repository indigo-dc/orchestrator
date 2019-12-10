/*
 * Copyright © 2019 I.N.F.N.
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

import it.reply.orchestrator.config.properties.VaultProperties;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dto.vault.TokenAuthenticationExtended;
import it.reply.orchestrator.dto.vault.VaultTokenExtended;
import it.reply.orchestrator.dto.vault.VaultTokenResponseExtended;
import it.reply.orchestrator.exception.VaultJwtTokenExpiredException;
import it.reply.orchestrator.exception.VaultServiceNotAvailableException;
import it.reply.orchestrator.service.security.OAuth2TokenService;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.client.VaultResponses;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;
import org.springframework.vault.support.VaultToken;
import org.springframework.vault.support.VaultTokenResponse;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@EnableConfigurationProperties(VaultProperties.class)
@Slf4j
public class VaultServiceImpl implements VaultService {

  private VaultProperties vaultProperties;
  private OAuth2TokenService oauth2TokenService;
  private RestTemplate restTemplate;

  /**
   * Creates a new {@link VaultServiceImpl}.
   *
   * @param vaultProperties
   *     the vaultProperties
   * @param oauth2TokenService
   *     the oauth2TokenService
   * @param restTemplateBuilder
   *     the restTemplateBuilder
   */
  public VaultServiceImpl(VaultProperties vaultProperties,
      OAuth2TokenService oauth2TokenService,
      RestTemplateBuilder restTemplateBuilder) {
    this.vaultProperties = vaultProperties;
    this.oauth2TokenService = oauth2TokenService;
    this.restTemplate = restTemplateBuilder.build();
  }

  private URI getSystemVaultUri() {
    return getServiceUri()
        .orElseThrow(() -> new VaultServiceNotAvailableException());
  }

  @Override
  public Optional<URI> getServiceUri() {
    return Optional.ofNullable(vaultProperties.getUrl());
  }

  private VaultTemplate getTemplate(URI uri, ClientAuthentication token) {
    return new VaultTemplate(VaultEndpoint.from(uri), token);
  }

  @Override
  public VaultResponse writeSecret(URI uri, ClientAuthentication token, String path,
      Object secret) {
    return getTemplate(uri, token).write(path, secret);
  }

  @Override
  public VaultResponse writeSecret(ClientAuthentication token, String path, Object secret) {
    return this.writeSecret(getSystemVaultUri(), token, path, secret);
  }

  @Override
  public <T> T readSecret(URI uri, ClientAuthentication token, String path, Class<T> type) {
    return getTemplate(uri, token).read(path, type).getData();
  }

  @Override
  public <T> T readSecret(ClientAuthentication token, String path, Class<T> type) {
    return this.readSecret(getSystemVaultUri(), token, path, type);
  }

  @Override
  public Map<String, Object> readSecret(URI uri, ClientAuthentication token, String path) {
    return getTemplate(uri, token).read(path).getData();
  }

  @Override
  public Map<String, Object> readSecret(ClientAuthentication token, String path) {
    return this.readSecret(getSystemVaultUri(), token, path);
  }

  @Override
  public void deleteSecret(URI uri, ClientAuthentication token, String path) {
    getTemplate(uri, token).delete(path);
  }

  @Override
  public void deleteSecret(ClientAuthentication token, String path) {
    this.deleteSecret(getSystemVaultUri(), token, path);
  }

  @Override
  public List<String> listSecrets(URI uri, ClientAuthentication token, String path) {
    return getTemplate(uri, token).list(path);
  }

  @Override
  public List<String> listSecrets(ClientAuthentication token, String path) {
    return this.listSecrets(getSystemVaultUri(), token, path);
  }

  /**
   * Retrieve the vault token from the IAM token using passed Vault server URI.
   */
  @Override
  public TokenAuthenticationExtended retrieveToken(URI uri, String accessToken) {
    uri = VaultEndpoint.from(uri).createUri("auth/jwt/login");
    Map<String, String> login = new HashMap<>();
    login.put("jwt", accessToken);
    try {
      VaultTokenResponseExtended token = restTemplate
          .postForObject(uri, login, VaultTokenResponseExtended.class);
      return new TokenAuthenticationExtended(token.getToken(), token.getEntityId());
    } catch (HttpClientErrorException ex) {
      if (ex.getRawStatusCode() == 400) {
        String errorCause = VaultResponses.getError(ex.getResponseBodyAsString());
        if (errorCause != null) {
          if (errorCause.contains("expired")) {
            throw new VaultJwtTokenExpiredException(
                "Unable to retrieve token for Vault: IAM access token is expired");
          } else {
            LOG.warn("Got response 400 with error cause:\n{}", errorCause);
          }
        }
      }
      throw ex;
    }
  }

  /**
   * Retrieve the vault token from the IAM token.
   */
  @Override
  public TokenAuthenticationExtended retrieveToken(String accessToken) {
    return retrieveToken(getSystemVaultUri(), accessToken);
  }

  /**
   * Retrieve the vault token from the IAM token identifier using passed Vault server URI.
   */
  @Override
  public TokenAuthenticationExtended retrieveToken(URI uri, OidcTokenId oidcTokenId) {
    return oauth2TokenService.executeWithClientForResult(
        oidcTokenId,
        accessToken -> this.retrieveToken(uri, accessToken),
        VaultJwtTokenExpiredException.class::isInstance);
  }

  /**
   * Retrieve the vault token from the IAM token identifier.
   */
  @Override
  public TokenAuthenticationExtended retrieveToken(OidcTokenId oidcTokenId) {
    return this.retrieveToken(getSystemVaultUri(), oidcTokenId);
  }

  /**
   * Retrieve the vault path.
   */
  @Override
  public String getServicePath() {
    return vaultProperties.getPath();
  }

}
