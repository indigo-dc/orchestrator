package it.reply.orchestrator.service;

import org.springframework.web.client.RestTemplate;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dto.iam.WellKnownResponse;

import java.util.Map;
import java.util.Set;

public interface IamService {

  public String getOrchestratorScopes();

  public WellKnownResponse getWellKnown(RestTemplate restTemplate, String issuer);

  public String getTokenClientCredentials(RestTemplate restTemplate, String iamClientId, String iamClientSecret,
      String iamClientScopes, String iamTokenEndpoint);

  public Map<String, String> createClient(RestTemplate restTemplate, String iamRegistration, String uuid,
      String userEmail, String scopes);

  public boolean deleteClient(String clientId, String iamUrl, String token);

  public void deleteAllClients(RestTemplate restTemplate, Map<Boolean, Set<Resource>> resources);

  public boolean checkIam(RestTemplate restTemplate, String idpUrl);

  public String getInfoIamClient(String clientId, String iamUrl, String token);

  public String updateClient(String clientId, String iamUrl, String token, String jsonUpdated);

  public void assignOwnership(String clientId, String iamUrl, String accountId, String token);

}
