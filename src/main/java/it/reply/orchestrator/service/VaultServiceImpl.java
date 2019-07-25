/*
 * Copyright © 2016-2019 I.N.F.N.
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.vault.VaultException;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

@Service
@EnableConfigurationProperties(VaultProperties.class)
public class VaultServiceImpl implements VaultService {

  @Autowired
  private VaultProperties vaultProperties;

  private String token;

  public VaultServiceImpl(VaultProperties vaultProperties) {
    this.vaultProperties = vaultProperties;
    this.token = "";
  }

  private VaultTemplate getTemplate() {
    return new VaultTemplate(VaultEndpoint.create(
        vaultProperties.getUrl(),
        vaultProperties.getPort()),
        new TokenAuthentication(token));       
  }

  public VaultResponse writeSecret(String path, Object secret) {
    return getTemplate().write(path, secret);
  }

  public <T> T readSecret(String path, Class<T> type) {        
    return getTemplate().read(path, type).getData();
  }

  public Map<String,Object> readSecret(String path) {
    return getTemplate().read(path).getData();
  }

  public void deleteSecret(String path) {
    getTemplate().delete(path);
  }

  public List<String> listSecrets(String path) {
    return getTemplate().list(path);
  }

  /**
   * Retrieve Vault token starting from access token.
   */
  @SuppressWarnings("unchecked")
  public VaultService retrieveToken(String accessToken) {
    String token = "";
    try {
      VaultEndpoint endpoint = VaultEndpoint.create(
          vaultProperties.getUrl(),
          vaultProperties.getPort());
      URI uri = endpoint.createUri("auth/jwt/login");

      CloseableHttpClient httpclient = HttpClients.createDefault();
      HttpPost httpPost = new HttpPost(uri.toString());
      String json = "{\"jwt\":\"" + accessToken + "\"}";
      HttpEntity stringEntity = new StringEntity(json,ContentType.APPLICATION_JSON);
      httpPost.setEntity(stringEntity);
      CloseableHttpResponse response = httpclient.execute(httpPost);            

      StringBuilder responseStrBuilder = new StringBuilder();

      BufferedReader reader = new BufferedReader(
          new InputStreamReader((response.getEntity().getContent())));

      String inputStr;
      while ((inputStr = reader.readLine()) != null) {
        responseStrBuilder.append(inputStr);
      }
      
      final int status = response.getStatusLine().getStatusCode();

      reader.close();
      response.close();
      httpclient.close();

      if (status == 200) {

        HashMap<String,Object> result =
            new ObjectMapper().readValue(responseStrBuilder.toString(), HashMap.class);         

        HashMap<String,Object> auth = (HashMap<String,Object>) result.get("auth");


        token = (String) auth.get("client_token");
      } else {
        String message = responseStrBuilder.toString();
        if (status == 400) {
          if (message.toLowerCase().contains("token is expired")) {
            throw new VaultTokenExpiredException("Unable to retrieve token for Vault:"
                + " accessToken is expired");
          }
        }
        throw new VaultException(String.format("Unable to retrieve token for Vault:"
            + " %d (%s).", status, message));
      }        
    } catch (VaultException ve) {            
      throw ve;
    } catch (Exception e) {
      throw new VaultException(String.format("Unable to retrieve token for Vault:"
          + " %s.", e.getMessage()));
    }     


    this.token = token;
    return this;
  }

  public VaultService setVaultToken(String token) {
    this.token = token;
    return this;
  }

  public String getVaultToken() {
    return token;
  }
}
