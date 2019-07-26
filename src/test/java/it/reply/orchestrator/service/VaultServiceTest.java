/*
 * Copyright © 2015-2019 I.N.F.N.
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

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.reply.orchestrator.Application;
import it.reply.orchestrator.service.deployment.providers.MarathonServiceImpl.VaultSecret;

/**
 * This integration test makes real request to the Vault APIs.
 * 
 * @author Michele Perniola
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
public class VaultServiceTest {

  @Autowired
  private VaultService vaultService;

  @Test
  @Ignore
  public void testVault() {

    //fake token, replace with valid one
    vaultService.setVaultToken("s.DQSf698xaTFLtBCY9bG2QdhI");

    //write secret on service
    String spath = "secret/private/marathon/11e9a30f-f358-0741-a6d8-024283ff312b/password"; 
    vaultService.writeSecret(spath, (new VaultSecret()).setValue("mypass")); 

    VaultSecret mypass = vaultService.readSecret(spath, VaultSecret.class);

    assertEquals(mypass.getValue(), "mypass");

    spath = "secret/private/marathon/11e9a30f-f358-0741-a6d8-024283ff312b";
    List<String> depentries = vaultService.listSecrets(spath);

    assertEquals(depentries.size(), 1);

    vaultService.deleteSecret(spath + "/" + depentries.get(0));

    depentries = vaultService.listSecrets(spath);
    assertEquals(depentries.size(), 0);
  }

}
