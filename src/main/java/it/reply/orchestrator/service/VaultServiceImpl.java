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

import java.io.BufferedReader;
import java.io.IOException;
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
   */
  @SuppressWarnings("unchecked")
  public String retrieveToken(String accessToken) {
    String exmessage = "Unable to retrieve token for Vault:";
    String token = "";
    VaultEndpoint endpoint = VaultEndpoint.create(
        vaultProperties.getUrl(),
        vaultProperties.getPort());
    URI uri = endpoint.createUri("auth/jwt/login");

    CloseableHttpClient httpclient = HttpClients.createDefault();
    CloseableHttpResponse response = null;
    BufferedReader reader = null;
    HttpPost httpPost = new HttpPost(uri.toString());
    String json = "{\"jwt\":\"" + accessToken + "\"}";
    HttpEntity stringEntity = new StringEntity(json,ContentType.APPLICATION_JSON);
    httpPost.setEntity(stringEntity);
    
    try {
      response = httpclient.execute(httpPost);            

      StringBuilder responseStrBuilder = new StringBuilder();

      reader = new BufferedReader(
          new InputStreamReader((response.getEntity().getContent())));

      String inputStr;
      while ((inputStr = reader.readLine()) != null) {
        responseStrBuilder.append(inputStr);
      }
      
      final int status = response.getStatusLine().getStatusCode();

      if (status == 200) {

        HashMap<String,Object> result =
            new ObjectMapper().readValue(responseStrBuilder.toString(), HashMap.class);         

        HashMap<String,Object> auth = (HashMap<String,Object>) result.get("auth");


        token = (String) auth.get("client_token");
      } else {
        String message = responseStrBuilder.toString();
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
    } catch (IOException e) {      
      throw new VaultException(String.format("%s %s.", exmessage, e.getMessage()));
    }     
    finally {
      if (reader != null) {
        try {
          reader.close();
        } 
        catch(IOException e) {          
        }
      }
      if (response != null) {
        try {
          response.close();
        } 
        catch(IOException e) {          
        }
      }
      try {
        httpclient.close();
      }
      catch(IOException e) {        
      }
    }

    return token;
  }

}
