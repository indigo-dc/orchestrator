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

import it.reply.orchestrator.config.properties.MarathonProperties;
import it.reply.orchestrator.config.properties.MesosProperties;
import it.reply.orchestrator.config.properties.MesosProperties.MesosInstanceProperties;
import it.reply.orchestrator.controller.ControllerTestUtils;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.exception.service.DeploymentException;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.net.URI;

import junitparams.JUnitParamsRunner;

@RunWith(JUnitParamsRunner.class)
public class MarathonClientFactoryTest {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  private MesosProperties mesosProperties;

  private MesosInstanceProperties mesosInstanceProperties;

  private MarathonProperties marathonProperties;

  private static String cloudProviderName = "cloud-provider";

  private MarathonClientFactory clientFactory;

  @Before
  public void setup() {
    mesosProperties = new MesosProperties();
    clientFactory = new MarathonClientFactory(mesosProperties);
  }

  @Test
  public void testgetClientSuccessful() {
    marathonProperties = new MarathonProperties();
    marathonProperties.setUrl(URI.create("http://endpoint"));
    marathonProperties.setUsername("username");
    marathonProperties.setPassword("password");
    mesosInstanceProperties = new MesosInstanceProperties();
    mesosInstanceProperties.setMarathon(marathonProperties);
    mesosProperties.getInstances().put(cloudProviderName, mesosInstanceProperties);
    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.setCloudProviderName(cloudProviderName);
    Assertions
        .assertThat(Assertions.catchThrowable(() -> clientFactory.build(deployment)))
        .isNull();
  }

  @Test
  public void testgetClientError() {
    marathonProperties = new MarathonProperties();
    mesosProperties.getInstances().put(cloudProviderName, mesosInstanceProperties);
    Deployment deployment = ControllerTestUtils.createDeployment();
    Assertions.assertThatThrownBy(() -> clientFactory.build(deployment)).isInstanceOf(
        DeploymentException.class);
  }
}
