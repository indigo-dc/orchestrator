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

import it.reply.monitoringpillar.domain.dsl.monitoring.pillar.wrapper.paas.Group;
import it.reply.monitoringpillar.domain.dsl.monitoring.pillar.wrapper.paas.MonitoringWrappedResponsePaas;
import it.reply.orchestrator.config.properties.MonitoringProperties;
import it.reply.orchestrator.dto.monitoring.MonitoringResponse;
import it.reply.orchestrator.exception.service.DeploymentException;

import java.net.URI;
import java.util.Optional;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@EnableConfigurationProperties(MonitoringProperties.class)
public class MonitoringServiceImpl implements MonitoringService {

  private MonitoringProperties monitoringProperties;

  private RestTemplate restTemplate;

  public MonitoringServiceImpl(MonitoringProperties monitoringProperties,
      RestTemplateBuilder restTemplateBuilder) {
    this.monitoringProperties = monitoringProperties;
    this.restTemplate = restTemplateBuilder.build();
  }

  @Override
  public Group getProviderData(String providerId) {

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(monitoringProperties.getUrl() + monitoringProperties.getProviderMetricsPath())
        .buildAndExpand(providerId)
        .normalize()
        .toUri();

    try {
      ResponseEntity<MonitoringResponse> response =
          restTemplate.getForEntity(requestUri, MonitoringResponse.class);
      return Optional
          .ofNullable(response.getBody().getResult())
          .map(MonitoringWrappedResponsePaas::getGroups)
          .flatMap(groups -> groups.stream().findFirst())
          .orElseThrow(() -> new DeploymentException(
              "No metrics available for provider <" + providerId + ">"));
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error fetching monitoring data for provider <" + providerId + ">", ex);
    }
  }

}
