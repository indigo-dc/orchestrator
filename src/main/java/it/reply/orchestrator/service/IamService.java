/*
 * Copyright © 2015-2021 I.N.F.N.
 * Copyright © 2015-2020 Santer Reply S.p.A.
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

import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dto.iam.WellKnownResponse;
import java.util.Map;
import java.util.Set;
import org.springframework.web.client.RestTemplate;

public interface IamService {

  public String getOrchestratorScopes();

  /**
   * Create a WellKnownResponse object for an IAM idp containing registration_endpoint,
   * token_endpoint, and scopes_supported.
   *
   * @param restTemplate object used to make HTTP requests
   * @param issuer the identity provider
   * @return the WellKnownResponse object
   */
  public WellKnownResponse getWellKnown(RestTemplate restTemplate, String issuer);

  /**
   * Get a token with client credentials as grant type.
   *
   * @param restTemplate object used to make HTTP requests
   * @param iamClientId client_id
   * @param iamClientSecret client_secret
   * @param iamClientScopes scopes to ask in the request
   * @param iamTokenEndpoint IAM token endpoint
   * @return the token with client credentials as grant type
   */
  public String getTokenClientCredentials(RestTemplate restTemplate, String iamClientId,
      String iamClientSecret, String iamClientScopes, String iamTokenEndpoint);

  /**
   * Create an IAM client setting the minimal information, in addition to the mail field.
   * As output it gives the client_id and registration_access_token of the created client.
   *
   * @param restTemplate object used to make HTTP requests
   * @param iamRegistration registration endpoint used to create a client
   * @param uuid uuid of the deployment, used to set the client name
   * @param userEmail user email, used to set the contacts field of the client
   * @param scopes scopes to set for the new client
   * @return a map of client_id:registration_access_token
   */
  public Map<String, String> createClient(RestTemplate restTemplate, String iamRegistration,
      String uuid, String userEmail, String scopes);

  /**
   * Delete a client.
   *
   * @param restTemplate object used to make HTTP requests
   * @param clientId client_id
   * @param iamUrl IAM endpoint to contact for the client deletion
   * @param token the registration_access_token used for the deletion
   * @return true if the deletion has been successful
   */
  public boolean deleteClient(RestTemplate restTemplate, String clientId, String iamUrl,
      String token);

  /**
   * Delete all the clients stored in resources.
   *
   * @param restTemplate object used to make HTTP requests
   * @param resources resources linked to a given deployment
   */
  public void deleteAllClients(RestTemplate restTemplate, Map<Boolean, Set<Resource>> resources);

  /**
   * Assign the ownership of a client.
   *
   * @param restTemplate object used to make HTTP requests
   * @param clientId client_id
   * @param iamUrl IAM Url
   * @param accountId the id of the owner to be assigned to the client
   * @param token the token with client credentials as grant type using the orchestrator client
   */
  public void assignOwnership(RestTemplate restTemplate, String clientId, String iamUrl,
      String accountId, String token);

  /**
   * Check if a given idp ia an IAM.
   *
   * @param restTemplate object used to make HTTP requests
   * @param idpUrl Url to be checked if it is an IAM or not
   * @return true if the idpurl is an IAM, otherwise false
   */
  public boolean checkIam(RestTemplate restTemplate, String idpUrl);

  /**
   * Get information about a client.
   *
   * @param restTemplate object used to make HTTP requests
   * @param clientId client_id
   * @param iamUrl IAM Url
   * @param token the token to use
   * @return the json obtained as output from the HTPP request
   */
  public String getInfoIamClient(RestTemplate restTemplate, String clientId, String iamUrl,
      String token);

  /**
   * Update information about a client.
   *
   * @param restTemplate object used to make HTTP requests
   * @param clientId client_id
   * @param iamUrl IAM Url
   * @param token the token to use
   * @param jsonUpdated the updated json obtained as output
   * @return the json with the updated info about the client
   */
  public String updateClient(RestTemplate restTemplate, String clientId, String iamUrl,
      String token, String jsonUpdated);

}
