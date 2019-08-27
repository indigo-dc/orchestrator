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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.vault.support.VaultResponse;

import it.reply.orchestrator.Application;
import it.reply.orchestrator.dto.vault.VaultSecret;

/**
 * Vault Service API test
 * 
 * @author Michele Perniola
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
public class VaultServiceTest {

  @Mock
  private VaultService vaultService;
  
  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }
  
  @Test
  public void testReadSecret() {
    String token = "s.DQSf698xaTFLtBCY9bG2QdhI";
    String spath = "secret/private/marathon/11e9a30f-f358-0741-a6d8-024283ff312b/password";
    VaultSecret mypass = (new VaultSecret()).setValue("mypass");
    
    Mockito.when(vaultService.readSecret(token, spath, VaultSecret.class)).thenReturn(mypass);

    Assert.assertEquals(vaultService.readSecret(token, spath, VaultSecret.class), mypass);    
  }
  
  @Test
  public void testWriteSecret() {
    String token = "s.DQSf698xaTFLtBCY9bG2QdhI";
    String spath = "secret/private/marathon/11e9a30f-f358-0741-a6d8-024283ff312b/password";
    VaultSecret mypass = (new VaultSecret()).setValue("mypass");
    VaultResponse response = new VaultResponse();

    Mockito.when(vaultService.writeSecret(token, spath, mypass)).thenReturn(response);
    
    assertEquals(vaultService.writeSecret(token, spath, mypass), response);
    
  }
  
  @Test
  public void testListSecrets() {

    String token = "s.DQSf698xaTFLtBCY9bG2QdhI";
    String spath = "secret/private/marathon/11e9a30f-f358-0741-a6d8-024283ff312b";
    List<String> depentries = Arrays.asList("mypass");

    assertEquals(depentries.size(), 1);

    Mockito.when(vaultService.listSecrets(token, spath)).thenReturn(depentries);
    
    List<String> result = vaultService.listSecrets(token, spath);
    assertEquals(result, depentries);
  }
  
  @Test
  public void testDeleteSecret() {
    String token = "s.DQSf698xaTFLtBCY9bG2QdhI";
    String spath = "secret/private/marathon/11e9a30f-f358-0741-a6d8-024283ff312b/password";
    VaultSecret mypass = (new VaultSecret()).setValue("mypass");
    vaultService.deleteSecret(token, spath + "/" + mypass.getValue());    
  }

}
