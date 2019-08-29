/*
 * Copyright © 2019 I.N.F.N.
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

import com.fasterxml.jackson.databind.ObjectMapper;

import it.reply.orchestrator.config.properties.VaultProperties;
import it.reply.orchestrator.exception.VaultTokenExpiredException;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.vault.VaultException;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@EnableConfigurationProperties(VaultProperties.class)
public class VaultServiceImpl implements VaultService {

  @Autowired
  private VaultProperties vaultProperties;

  public VaultServiceImpl(VaultProperties vaultProperties) {
    this.vaultProperties = vaultProperties;
  }

  private VaultTemplate getTemplate(String token) {
    return new VaultTemplate(VaultEndpoint.create(
        vaultProperties.getUrl(),
        vaultProperties.getPort()),
        new TokenAuthentication(token));       
  }

  public VaultResponse writeSecret(String token, String path, Object secret) {
    return getTemplate(token).write(path, secret);
  }

  public <T> T readSecret(String token, String path, Class<T> type) {        
    return getTemplate(token).read(path, type).getData();
  }

  public Map<String,Object> readSecret(String token, String path) {
    return getTemplate(token).read(path).getData();
  }

  public void deleteSecret(String token, String path) {
    getTemplate(token).delete(path);
  }

  public List<String> listSecrets(String token, String path) {
    return getTemplate(token).list(path);
  }

  /**
   * Retrieve the vault token from the IAM token.
   * @throws IOException when IO operations fail
   */
  @SuppressWarnings("unchecked")
  public String retrieveToken(String accessToken) throws IOException {
    String exmessage = "Unable to retrieve token for Vault:";
    String token = "";
    VaultEndpoint endpoint = VaultEndpoint.create(
        vaultProperties.getUrl(),
        vaultProperties.getPort());
    URI uri = endpoint.createUri("auth/jwt/login");

    RestTemplate restTemplate = new RestTemplate();
    
    String json = "{\"jwt\":\"" + accessToken + "\"}";
    
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
    HttpEntity<String> stringEntity = new HttpEntity<String>(json, headers);
     
    try {
      ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST,
          stringEntity, String.class);
      
      final int status = response.getStatusCodeValue();

      if (status == 200) {
        HashMap<String,Object> result =
            new ObjectMapper().readValue(response.getBody(), HashMap.class);         

        HashMap<String,Object> auth = (HashMap<String,Object>) result.get("auth");

        token = (String) auth.get("client_token");
      } else {
        String message = response.getBody();
        if (status == 400) {
          if (message.toLowerCase().contains("token is expired")) {
            throw new VaultTokenExpiredException(
                String.format("%s accessToken is expired",exmessage));
          }
        }
        throw new VaultException(String.format("%s %d (%s).", exmessage, status, message));
      }        
    } catch (VaultException ve) {            
      throw ve;
    } catch (RestClientException e) {      
      throw new VaultException(String.format("%s %s.", exmessage, e.getMessage()));
    }

    return token;
  }

}
