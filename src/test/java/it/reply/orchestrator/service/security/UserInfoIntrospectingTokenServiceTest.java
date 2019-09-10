/*
 * Copyright © 2015-2019 Santer Reply S.p.A.
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

import static org.assertj.core.api.Assertions.*;

import org.junit.Before;
import org.junit.Test;
import org.mitre.jwt.signer.service.impl.JWKSetCacheService;
import org.mitre.openid.connect.client.UserInfoFetcher;
import org.mitre.openid.connect.config.ServerConfiguration;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class UserInfoIntrospectingTokenServiceTest {

  @InjectMocks
  private UserInfoIntrospectingTokenService userInfoIntrospectingTokenService;

  @Mock
  private OAuth2ConfigurationsService oauth2ConfigurationsService;

  @Mock
  private UserInfoFetcher userInfoFetcher;

  @Mock
  private JWKSetCacheService validationServices;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void loadAuthenticationFailInvalidToken() {
    String stringToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6I";
    assertThat(userInfoIntrospectingTokenService.loadAuthentication(stringToken)).isNull();
  }

  @Test
  public void loadAuthenticationFailgetServerConfiguration() throws Exception {
    String stringToken =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhbGljZSIsImlzcyI6Imh0dHBzOi8vYzJpZC5jb20iLCJleHAiOjE0ODg1NTIyNzIxOTV9.3c7wkuOXGjXxVLJXcsaZ259HK07jtwVCQZcwN_fhF3M";
    assertThat(userInfoIntrospectingTokenService.loadAuthentication(stringToken)).isNull();
  }

  @Test
  public void loadAuthenticationFailExpiredToken() throws Exception {
    String stringToken =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhbGljZSIsImlzcyI6Imh0dHBzOi8vYzJpZC5jb20iLCJleHAiOjB9.weKzty6VmVhigzJDhEc_tMMTk2QyDoSO6w0JWHE041Q";
    assertThat(userInfoIntrospectingTokenService.loadAuthentication(stringToken)).isNull();
  }

  @Test
  public void loadAuthenticationValidatorNull() throws Exception {
    String stringToken =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhbGljZSIsImlzcyI6Imh0dHBzOi8vYzJpZC5jb20iLCJleHAiOjE0ODg1NTIyNzIxOTV9.3c7wkuOXGjXxVLJXcsaZ259HK07jtwVCQZcwN_fhF3M";

    ServerConfiguration serverConfiguration = new ServerConfiguration();
    Mockito
        .when(oauth2ConfigurationsService.getServerConfiguration("https://c2id.com"))
        .thenReturn(serverConfiguration);

    assertThat(userInfoIntrospectingTokenService.loadAuthentication(stringToken)).isNull();
  }

}
