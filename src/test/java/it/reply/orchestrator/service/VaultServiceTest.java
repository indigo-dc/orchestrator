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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.support.VaultTokenResponse;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.reply.orchestrator.config.properties.VaultProperties;
import it.reply.orchestrator.dal.entity.OidcEntityId;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.exception.VaultJwtTokenExpiredException;
import it.reply.orchestrator.function.ThrowingFunction;
import it.reply.orchestrator.function.ThrowingFunction2p;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.orchestrator.utils.JsonUtils;
import junitparams.JUnitParamsRunner;

@RunWith(JUnitParamsRunner.class)
@RestClientTest(VaultService.class)
public class VaultServiceTest {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @MockBean
  private OAuth2TokenService oauth2TokenService;

  @Autowired
  private VaultService vaultService;

  @Autowired
  private VaultProperties vaultProperties;

  @Autowired
  private MockRestServiceServer mockServer;

  private ObjectMapper objectMapper;

  private static final String defaultVaultEndpoint = "https://default.vault.com:8200";
  private static final String vaultToken = "s.DQSf698xaTFLtBCY9bG2QdhI";
  private static final String validAccessToken = "eyJhbGciOiJub25lIn0.eyJqdGkiOiI0Y2IyNGQ1Ny1kMzRkLTQxZDQtYmZiYy04NzFiN2I0MDRjZDAifQ.";
  private static final String expiredAccessToken = "eyJhbGciOiJub25lIn0.fyJqdGkiOiI0Y2IyNGQ1Ny1kMzRkLTQxZDQtYmZiYy04NzFiN2I0MDRjZDAifQ.";
  private static final String badAccessToken = "foo";

  private static final String validsubject = "55555555-6666-7777-8888-999999999990";
  private static final String expiredsubject = "55555555-6666-7777-8888-999999999991";

  private static final OidcTokenId oidcTokenId = new OidcTokenId();
  private static final OidcTokenId expiredOidcTokenId = new OidcTokenId();

  private URI defaultVaultUri;

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Before
  public void setup() throws Exception {
    objectMapper = new ObjectMapper();
    defaultVaultUri = new URI(defaultVaultEndpoint);

    OidcEntityId validoidc = new OidcEntityId();
    validoidc.setSubject(validsubject);
    oidcTokenId.setOidcEntityId(validoidc);

    OidcEntityId expiredidc = new OidcEntityId();
    expiredidc.setSubject(expiredsubject);
    expiredOidcTokenId.setOidcEntityId(expiredidc);

    when(oauth2TokenService
        .executeWithClientForResult(eq(oidcTokenId), any(), any()))
            .then(a -> ((ThrowingFunction) a.getArguments()[1]).apply(validAccessToken));

    when(oauth2TokenService
        .executeUriWithClientForResult(eq(defaultVaultUri), eq(oidcTokenId), any(), any()))
            .then(a -> ((ThrowingFunction2p) a.getArguments()[2]).apply(defaultVaultUri, validAccessToken));

    when(oauth2TokenService
        .executeWithClientForResult(eq(expiredOidcTokenId), any(), any()))
            .then(a -> ((ThrowingFunction) a.getArguments()[1]).apply(expiredAccessToken));

    when(oauth2TokenService
        .executeUriWithClientForResult(eq(defaultVaultUri), eq(expiredOidcTokenId), any(), any()))
            .then(a -> ((ThrowingFunction2p) a.getArguments()[2]).apply(defaultVaultUri, expiredAccessToken));

  }

  private URI buildEndpoint() {
    VaultEndpoint endpoint = VaultEndpoint.from(
        vaultProperties.getUri());
      URI uri = endpoint.createUri("auth/jwt/login");
      return uri;
  }

  private Map<String, String> buildLogin(String token) {
    Map<String, String> login = new HashMap<>();
    login.put("jwt", token);
    return login;
  }

  private VaultTokenResponse buildVaultResponse() {
    Map<String, Object> auth = new HashMap<String, Object>();
    auth.put("client_token", vaultToken);
    VaultTokenResponse response = new VaultTokenResponse();
    response.setAuth(auth);
    return response;
  }

  private void setEndpoint() throws  URISyntaxException {
    vaultProperties.setUri(new URI(defaultVaultEndpoint));
  }

  @Test
  public void testSuccessRetrieveTokenString() throws IOException, URISyntaxException {
    setEndpoint();
    //mock server
    mockServer
    .expect(once(), requestTo(buildEndpoint().toString()))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content()
            .string(objectMapper.writeValueAsString(buildLogin(validAccessToken))))
        .andRespond(
          withSuccess(JsonUtils.serialize(buildVaultResponse()),
              MediaType.APPLICATION_JSON_UTF8));
    //do test
    assertThat(vaultService.retrieveToken(validAccessToken).login().getToken())
      .isEqualTo(vaultToken);

    mockServer.verify();
  }

  @Test
  public void testSuccessUriRetrieveTokenString() throws IOException, URISyntaxException {
    //mock server
    mockServer
    .expect(once(), requestTo(buildEndpoint().toString()))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content()
            .string(objectMapper.writeValueAsString(buildLogin(validAccessToken))))
        .andRespond(
          withSuccess(JsonUtils.serialize(buildVaultResponse()),
              MediaType.APPLICATION_JSON_UTF8));
    //do test
    assertThat(vaultService.retrieveToken(defaultVaultUri, validAccessToken).login().getToken())
      .isEqualTo(vaultToken);

    mockServer.verify();
  }

  @Test
  public void testExpiredRetrieveTokenString() throws IOException, URISyntaxException {
    setEndpoint();
    //mock server
    mockServer
        .expect(once(), requestTo(buildEndpoint().toString()))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content()
            .string(objectMapper.writeValueAsString(buildLogin(expiredAccessToken))))
        .andRespond(withBadRequest().body("token is expired"));
    // do test
    assertThatThrownBy(
      () -> vaultService.retrieveToken(expiredAccessToken))
      .isInstanceOf(VaultJwtTokenExpiredException.class);

    mockServer.verify();
  }

  @Test
  public void testExpiredUriRetrieveTokenString() throws IOException, URISyntaxException {
    //mock server
    mockServer
        .expect(once(), requestTo(buildEndpoint().toString()))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content()
            .string(objectMapper.writeValueAsString(buildLogin(expiredAccessToken))))
        .andRespond(withBadRequest().body("token is expired"));
    // do test
    assertThatThrownBy(
      () -> vaultService.retrieveToken(defaultVaultUri, expiredAccessToken))
      .isInstanceOf(VaultJwtTokenExpiredException.class);

    mockServer.verify();
  }

  @Test
  public void testHttpErrorRetrieveTokenString() throws IOException, URISyntaxException {
    setEndpoint();
    //mock server
    mockServer
        .expect(once(), requestTo(buildEndpoint().toString()))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content()
            .string(objectMapper.writeValueAsString(buildLogin(badAccessToken))))
        .andRespond(withBadRequest());
    //do test
    assertThatThrownBy(
      () -> vaultService.retrieveToken(badAccessToken))
      .isInstanceOf(HttpClientErrorException.class);

    mockServer.verify();
  }

  @Test
  public void testSuccessRetrieveTokenOidc() throws JsonProcessingException, URISyntaxException {
    setEndpoint();
    //mock server
    mockServer
        .expect(once(), requestTo(buildEndpoint().toString()))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content()
            .string(objectMapper.writeValueAsString(buildLogin(validAccessToken))))
        .andRespond(
          withSuccess(JsonUtils.serialize(buildVaultResponse()),
              MediaType.APPLICATION_JSON_UTF8));
    //do test
    assertThat(vaultService.retrieveToken(oidcTokenId).login().getToken())
      .isEqualTo(vaultToken);

    mockServer.verify();
  }

  @Test
  public void testSuccessUriRetrieveTokenOidc() throws JsonProcessingException, URISyntaxException {
    //mock server
    mockServer
        .expect(once(), requestTo(buildEndpoint().toString()))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content()
            .string(objectMapper.writeValueAsString(buildLogin(validAccessToken))))
        .andRespond(
          withSuccess(JsonUtils.serialize(buildVaultResponse()),
              MediaType.APPLICATION_JSON_UTF8));
    //do test
    assertThat(vaultService.retrieveToken(defaultVaultUri, oidcTokenId).login().getToken())
      .isEqualTo(vaultToken);

    mockServer.verify();
  }

  @Test
  public void testExpiredRetrieveTokenOidc() throws JsonProcessingException, URISyntaxException {
    setEndpoint();
    //mock server
    mockServer
        .expect(once(), requestTo(buildEndpoint().toString()))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content()
            .string(objectMapper.writeValueAsString(buildLogin(expiredAccessToken))))
        .andRespond(withBadRequest().body("token is expired"));

    //do test
    assertThatThrownBy(
        () -> vaultService.retrieveToken(expiredOidcTokenId))
        .isInstanceOf(VaultJwtTokenExpiredException.class);

    mockServer.verify();
  }

  @Test
  public void testExpiredUriRetrieveTokenOidc() throws JsonProcessingException, URISyntaxException {
    //mock server
    mockServer
        .expect(once(), requestTo(buildEndpoint().toString()))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content()
            .string(objectMapper.writeValueAsString(buildLogin(expiredAccessToken))))
        .andRespond(withBadRequest().body("token is expired"));

    //do test
    assertThatThrownBy(
        () -> vaultService.retrieveToken(defaultVaultUri, expiredOidcTokenId))
        .isInstanceOf(VaultJwtTokenExpiredException.class);

    mockServer.verify();
  }

}
