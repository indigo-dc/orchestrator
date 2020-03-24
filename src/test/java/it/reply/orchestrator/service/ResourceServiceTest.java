/*
 * Copyright © 2015-2020 Santer Reply S.p.A.
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
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.exception.http.NotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ResourceServiceTest {

  @Mock
  private ResourceRepository resourceRepository;

  @Mock
  private DeploymentService deploymentService;

  private ResourceService service;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    service = new ResourceServiceImpl(resourceRepository, deploymentService);
  }

  @Test(expected = NotFoundException.class)
  public void getResourcesNotFoundDeployment() throws Exception {
    String deploymentId = UUID.randomUUID().toString();
    Pageable pageable = new PageRequest(0, 1);

    Mockito.when(deploymentService.getDeployment(deploymentId))
        .thenThrow(new NotFoundException(deploymentId));
    service.getResources(deploymentId, pageable);
  }

  @Test
  public void getResources() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment();
    List<Resource> resources = ControllerTestUtils.createResources(deployment, 2, false);
    Pageable pageable = new PageRequest(0, 2);

    Mockito.when(deploymentService.getDeployment(deployment.getId())).thenReturn(deployment);
    Mockito.when(resourceRepository.findByDeployment_id(deployment.getId(), pageable))
        .thenReturn(new PageImpl<Resource>(resources, pageable, 2));
    Page<Resource> pagedResources = service.getResources(deployment.getId(), pageable);

    assertEquals(resources, pagedResources.getContent());
  }

  @Test(expected = NotFoundException.class)
  public void getResourceNotFoundDeployment() throws Exception {
    String deploymentId = UUID.randomUUID().toString();

    Mockito.when(deploymentService.getDeployment(deploymentId))
        .thenThrow(new NotFoundException(deploymentId));

    service.getResource(UUID.randomUUID().toString(), deploymentId);
  }

  @Test(expected = NotFoundException.class)
  public void getResourceNotFoundResource() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment();
    String resourceId = UUID.randomUUID().toString();
    Mockito.when(deploymentService.getDeployment(deployment.getId())).thenReturn(deployment);
    Mockito.when(resourceRepository.findByIdAndDeployment_id(resourceId, deployment.getId()))
        .thenReturn(Optional.empty());
    service.getResource(resourceId, deployment.getId());
  }

  @Test
  public void getResource() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment();
    Resource expectedResource = ControllerTestUtils.createResource(deployment);
    Mockito.when(deploymentService.getDeployment(deployment.getId())).thenReturn(deployment);
    Mockito.when(
        resourceRepository.findByIdAndDeployment_id(expectedResource.getId(), deployment.getId()))
        .thenReturn(Optional.of(expectedResource));
    Resource actualResource = service.getResource(expectedResource.getId(), deployment.getId());

    assertEquals(expectedResource, actualResource);

  }
}
