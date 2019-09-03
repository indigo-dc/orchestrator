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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.net.URI;
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
import org.springframework.test.web.client.response.DefaultResponseCreator;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.support.VaultTokenResponse;
import org.springframework.web.client.HttpClientErrorException;

import it.reply.orchestrator.config.properties.VaultProperties;
import it.reply.orchestrator.exception.VaultJwtTokenExpiredException;
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

  @Autowired
  private VaultService vaultService;

  @Autowired
  private VaultProperties vaultProperties;
  
  @Autowired
  private MockRestServiceServer mockServer;
  
  @MockBean
  private OAuth2TokenService oauth2TokenService;
  
  
  private static final String defaultVaultEndpoint = "http://default.vault.com";
  private static final int defaultVaultPort = 8200;
  private static final String vaultToken = "s.DQSf698xaTFLtBCY9bG2QdhI";
  private static final String accessToken = "eyJhbGciOiJub25lIn0.eyJqdGkiOiI0Y2IyNGQ1Ny1kMzRkLTQxZDQtYmZiYy04NzFiN2I0MDRjZDAifQ.";
  
  @Before
  public void setup() {
    vaultProperties.setUrl(defaultVaultEndpoint);
    vaultProperties.setPort(defaultVaultPort);
  }
  
  @Test
  public void testSuccessRetrieveTokenString() throws IOException {

    VaultEndpoint endpoint = VaultEndpoint.create(
        vaultProperties.getUrl(),
        vaultProperties.getPort());
    URI uri = endpoint.createUri("auth/jwt/login");
    Map<String, Object> auth = new HashMap<String, Object>();
    auth.put("client_token", vaultToken);
    VaultTokenResponse response = new VaultTokenResponse();    
    response.setAuth(auth);
    TokenAuthentication ta = new TokenAuthentication(vaultToken);    
    
    mockServer
    .expect(requestTo(uri.toString()))
    .andExpect(method(HttpMethod.POST))
    .andRespond(
        withSuccess(JsonUtils.serialize(response), MediaType.APPLICATION_JSON_UTF8));
    
    assertThat(vaultService.retrieveToken(accessToken).login().getToken())
      .isEqualTo(ta.login().getToken());
    
    mockServer.verify();
  }
  
  @Test
  public void testExpiredRetrieveTokenString() throws IOException {

    VaultEndpoint endpoint = VaultEndpoint.create(
        vaultProperties.getUrl(),
        vaultProperties.getPort());
    URI uri = endpoint.createUri("auth/jwt/login");
    DefaultResponseCreator response = withBadRequest(); 
    response.body("token is expired");
    
    mockServer
    .expect(requestTo(uri.toString()))
    .andExpect(method(HttpMethod.POST))
    .andRespond(response);
    
    assertThatThrownBy(
        () -> vaultService.retrieveToken(accessToken))
        .isInstanceOf(VaultJwtTokenExpiredException.class);
    
    mockServer.verify();
  }

  @Test
  public void testHttpErrorRetrieveTokenString() throws IOException {

    VaultEndpoint endpoint = VaultEndpoint.create(
        vaultProperties.getUrl(),
        vaultProperties.getPort());
    URI uri = endpoint.createUri("auth/jwt/login");
    
    mockServer
    .expect(requestTo(uri.toString()))
    .andExpect(method(HttpMethod.POST))
    .andRespond(withBadRequest());
    
    assertThatThrownBy(
        () -> vaultService.retrieveToken(accessToken))
        .isInstanceOf(HttpClientErrorException.class);
    
    mockServer.verify();
  }
  
}
