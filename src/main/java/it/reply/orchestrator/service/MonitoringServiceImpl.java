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

import it.reply.monitoringpillar.domain.dsl.monitoring.pillar.wrapper.paas.PaaSMetric;
import it.reply.orchestrator.config.properties.MonitoringProperties;
import it.reply.orchestrator.dto.monitoring.MonitoringResponse;
import it.reply.orchestrator.exception.service.DeploymentException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

@Service
@EnableConfigurationProperties(MonitoringProperties.class)
public class MonitoringServiceImpl implements MonitoringService {

  @Autowired
  private MonitoringProperties monitoringProperties;

  @Autowired
  private RestTemplate restTemplate;

  @Override
  public List<PaaSMetric> getProviderData(String providerId) {

    URI requestUri = UriBuilder
        .fromUri(monitoringProperties.getUrl() + monitoringProperties.getProviderMetricsPath())
        .build(providerId)
        .normalize();

    ResponseEntity<MonitoringResponse> response = restTemplate
        .getForEntity(requestUri, MonitoringResponse.class);
    if (response.getStatusCode().is2xxSuccessful()) {
      // FIXME remove this ugliness
      return response
          .getBody()
          .getResult()
          .getGroups()
          .get(0)
          .getPaasMachines()
          .get(0)
          .getServices()
          .get(0)
          .getPaasMetrics();
    }

    throw new DeploymentException(
        "Error retrieving monitoring data for provider <" + providerId + ">");
  }

}
