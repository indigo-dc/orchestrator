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

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.dto.security.IndigoOAuth2Authentication;

public class OAuth2TokenServiceTest {

  @InjectMocks
  OAuth2TokenService oAuth2TokenService = new OAuth2TokenService();

  @Mock
  OidcProperties oidcProperties;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test(expected = IllegalStateException.class)
  public void failGetOAuth2Token() {
    Mockito.when(oidcProperties.isEnabled()).thenReturn(false);
    oAuth2TokenService.getOAuth2TokenFromCurrentAuth();
  }

  @Test(expected = IllegalStateException.class)
  public void failGetOAuth2TokenNull() {
    Mockito.when(oidcProperties.isEnabled()).thenReturn(true);
    oAuth2TokenService.getOAuth2TokenFromCurrentAuth();
  }

}
