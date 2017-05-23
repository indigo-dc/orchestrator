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

import it.reply.orchestrator.config.properties.CprProperties;
import it.reply.orchestrator.dto.ranker.CloudProviderRankerRequest;
import it.reply.orchestrator.dto.ranker.RankedCloudProvider;
import it.reply.orchestrator.exception.service.DeploymentException;

import lombok.AllArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@PropertySource("classpath:cloud-provider-ranker/cloud-provider-ranker.properties")
@AllArgsConstructor(onConstructor = @__({ @Autowired }))
@EnableConfigurationProperties(CprProperties.class)
public class CloudProviderRankerServiceImpl implements CloudProviderRankerService {

  private CprProperties cprProperties;

  private RestTemplate restTemplate;

  @Override
  public List<RankedCloudProvider> getProviderRanking(
      CloudProviderRankerRequest cloudProviderRankerRequest) {

    HttpEntity<CloudProviderRankerRequest> entity = new HttpEntity<>(cloudProviderRankerRequest);

    ResponseEntity<List<RankedCloudProvider>> response =
        restTemplate.exchange(cprProperties.getUrl(), HttpMethod.POST, entity,
            new ParameterizedTypeReference<List<RankedCloudProvider>>() {
            });
    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody();
    }

    throw new DeploymentException("Error retrieving cloud provider ranking data for request <"
        + cloudProviderRankerRequest + ">");
  }
}
