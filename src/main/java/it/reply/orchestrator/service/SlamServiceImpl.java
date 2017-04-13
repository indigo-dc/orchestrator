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

package it.reply.orchestrator.service;

import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.dal.entity.OidcEntity;
import it.reply.orchestrator.dal.entity.OidcRefreshToken;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dal.repository.OidcTokenRepository;
import it.reply.orchestrator.dto.slam.SlamPreferences;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.security.OAuth2TokenService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.RequestEntity.HeadersBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.social.support.URIBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Optional;
import java.util.function.Function;

@Service
@PropertySource("classpath:slam/slam.properties")
public class SlamServiceImpl implements SlamService {

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private OidcTokenRepository tokenRepository;

  @Autowired
  private OidcProperties oidcProperties;

  @Autowired
  private OAuth2TokenService oauth2TokenService;

  @Value("${slam.url}")
  private String url;

  @Value("${preferences}")
  private String preferences;

  @Override
  public String getUrl() {
    return url;
  }

  protected <R> R executeWithClient(Function<RequestEntity<?>, R> function, URI requestUri,
      OidcTokenId tokenId) {
    if (!oidcProperties.isEnabled()) {
      return function.apply(RequestEntity.get(requestUri).build());
    }
    try {
      String accessToken =
          oauth2TokenService.getAccessToken(tokenId, OAuth2TokenService.REQUIRED_SCOPES);
      HeadersBuilder<?> request =
          RequestEntity.get(requestUri).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

      return function.apply(request.build());
    } catch (HttpClientErrorException ex) {
      if (Optional.ofNullable(ex.getStatusCode())
          .map(code -> HttpStatus.UNAUTHORIZED == code)
          .orElse(false)) {
        String accessToken = oauth2TokenService
            .refreshAccessToken(tokenId, OAuth2TokenService.REQUIRED_SCOPES).getAccessToken();
        HeadersBuilder<?> request = RequestEntity.get(requestUri).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + accessToken);
        return function.apply(request.build());
      } else {
        throw ex;
      }
    }
  }

  @Override
  public SlamPreferences getCustomerPreferences(OidcTokenId tokenId) {

    String slamCustomer = Optional.ofNullable(tokenId)
        .flatMap(tokenRepository::findByOidcTokenId)
        .map(OidcRefreshToken::getEntity)
        .map(OidcEntity::getOrganization)
        .orElse("indigo-dc");

    URI requestUri = URIBuilder.fromUri(url.concat(preferences).concat(slamCustomer)).build();

    ResponseEntity<SlamPreferences> response = this.executeWithClient(
        request -> restTemplate.exchange(request, SlamPreferences.class), requestUri, tokenId);

    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody();
    }
    throw new DeploymentException("Unable to find SLAM preferences. "
        + response.getStatusCode().toString() + " " + response.getStatusCode().getReasonPhrase());
  }

}
