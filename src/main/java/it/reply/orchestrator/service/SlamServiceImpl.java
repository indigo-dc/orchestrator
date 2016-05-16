package it.reply.orchestrator.service;

import it.reply.orchestrator.dto.slam.SlamPreferences;
import it.reply.orchestrator.exception.service.DeploymentException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@PropertySource("classpath:slam/slam.properties")
public class SlamServiceImpl implements SlamService {

  @Autowired
  private RestTemplate restTemplate;

  @Value("${slam.url}")
  private String url;

  @Value("${preferences}")
  private String preferences;

  @Override
  public SlamPreferences getCustomerPreferences() {

    ResponseEntity<SlamPreferences> response = restTemplate
        .getForEntity(url.concat(preferences).concat("/indigo-demo"), SlamPreferences.class);
    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody();
    }
    throw new DeploymentException("Unable to find SLAM preferences. "
        + response.getStatusCode().toString() + " " + response.getStatusCode().getReasonPhrase());
  }

}
