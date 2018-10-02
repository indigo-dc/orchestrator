/*
 * Copyright © 2015-2018 Santer Reply S.p.A.
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.CloudProviderEndpoint.IaaSType;

import java.util.UUID;

import junitparams.JUnitParamsRunner;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(JUnitParamsRunner.class)
public class MarathonClientFactoryTest {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  private MarathonClientFactory clientFactory;

  @Before
  public void setup() {
    clientFactory = new MarathonClientFactory();
  }

  @Test
  public void testgetClientSuccessful() {
    CloudProviderEndpoint cloudProviderEndpoint = CloudProviderEndpoint
        .builder()
        .cpEndpoint("http://example.com")
        .cpComputeServiceId(UUID.randomUUID().toString())
        .iaasType(IaaSType.MARATHON)
        .build();
    Assertions
        .assertThat(
            Assertions.catchThrowable(() -> clientFactory.build(cloudProviderEndpoint, "token")))
        .isNull();
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
