/*
 * Copyright © 2015-2018 Santer Reply S.p.A.
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

package it.reply.orchestrator.service.security;

import it.reply.orchestrator.dto.security.AccessGrant;
import it.reply.orchestrator.dto.security.TokenIntrospectionResponse;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.mitre.oauth2.model.ClientDetailsEntity.AuthMethod;
import org.mitre.oauth2.model.RegisteredClient;
import org.mitre.openid.connect.config.ServerConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.RequestEntity.BodyBuilder;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.Base64Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

public class CustomOAuth2Template {

  private ServerConfiguration serverConfiguration;
  private RegisteredClient clientConfiguration;

  private RestTemplate restTemplate;

  /**
   * * Creates a new OAuth2Template.
   * 
   * @param serverConfiguration
   *          the authorization server configuration
   * @param clientConfiguration
   *          the client configuration
   * @param builder
   *          the RestTemplate builder
   */
  public CustomOAuth2Template(ServerConfiguration serverConfiguration,
      RegisteredClient clientConfiguration, RestTemplateBuilder builder) {
    this.serverConfiguration = serverConfiguration;
    this.clientConfiguration = clientConfiguration;

    builder.messageConverters(new FormHttpMessageConverter(), new FormHttpMessageConverter(),
        new MappingJackson2HttpMessageConverter());
    this.restTemplate = builder.build();
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
  public AccessGrant exchangeToken(String accessToken, Set<String> scopes) {
    MultiValueMap<String, String> params =
        generateParams(clientConfiguration.getTokenEndpointAuthMethod());
    params.set("subject_token", accessToken);
    params.set("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
    return postForAccessGrant(params, scopes);
  }

  /**
   * Get a new access token (and maybe a refresh token) from an existing refresh token.
   * 
   * @param refreshToken
   *          the refresh token to use
   * @param scopes
   *          the scope to request
   * @return the new grant
   */
  public AccessGrant refreshToken(String refreshToken, Set<String> scopes) {
    MultiValueMap<String, String> params =
        generateParams(clientConfiguration.getTokenEndpointAuthMethod());
    params.set("refresh_token", refreshToken);
    params.set("grant_type", "refresh_token");
    return postForAccessGrant(params, scopes);
  }

  /**
   * Introspect an access token or a refresh token.
   * 
   * @param token
   *          the token
   * @return the introspection response
   */
  public TokenIntrospectionResponse introspectToken(String token) {
    MultiValueMap<String, String> params = generateParams(AuthMethod.SECRET_BASIC);
    params.set("token", token);
    return postForObject(serverConfiguration.getIntrospectionEndpointUri(), AuthMethod.SECRET_BASIC,
        params, TokenIntrospectionResponse.class);
  }

  protected <T> T postForObject(String url, AuthMethod authMethod,
      MultiValueMap<String, String> params,
      Class<T> responseClass) {
    BodyBuilder request = RequestEntity.post(URI.create(url));
    if (AuthMethod.SECRET_BASIC.equals(authMethod)) {
      request.header(HttpHeaders.AUTHORIZATION,
          "Basic " + Base64Utils.encodeToString(
              (clientConfiguration.getClientId() + ":" + clientConfiguration.getClientSecret())
                  .getBytes(Charset.forName("UTF-8"))));
    }
    return restTemplate.exchange(request.body(params), responseClass).getBody();
  }

  protected AccessGrant postForAccessGrant(MultiValueMap<String, String> params,
      Set<String> scopes) {
    params.set("scope", scopeFromCollection(scopes));
    return validateAccessGrantScopes(postForObject(serverConfiguration.getTokenEndpointUri(),
        clientConfiguration.getTokenEndpointAuthMethod(), params, AccessGrant.class), scopes);
  }

  private MultiValueMap<String, String> generateParams(AuthMethod authMethod) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    if (AuthMethod.SECRET_POST.equals(authMethod)) {
      params.set("client_id", clientConfiguration.getClientId());
      params.set("client_secret", clientConfiguration.getClientSecret());
    }
    return params;
  }

  private String scopeFromCollection(Collection<String> scopes) {
    return scopes
        .stream()
        .filter(StringUtils::hasText)
        .map(String::trim)
        .collect(Collectors.joining(" "));
  }

  private AccessGrant validateAccessGrantScopes(AccessGrant grant, Set<String> scopes) {
    if (!scopes.isEmpty() && !grant.getScopes().containsAll(scopes)) {
      throw new RuntimeException("Not all the required scopes have been granted");
    } else {
      return grant;
    }
  }
}
