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

package it.reply.orchestrator.service.security;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mitre.jwt.signer.service.impl.JWKSetCacheService;
import org.mitre.openid.connect.client.UserInfoFetcher;
import org.mitre.openid.connect.client.service.ServerConfigurationService;
import org.mitre.openid.connect.config.ServerConfiguration;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;



public class UserInfoIntrospectingTokenServiceTest {

  String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6I";

  UserInfoIntrospectingTokenService iserInfoIntrospectingTokenService;

  @Mock
  ServerConfigurationService serverConfigurationService;

  @Mock
  UserInfoFetcher userInfoFetcher;
  
  @Mock
  JWKSetCacheService validationServices;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    iserInfoIntrospectingTokenService =
        new UserInfoIntrospectingTokenService(serverConfigurationService, null, validationServices);
  }

  @Test
  public void loadAuthenticationFailInvalidToken() {
    Assert.assertEquals(iserInfoIntrospectingTokenService.loadAuthentication(invalidToken), null);
  }

  @Test
  public void loadAuthenticationFailgetServerConfiguration() throws Exception {
    String stringToken =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhbGljZSIsImlzcyI6Imh0dHBzOi8vYzJpZC5jb20iLCJleHAiOjE0ODg1NTIyNzIxOTV9.3c7wkuOXGjXxVLJXcsaZ259HK07jtwVCQZcwN_fhF3M";
    Assert.assertEquals(iserInfoIntrospectingTokenService.loadAuthentication(stringToken), null);
  }

  @Test
  public void loadAuthenticationFailExpiredToken() throws Exception {
    String stringToken =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhbGljZSIsImlzcyI6Imh0dHBzOi8vYzJpZC5jb20iLCJleHAiOjB9.weKzty6VmVhigzJDhEc_tMMTk2QyDoSO6w0JWHE041Q";
    Assert.assertEquals(iserInfoIntrospectingTokenService.loadAuthentication(stringToken), null);
  }

  @Test
  public void loadAuthenticationValidatorNull() throws Exception {
    String stringToken =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhbGljZSIsImlzcyI6Imh0dHBzOi8vYzJpZC5jb20iLCJleHAiOjE0ODg1NTIyNzIxOTV9.3c7wkuOXGjXxVLJXcsaZ259HK07jtwVCQZcwN_fhF3M";

    ServerConfiguration serverConfiguration = new ServerConfiguration();
    Mockito.when(serverConfigurationService.getServerConfiguration("https://c2id.com"))
        .thenReturn(serverConfiguration);

    Mockito.when(validationServices.getValidator(null)).thenReturn(null);
    Assert.assertEquals(iserInfoIntrospectingTokenService.loadAuthentication(stringToken), null);
  }

}
