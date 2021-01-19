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

package it.reply.orchestrator.service;

import it.reply.orchestrator.annotation.ServiceVersion;
import it.reply.orchestrator.config.properties.SlamProperties;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dto.slam.SlamPreferences;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.security.OAuth2TokenService;

import java.net.URI;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.RequestEntity.HeadersBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@ServiceVersion("v1")
public class SlamServiceV1Impl implements SlamService {

  private SlamProperties slamProperties;

  private OAuth2TokenService oauth2TokenService;

  private RestTemplate restTemplate;

  /**
   * Creates a new SlamServiceImpl.
   *
   * @param slamProperties
   *          the SlamProperties to use
   * @param oauth2TokenService
   *          the OAuth2TokenService to use
   * @param restTemplateBuilder
   *          the RestTemplateBuilder to use
   */
  public SlamServiceV1Impl(
      SlamProperties slamProperties, OAuth2TokenService oauth2TokenService,
      RestTemplateBuilder restTemplateBuilder) {
    this.slamProperties = slamProperties;
    this.oauth2TokenService = oauth2TokenService;
    this.restTemplate = restTemplateBuilder.build();
  }

  @Override
  public SlamPreferences getCustomerPreferences(OidcTokenId tokenId) {

    String slamCustomer = oauth2TokenService.getOrganization(tokenId);

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(slamProperties.getUrl() + slamProperties.getCustomerPreferencesPath())
        .buildAndExpand(slamCustomer)
        .normalize()
        .toUri();

    try {
      return oauth2TokenService.executeWithClientForResult(tokenId,
          accessToken -> {
            HeadersBuilder<?> requestBuilder = RequestEntity.get(requestUri);
            if (accessToken != null) {
              requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            }
            return restTemplate.exchange(requestBuilder.build(), SlamPreferences.class);
          }, OAuth2TokenService.restTemplateTokenRefreshEvaluator).getBody();
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error fetching SLA for customer <" + slamCustomer + "> from SLAM.", ex);
    }
  }

}
