package it.reply.orchestrator.service;

import it.reply.orchestrator.dto.ranker.CloudProviderRankerRequest;
import it.reply.orchestrator.dto.ranker.RankedCloudProvider;
import it.reply.orchestrator.exception.service.DeploymentException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
public class CloudProviderRankerServiceImpl implements CloudProviderRankerService {

  @Value("${cloud-provider-ranker.url}")
  private String url;

  @Autowired
  private RestTemplate restTemplate;

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public List<RankedCloudProvider>
      getProviderRanking(CloudProviderRankerRequest cloudProviderRankerRequest) {

    HttpEntity<CloudProviderRankerRequest> entity =
        new HttpEntity<CloudProviderRankerRequest>(cloudProviderRankerRequest);

    ResponseEntity<List<RankedCloudProvider>> response = restTemplate.exchange(url, HttpMethod.POST,
        entity, new ParameterizedTypeReference<List<RankedCloudProvider>>() {
        });
    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody();
    }

    throw new DeploymentException("Error retrieving cloud provider ranking data for request <"
        + cloudProviderRankerRequest + ">");
  }
}
