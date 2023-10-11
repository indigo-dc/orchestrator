package it.reply.orchestrator.service;

import org.springframework.web.client.RestTemplate;

import java.util.List;
import it.reply.orchestrator.WellKnownResponse;

public interface IamService {

  public String getOrchestratorScopes();

  public WellKnownResponse getWellKnown(RestTemplate restTemplate, String issuer);

  public String getEndpoint(RestTemplate restTemplate, String url, String endpointName);

  public String getTokenClientCredentials(RestTemplate restTemplate, String iamClientId, String iamClientSecret, String iamClientScopes, String iamTokenEndpoint);

  public String createClient(RestTemplate restTemplate, String iamRegistration, String uuid, String userEmail, String scopes);

  public boolean deleteClient(String clientId, String iamUrl, String token);

  public boolean checkIam(RestTemplate restTemplate, String idpUrl);

}