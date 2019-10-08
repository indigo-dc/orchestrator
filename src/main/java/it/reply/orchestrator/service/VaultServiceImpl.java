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
import it.reply.orchestrator.exception.VaultJwtTokenExpiredException;
import it.reply.orchestrator.exception.VaultServiceNotAvailableException;
import it.reply.orchestrator.service.security.OAuth2TokenService;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  private VaultEndpoint getEndpoint() {
    if (vaultProperties.getUri() == null) {
      throw new VaultServiceNotAvailableException();
    }
    return VaultEndpoint.from(vaultProperties.getUri());
  }

  private VaultEndpoint getEndpoint(URI vaultUri) {
    return VaultEndpoint.from(vaultUri);
  }

  private VaultTemplate getTemplate(ClientAuthentication token) {
    return new VaultTemplate(getEndpoint(), token);
  }

  private VaultTemplate getTemplate(URI uri, ClientAuthentication token) {
    return new VaultTemplate(getEndpoint(uri), token);
  }

  @Override
  public URI getServiceUri() {
    return vaultProperties.getUri();
  }

  @Override
  public VaultResponse writeSecret(ClientAuthentication token, String path, Object secret) {
    return getTemplate(token).write(path, secret);
  }

  @Override
  public VaultResponse writeSecret(URI uri, ClientAuthentication token, String path,
      Object secret) {
    return getTemplate(uri, token).write(path, secret);
  }

  @Override
  public <T> T readSecret(ClientAuthentication token, String path, Class<T> type) {
    return getTemplate(token).read(path, type).getData();
  }

  @Override
  public <T> T readSecret(URI uri, ClientAuthentication token, String path, Class<T> type) {
    return getTemplate(uri, token).read(path, type).getData();
  }

  @Override
  public Map<String, Object> readSecret(ClientAuthentication token, String path) {
    return getTemplate(token).read(path).getData();
  }

  @Override
  public Map<String, Object> readSecret(URI uri, ClientAuthentication token, String path) {
    return getTemplate(uri, token).read(path).getData();
  }

  @Override
  public void deleteSecret(ClientAuthentication token, String path) {
    getTemplate(token).delete(path);
  }

  @Override
  public void deleteSecret(URI uri, ClientAuthentication token, String path) {
    getTemplate(uri, token).delete(path);
  }

  @Override
  public List<String> listSecrets(ClientAuthentication token, String path) {
    return getTemplate(token).list(path);
  }

  @Override
  public List<String> listSecrets(URI uri, ClientAuthentication token, String path) {
    return getTemplate(uri, token).list(path);
  }

  /**
   * Retrieve the vault token from the IAM token.
   */
  @Override
  public TokenAuthentication retrieveToken(String accessToken) {
    URI authUri = getEndpoint().createUri("auth/jwt/login");
    Map<String, String> login = new HashMap<>();
    login.put("jwt", accessToken);
    try {
      VaultToken token = restTemplate
          .postForObject(authUri, login, VaultTokenResponse.class)
          .getToken();
      return new TokenAuthentication(token);
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
  public TokenAuthentication retrieveToken(URI uri, String accessToken) {
    URI authUri = getEndpoint(uri).createUri("auth/jwt/login");
    Map<String, String> login = new HashMap<>();
    login.put("jwt", accessToken);
    try {
      VaultToken token = restTemplate
          .postForObject(authUri, login, VaultTokenResponse.class)
          .getToken();
      return new TokenAuthentication(token);
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
   * Retrieve the vault token from the IAM token identifier.
   */
  @Override
  public TokenAuthentication retrieveToken(OidcTokenId oidcTokenId) {
    return oauth2TokenService.executeWithClientForResult(
        oidcTokenId,
        this::retrieveToken,
        VaultJwtTokenExpiredException.class::isInstance);
  }

  /**
   * Retrieve the vault token from the IAM token identifier.
   */
  @Override
  public TokenAuthentication retrieveToken(URI uri, OidcTokenId oidcTokenId) {
    return oauth2TokenService.executeWithClientForResult(
        oidcTokenId,
        this::retrieveToken,
        VaultJwtTokenExpiredException.class::isInstance);
  }

}
