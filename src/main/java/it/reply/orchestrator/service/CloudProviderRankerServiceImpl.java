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

import it.reply.orchestrator.config.properties.CprProperties;
import it.reply.orchestrator.dto.ranker.CloudProviderRankerRequest;
import it.reply.orchestrator.dto.ranker.RankedCloudService;
import it.reply.orchestrator.exception.service.DeploymentException;

import java.net.URI;
import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@EnableConfigurationProperties(CprProperties.class)
public class CloudProviderRankerServiceImpl implements CloudProviderRankerService {

  private static final ParameterizedTypeReference<List<RankedCloudService>> RESPONSE_TYPE =
      new ParameterizedTypeReference<List<RankedCloudService>>() {
      };

  private CprProperties cprProperties;

  private RestTemplate restTemplate;

  public CloudProviderRankerServiceImpl(CprProperties cprProperties,
      RestTemplateBuilder restTemplateBuilder) {
    this.cprProperties = cprProperties;
    this.restTemplate = restTemplateBuilder.build();
  }

  @Override
  public List<RankedCloudService> getProviderServicesRanking(
      CloudProviderRankerRequest cloudProviderRankerRequest) {

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(cprProperties.getUrl() + cprProperties.getRankPath())
        .build()
        .normalize()
        .toUri();

    HttpEntity<CloudProviderRankerRequest> entity = new HttpEntity<>(cloudProviderRankerRequest);

    try {
      return restTemplate.exchange(requestUri, HttpMethod.POST, entity, RESPONSE_TYPE).getBody();
    } catch (RestClientException ex) {
      throw new DeploymentException("Error retrieving cloud provider ranking data", ex);
    }
  }
}
