package it.reply.orchestrator.service;

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
import it.reply.monitoringpillar.domain.dsl.monitoring.pillar.wrapper.paas.MonitoringWrappedResponsePaas;
import it.reply.orchestrator.dto.monitoring.MonitoringResponse;
import it.reply.orchestrator.exception.service.DeploymentException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@PropertySource("classpath:monitoring/monitoring.properties")
public class MonitoringServiceImpl implements MonitoringService {

  @Value("${wrapper.url}")
  private String url;

  @Autowired
  private RestTemplate restTemplate;

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public MonitoringWrappedResponsePaas getProviderData(String providerId) {

    ResponseEntity<MonitoringResponse> response =
        restTemplate.getForEntity(url.concat(providerId), MonitoringResponse.class);
    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody().getResult();
    }

    throw new DeploymentException(
        "Error retrieving monitoring data for provider <" + providerId + ">");
  }

}
