package it.reply.orchestrator.service;

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
