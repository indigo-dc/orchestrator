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

import it.reply.orchestrator.config.properties.OidcProperties;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class OAuth2TokenServiceTest {

  @InjectMocks
  private OAuth2TokenService oAuth2TokenService = new OAuth2TokenService();

  @Spy
  private OidcProperties oidcProperties;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void failGetOAuth2Token() {
    oidcProperties.setEnabled(false);
    assertThatThrownBy(() -> oAuth2TokenService.getOAuth2TokenFromCurrentAuth())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Security is not enabled");
  }

  @Test
  public void failGetOAuth2TokenNull() {
    oidcProperties.setEnabled(true);
    assertThatThrownBy(() -> oAuth2TokenService.getOAuth2TokenFromCurrentAuth())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("User is not authenticated");
  }

}
