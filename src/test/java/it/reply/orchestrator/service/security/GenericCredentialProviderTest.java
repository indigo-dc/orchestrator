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

import static org.assertj.core.api.Assertions.assertThat;

import it.reply.orchestrator.dto.security.GenericServiceCredentialWithTenant;
import it.reply.orchestrator.dto.vault.TokenAuthenticationExtended;
import it.reply.orchestrator.service.VaultService;
import it.reply.orchestrator.service.deployment.providers.CredentialProviderServiceImpl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.vault.support.VaultToken;

import junitparams.JUnitParamsRunner;

@RunWith(JUnitParamsRunner.class)
public class GenericCredentialProviderTest {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  private static final String defaultVaultEndpoint = "https://default.vault.com:8200";

  @InjectMocks
  private CredentialProviderServiceImpl credProvServ;

  @MockBean
  private VaultService vaultService;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testCredentialProvider() throws URISyntaxException, IOException {

    Mockito
        .when(vaultService.getServiceUri())
        .thenReturn(Optional.of(new URI(defaultVaultEndpoint)));

    TokenAuthenticationExtended tokenExt = new TokenAuthenticationExtended(
        VaultToken.of("vaultToken".toCharArray()), "entityId");

    Mockito
        .when(vaultService.retrieveToken(new URI(defaultVaultEndpoint), "accessToken"))
        .thenReturn(tokenExt);

    GenericServiceCredentialWithTenant genCredWTen =
        new GenericServiceCredentialWithTenant("username", "password", "tenant");

    Mockito
        .when(vaultService.readSecret(tokenExt,
            "https://default.vault.com:8200/v1/secret/data/entityIdnullcom.amazonaws.ec2",
            GenericServiceCredentialWithTenant.class))
        .thenReturn(genCredWTen);

    GenericServiceCredentialWithTenant imCred = credProvServ.credentialProvider("com.amazonaws.ec2",
        "accessToken", GenericServiceCredentialWithTenant.class);

    assertThat(imCred).isNotNull();
    assertThat(imCred.getUsername()).isEqualTo("username");
    assertThat(imCred.getPassword()).isEqualTo("password");
    assertThat(imCred.getTenant()).isEqualTo("tenant");
  }

}
