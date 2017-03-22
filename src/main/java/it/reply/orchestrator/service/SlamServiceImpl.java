package it.reply.orchestrator.service;

import it.reply.orchestrator.dto.slam.SlamPreferences;
import it.reply.orchestrator.exception.service.DeploymentException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.RequestEntity.HeadersBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.social.support.URIBuilder;
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
  public String getUrl() {
    return url;
  }

  @Override
  public SlamPreferences getCustomerPreferences(String accessToken) {

    HeadersBuilder<?> request =
        RequestEntity.get(URIBuilder.fromUri(url.concat(preferences).concat("indigo-dc")).build());
    if (accessToken != null) {
      request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    }

    ResponseEntity<SlamPreferences> response =
        restTemplate.exchange(request.build(), SlamPreferences.class);
    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody();
    }
    throw new DeploymentException("Unable to find SLAM preferences. "
        + response.getStatusCode().toString() + " " + response.getStatusCode().getReasonPhrase());
  }

}
