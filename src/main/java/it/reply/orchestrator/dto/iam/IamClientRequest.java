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

package it.reply.orchestrator.dto.iam;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class IamClientRequest {
  private List<String> redirectUris;
  private String clientName;
  private List<String> contacts;
  private String tokenEndpointAuthMethod;
  private String scope;
  private List<String> grantTypes;
  private List<String> responseTypes;

  /**
   * Constructor of IAMClientRequest, used to send a request to IAM to create a client.
   */
  public IamClientRequest(List<String> redirectUris, String clientName, List<String> contacts,
      String tokenEndpointAuthMethod, String scope, List<String> grantTypes,
      List<String> responseTypes) {
    this.redirectUris = redirectUris;
    this.clientName = clientName;
    this.contacts = contacts;
    this.tokenEndpointAuthMethod = tokenEndpointAuthMethod;
    this.scope = scope;
    this.grantTypes = grantTypes;
    this.responseTypes = responseTypes;
  }

  // Getter and setter for redirectUris

  public List<String> getRedirectUris() {
    return redirectUris;
  }

  public void setRedirectUris(List<String> redirectUris) {
    this.redirectUris = redirectUris;
  }

  // Getter and setter for clientName

  public String getClientName() {
    return clientName;
  }

  public void setClientName(String clientName) {
    this.clientName = clientName;
  }

  // Getter and setter for contacts

  public List<String> getContacts() {
    return contacts;
  }

  public void setContacts(List<String> contacts) {
    this.contacts = contacts;
  }

  // Getter and setter for tokenEndpointAuthMethod

  public String getTokenEndpointAuthMethod() {
    return tokenEndpointAuthMethod;
  }

  public void setTokenEndpointAuthMethod(String tokenEndpointAuthMethod) {
    this.tokenEndpointAuthMethod = tokenEndpointAuthMethod;
  }

  // Getter and setter for scope

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  // Getter and setter for grantTypes

  public List<String> getGrantTypes() {
    return grantTypes;
  }

  public void setGrantTypes(List<String> grantTypes) {
    this.grantTypes = grantTypes;
  }

  // Getter and setter for responseTypes

  public List<String> getResponseTypes() {
    return responseTypes;
  }

  public void setResponseTypes(List<String> responseTypes) {
    this.responseTypes = responseTypes;
  }
}
