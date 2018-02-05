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

package it.reply.orchestrator.controller;

import it.reply.orchestrator.dal.entity.AbstractResourceEntity;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dto.request.DeploymentRequest;
import it.reply.orchestrator.resource.DeploymentResource;
import it.reply.orchestrator.resource.DeploymentResourceAssembler;
import it.reply.orchestrator.service.DeploymentService;
import it.reply.orchestrator.utils.MdcUtils;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.core.DummyInvocationUtils;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
public class DeploymentController {

  private static final String OFFLINE_ACCESS_REQUIRED_CONDITION =
      "#oauth2.throwOnError(#oauth2.hasScope('offline_access'))";

  @Autowired
  private DeploymentService deploymentService;

  @Autowired
  private DeploymentResourceAssembler deploymentResourceAssembler;

  /**
   * Get all deployments.
   * 
   * @param pageable
   *          {@Link Pageable}
   * @param pagedAssembler
   *          {@link PagedResourcesAssembler}
   * @return {@link DeploymentResource}
   */
  @ResponseStatus(HttpStatus.OK)
  @RequestMapping(value = "/deployments", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public PagedResources<DeploymentResource> getDeployments(
      @RequestParam(name = "createdBy", required = false) @Nullable String createdBy,
      @PageableDefault(sort = AbstractResourceEntity.CREATED_COLUMN_NAME,
          direction = Direction.DESC) Pageable pageable,
      PagedResourcesAssembler<Deployment> pagedAssembler) {

    Page<Deployment> deployments = deploymentService.getDeployments(pageable, createdBy);

    return pagedAssembler.toResource(deployments, deploymentResourceAssembler,
        ControllerLinkBuilder
            .linkTo(
                DummyInvocationUtils
                    .methodOn(DeploymentController.class)
                    .getDeployments(createdBy, pageable, pagedAssembler))
            .withSelfRel());

  }

  /**
   * Create a deployment.
   * 
   * @param request
   *          {@link DeploymentRequest}
   * @return {@link DeploymentResource}
   */
  @ResponseStatus(HttpStatus.CREATED)
  @RequestMapping(value = "/deployments", method = RequestMethod.POST,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(OFFLINE_ACCESS_REQUIRED_CONDITION)
  public DeploymentResource createDeployment(@Valid @RequestBody DeploymentRequest request) {
    Deployment deployment = deploymentService.createDeployment(request);
    return deploymentResourceAssembler.toResource(deployment);

  }

  /**
   * Update the deployment.
   * 
   * @param id
   *          the deployment id
   * @param request
   *          {@link DeploymentRequest}
   */
  @ResponseStatus(HttpStatus.ACCEPTED)
  @RequestMapping(value = "/deployments/{deploymentId}", method = RequestMethod.PUT,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(OFFLINE_ACCESS_REQUIRED_CONDITION)
  public void updateDeployment(@PathVariable("deploymentId") String id,
      @Valid @RequestBody DeploymentRequest request) {
    deploymentService.updateDeployment(id, request);
  }

  /**
   * Get the deployment.
   * 
   * @param id
   *          the deployment id
   * @return {@link DeploymentResource}
   */
  @ResponseStatus(HttpStatus.OK)
  @RequestMapping(value = "/deployments/{deploymentId}", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public DeploymentResource getDeployment(@PathVariable("deploymentId") String id) {

    Deployment deployment = deploymentService.getDeployment(id);
    MdcUtils.setDeploymentId(deployment.getId());
    return deploymentResourceAssembler.toResource(deployment);
  }

  /**
   * Delete the deployment.
   * 
   * @param id
   *          the deployment id
   */
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @RequestMapping(value = "/deployments/{deploymentId}", method = RequestMethod.DELETE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(OFFLINE_ACCESS_REQUIRED_CONDITION)
  public void deleteDeployment(@PathVariable("deploymentId") String id) {

    deploymentService.deleteDeployment(id);
  }
}
