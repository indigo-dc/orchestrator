package it.reply.orchestrator.service;

import org.springframework.web.client.RestTemplate;

public interface IamService {

  public String getEndpoint(RestTemplate restTemplate, String url, String endpointName);

  public String getToken(RestTemplate restTemplate, String iamClientId, String iamClientSecret, String iamClientScopes, String iamTokenEndpoint);

  public String createClient(RestTemplate restTemplate, String iamRegistration, String uuid, String userEmail);

  public boolean deleteClient(String clientId, String iamUrl, String token);
  
  public boolean checkIam(RestTemplate restTemplate, String idpUrl);

}