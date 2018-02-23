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

package it.reply.orchestrator.service;

import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.config.properties.SlamProperties;
import it.reply.orchestrator.dal.entity.OidcEntity;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dal.repository.OidcEntityRepository;
import it.reply.orchestrator.dto.slam.SlamPreferences;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.security.OAuth2TokenService;

import java.net.URI;
import java.util.Optional;

import javax.ws.rs.core.UriBuilder;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.RequestEntity.HeadersBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@EnableConfigurationProperties(SlamProperties.class)
public class SlamServiceImpl implements SlamService {

  private OidcEntityRepository oidcEntityRepository;

  private OidcProperties oidcProperties;

  private SlamProperties slamProperties;

  private OAuth2TokenService oauth2TokenService;

  private RestTemplate restTemplate;

  /**
   * Creates a new SlamServiceImpl.
   * 
   * @param oidcEntityRepository
   *          the OidcEntityRepository to use
   * @param oidcProperties
   *          the OidcProperties to use
   * @param slamProperties
   *          the SlamProperties to use
   * @param oauth2TokenService
   *          the OAuth2TokenService to use
   * @param restTemplateBuilder
   *          the RestTemplateBuilder to use
   */
  public SlamServiceImpl(OidcEntityRepository oidcEntityRepository, OidcProperties oidcProperties,
      SlamProperties slamProperties, OAuth2TokenService oauth2TokenService,
      RestTemplateBuilder restTemplateBuilder) {
    this.oidcEntityRepository = oidcEntityRepository;
    this.oidcProperties = oidcProperties;
    this.slamProperties = slamProperties;
    this.oauth2TokenService = oauth2TokenService;
    this.restTemplate = restTemplateBuilder.build();
  }

  protected ResponseEntity<SlamPreferences> get(URI requestUri, OidcTokenId tokenId) {
    if (!oidcProperties.isEnabled()) {
      return executeGet(requestUri, null);
    }
    try {
      String accessToken = oauth2TokenService.getAccessToken(tokenId);
      return executeGet(requestUri, accessToken);
    } catch (HttpClientErrorException ex) {
      if (HttpStatus.UNAUTHORIZED == ex.getStatusCode()) {
        String refreshedAccessToken = oauth2TokenService.getRefreshedAccessToken(tokenId);
        return executeGet(requestUri, refreshedAccessToken);
      } else {
        throw ex;
      }
    }
  }

  private ResponseEntity<SlamPreferences> executeGet(URI requestUri, @Nullable String accessToken) {
    HeadersBuilder<?> requestBuilder = RequestEntity.get(requestUri);
    if (accessToken != null) {
      requestBuilder.header(HttpHeaders.AUTHORIZATION,
          String.format("%s %s", OAuth2AccessToken.BEARER_TYPE, accessToken));
    }
    return restTemplate.exchange(requestBuilder.build(), SlamPreferences.class);
  }

  @Override
  public SlamPreferences getCustomerPreferences(OidcTokenId tokenId) {

    String slamCustomer = Optional
        .ofNullable(tokenId)
        .map(OidcTokenId::getOidcEntityId)
        .flatMap(oidcEntityRepository::findByOidcEntityId)
        .map(OidcEntity::getOrganization)
        .orElse("indigo-dc");

    URI requestUri = UriBuilder
        .fromUri(slamProperties.getUrl() + slamProperties.getCustomerPreferencesPath())
        .build(slamCustomer)
        .normalize();

    try {
      return get(requestUri, tokenId).getBody();
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error fetching SLA for customer <" + slamCustomer + "> from SLAM.", ex);
    }
  }

}
