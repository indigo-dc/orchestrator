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

import static org.junit.Assert.assertEquals;

import it.reply.orchestrator.controller.ControllerTestUtils;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.resource.DeploymentResource;
import it.reply.orchestrator.resource.DeploymentResourceAssembler;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class CallbackServiceTest {

  @Mock
  private DeploymentRepository deploymentRepository;

  @Spy
  private DeploymentResourceAssembler deploymentResourceAssembler =
      new DeploymentResourceAssembler();

  @Mock
  private RestTemplate restTemplate;

  private CallbackService callbackService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    callbackService =
        new CallbackServiceImpl(deploymentRepository, deploymentResourceAssembler, restTemplate);
  }

  private boolean doCallback(String url, HttpStatus status) {
    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.setCallback(url);
    ResponseEntity<Object> response = new ResponseEntity<>(status);
    DeploymentResource resource = deploymentResourceAssembler.toResource(deployment);

    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    Mockito.when(deploymentResourceAssembler.toResource(deployment)).thenReturn(resource);
    Mockito.when(restTemplate.postForEntity(deployment.getCallback(), resource, Object.class))
        .thenReturn(response);

    return callbackService.doCallback(deployment.getId());
  }

  @Test
  public void doCallbackSuccessfully() {
    boolean result = doCallback("http://test.com", HttpStatus.OK);
    assertEquals(true, result);
  }

  @Test
  public void doCallbackError() {
    boolean result = doCallback("http://test.com", HttpStatus.INTERNAL_SERVER_ERROR);
    assertEquals(false, result);
  }

  @Test
  public void doCallbackWithoutUrlSuccessfully() {
    boolean result = doCallback(null, null);
    assertEquals(false, result);

  }

}
