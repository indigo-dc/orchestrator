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

import it.reply.orchestrator.controller.ControllerTestUtils;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.exception.http.NotFoundException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

public class TemplateServiceTest {

  private TemplateService templateService;

  @Mock
  private DeploymentService deploymentService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    templateService = new TemplateServiceImpl(deploymentService);
  }

  @Test
  public void getTemplate() {
    Deployment deployment = ControllerTestUtils.createDeployment();
    String deploymentId = deployment.getId();

    Mockito.when(deploymentService.getDeployment(deploymentId)).thenReturn(deployment);

    Assert.assertEquals(templateService.getTemplate(deploymentId), deployment.getTemplate());
  }

  @Test(expected = NotFoundException.class)
  public void failGetTemplate() {
    String deploymentId = UUID.randomUUID().toString();
    Mockito.when(deploymentService.getDeployment(deploymentId))
        .thenThrow(new NotFoundException(deploymentId));
    templateService.getTemplate(deploymentId);
  }

}
