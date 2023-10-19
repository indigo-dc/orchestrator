package it.reply.orchestrator.service;

import org.springframework.web.client.RestTemplate;
import it.reply.orchestrator.dal.entity.Resource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import it.reply.orchestrator.WellKnownResponse;

public interface IamService {

  public String getOrchestratorScopes();

  public WellKnownResponse getWellKnown(RestTemplate restTemplate, String issuer);

  public String getEndpoint(RestTemplate restTemplate, String url, String endpointName);

  public String getTokenClientCredentials(RestTemplate restTemplate, String iamClientId, String iamClientSecret, String iamClientScopes, String iamTokenEndpoint);

  public Map<String,String> createClient(RestTemplate restTemplate, String iamRegistration, String uuid, String userEmail, String scopes);

  public boolean deleteClient(String clientId, String iamUrl, String token);

  public boolean deleteAllClients(RestTemplate restTemplate, Map<Boolean, Set<Resource>> resources);

  public boolean checkIam(RestTemplate restTemplate, String idpUrl);

  public String getInfoIamClient(String clientId, String iamUrl, String token);

  public String updateClient(String clientId, String iamUrl, String token, String jsonUpdated);

  public boolean assignOwnership(String clientId, String iamUrl, String accountId, String token);

}