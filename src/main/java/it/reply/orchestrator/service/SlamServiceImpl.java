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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.RequestEntity.HeadersBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Optional;
import java.util.function.BiFunction;

import javax.ws.rs.core.UriBuilder;

@Service
@EnableConfigurationProperties(SlamProperties.class)
public class SlamServiceImpl implements SlamService {

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private OidcEntityRepository oidcEntityRepository;

  @Autowired
  private OidcProperties oidcProperties;

  @Autowired
  private SlamProperties slamProperties;

  @Autowired
  private OAuth2TokenService oauth2TokenService;

  protected <R> R executeWithClient(URI requestUri, OidcTokenId tokenId,
      BiFunction<URI, @Nullable String, R> function) {
    if (!oidcProperties.isEnabled()) {
      return function.apply(requestUri, null);
    }
    try {
      String accessToken = oauth2TokenService.getAccessToken(tokenId);
      return function.apply(requestUri, accessToken);
    } catch (HttpClientErrorException ex) {
      if (HttpStatus.UNAUTHORIZED == ex.getStatusCode()) {
        String refreshedAccessToken = oauth2TokenService.getRefreshedAccessToken(tokenId);
        return function.apply(requestUri, refreshedAccessToken);
      } else {
        throw ex;
      }
    }
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

    ResponseEntity<SlamPreferences> response = this.executeWithClient(requestUri, tokenId,
        (uri, accessToken) -> {
          HeadersBuilder<?> requestBuilder = RequestEntity.get(uri);
          if (accessToken != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION,
                String.format("%s %s", OAuth2AccessToken.BEARER_TYPE, accessToken));
          }
          return restTemplate.exchange(requestBuilder.build(), SlamPreferences.class);
        });

    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody();
    }
    throw new DeploymentException("Unable to find SLAM preferences. "
        + response.getStatusCode().toString() + " " + response.getStatusCode().getReasonPhrase());
  }

}
