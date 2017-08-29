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

package it.reply.orchestrator.service;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import it.reply.orchestrator.controller.ControllerTestUtils;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.resource.DeploymentResource;
import it.reply.orchestrator.resource.DeploymentResourceAssembler;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

public class CallbackServiceTest {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock
  private DeploymentRepository deploymentRepository;

  @Spy
  private DeploymentResourceAssembler deploymentResourceAssembler;

  @Spy
  private RestTemplate restTemplate;

  @InjectMocks
  private CallbackServiceImpl callbackService;

  protected MockRestServiceServer mockServer;

  @Before
  public void setup() {
    mockServer = MockRestServiceServer.createServer(restTemplate);
  }

  private boolean doCallback(String url, HttpStatus status) {
    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.setCallback(url);

    DeploymentResource resource = deploymentResourceAssembler.toResource(deployment);
    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    Mockito.when(deploymentResourceAssembler.toResource(deployment)).thenReturn(resource);
    if (status != null) {
      mockServer
          .expect(requestTo(url))
          .andExpect(method(HttpMethod.POST))
          .andRespond(withStatus(status));
    }

    return callbackService.doCallback(deployment.getId());
  }

  @Test
  public void doCallbackSuccessfully() {
    boolean result = doCallback("http://test.com", HttpStatus.OK);
    assertThat(result).isTrue();
  }

  @Test
  public void doCallbackError() {
    boolean result = doCallback("http://test.com", HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(result).isFalse();
  }

  @Test
  public void doCallbackWithoutUrlSuccessfully() {
    boolean result = doCallback(null, null);
    assertThat(result).isFalse();
  }

}
