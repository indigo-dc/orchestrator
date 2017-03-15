package it.reply.orchestrator.service.security;

/*
 * Copyright © 2015-2017 Santer Reply S.p.A.
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

import org.springframework.social.oauth2.AccessGrant;
import org.springframework.social.oauth2.OAuth2Template;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.stream.Collectors;

public class CustomOAuth2Template extends OAuth2Template {

  public static final String EXCHANGE_GRANT_TYPE =
      "urn:ietf:params:oauth:grant-type:token-exchange";

  private boolean useParametersForClientAuthentication;

  private String clientId;
  private String clientSecret;
  private String accessTokenUrl;

  /**
   * Creates a CustomOAuth2Template.
   * 
   * @param clientId
   *          the client id
   * @param clientSecret
   *          the client secret
   * @param authorizeUrl
   *          the authorization url
   * @param accessTokenUrl
   *          the token url
   */
  public CustomOAuth2Template(String clientId, String clientSecret, String authorizeUrl,
      String accessTokenUrl) {
    super(clientId, clientSecret, authorizeUrl, accessTokenUrl);
    this.accessTokenUrl = accessTokenUrl;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
  }

  @Override
  public void setUseParametersForClientAuthentication(
      boolean useParametersForClientAuthentication) {
    super.setUseParametersForClientAuthentication(useParametersForClientAuthentication);
    this.useParametersForClientAuthentication = useParametersForClientAuthentication;
  }

  /**
   * Exchange an access token for a new grant.
   * 
   * @param accessToken
   *          the access token to exchange
   * @param scopes
   *          the scope to request
   * @return the new grant
   */
  public AccessGrant exchangeToken(String accessToken, List<String> scopes) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    if (this.useParametersForClientAuthentication) {
      params.set("client_id", clientId);
      params.set("client_secret", clientSecret);
    }
    params.set("subject_token", accessToken);
    params.set("grant_type", EXCHANGE_GRANT_TYPE);
    params.set("scope", scopeFromList(scopes));

    return postForAccessGrant(accessTokenUrl, params);
  }

  /**
   * Use a refresh token to obtain a new grant.
   * 
   * @param refreshToken
   *          the refresh token to use
   * @param scopes
   *          the scope to request
   * @return the new grant
   */
  public AccessGrant refreshToken(String refreshToken, List<String> scopes) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.set("scope", scopeFromList(scopes));
    return this.refreshAccess(refreshToken, params);
  }

  public AccessGrant refreshToken(String refreshToken) {
    return this.refreshAccess(refreshToken, null);

  }

  private String scopeFromList(List<String> scopes) {
    return scopes.stream().collect(Collectors.joining(" "));
  }
}
