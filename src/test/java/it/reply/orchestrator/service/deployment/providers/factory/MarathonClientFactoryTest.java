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

package it.reply.orchestrator.service.deployment.providers.factory;

import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.CloudProviderEndpoint.IaaSType;
import it.reply.orchestrator.dto.security.GenericCredential;
import it.reply.orchestrator.dto.security.GenericCredentialWithTenant;
import it.reply.orchestrator.service.deployment.providers.CredentialProviderService;

import java.net.URISyntaxException;
import java.util.UUID;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import junitparams.JUnitParamsRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(JUnitParamsRunner.class)
public class MarathonClientFactoryTest {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @MockBean
  private CredentialProviderService credProvServ;

  private MarathonClientFactory clientFactory;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    clientFactory = new MarathonClientFactory(credProvServ);
  }

  @Test
  public void testgetClientSuccessful() throws URISyntaxException {

    CloudProviderEndpoint cloudProviderEndpoint = CloudProviderEndpoint
        .builder()
        .cpEndpoint("http://example.com")
        .cpComputeServiceId(UUID.randomUUID().toString())
        .iaasType(IaaSType.MARATHON)
        .build();

    String serviceId = cloudProviderEndpoint.getCpComputeServiceId();

    Mockito
        .when(credProvServ.credentialProvider(serviceId, "token", GenericCredential.class))
        .thenReturn(new GenericCredentialWithTenant("username", "password", "tenant"));

    assertThat(clientFactory.build(cloudProviderEndpoint, "token"))
        .extracting("h.target.url")
        .containsOnly("http://example.com");
  }

  @Test
  public void testgetClientError() {
    CloudProviderEndpoint cloudProviderEndpoint = CloudProviderEndpoint
        .builder()
        .cpEndpoint("http://example.com")
        .cpComputeServiceId(UUID.randomUUID().toString())
        .iaasType(IaaSType.MARATHON)
        .build();
    assertThatThrownBy(() -> clientFactory.build(cloudProviderEndpoint, null))
        .isInstanceOf(NullPointerException.class);
  }
}
