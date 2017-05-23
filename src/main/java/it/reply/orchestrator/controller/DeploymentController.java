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

package it.reply.orchestrator.controller;

import it.reply.orchestrator.dal.entity.AbstractResourceEntity;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dto.request.DeploymentRequest;
import it.reply.orchestrator.resource.DeploymentResource;
import it.reply.orchestrator.resource.DeploymentResourceAssembler;
import it.reply.orchestrator.service.DeploymentService;
import it.reply.orchestrator.validator.DeploymentRequestValidator;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@Slf4j
public class DeploymentController {

  private static final String OIDC_DISABLED_CONDITION = "!#oauth2.isOAuth()";

  @Autowired
  private DeploymentService deploymentService;

  @Autowired
  private DeploymentResourceAssembler deploymentResourceAssembler;

  @InitBinder
  protected void initBinder(WebDataBinder binder) {
    binder.setValidator(new DeploymentRequestValidator());
  }

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
      @PageableDefault(sort = AbstractResourceEntity.CREATED_COLUMN_NAME,
          direction = Direction.DESC) Pageable pageable,
      PagedResourcesAssembler<Deployment> pagedAssembler) {

    Page<Deployment> deployments = deploymentService.getDeployments(pageable);

    return pagedAssembler.toResource(deployments, deploymentResourceAssembler);

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
  @PreAuthorize(OIDC_DISABLED_CONDITION
      + " || #oauth2.throwOnError(#oauth2.hasScope('offline_access'))")
  public DeploymentResource createDeployment(@Valid @RequestBody DeploymentRequest request) {

    LOG.info("Creating deployment with template\n{}", request.getTemplate());
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
  @PreAuthorize(OIDC_DISABLED_CONDITION
      + " || #oauth2.throwOnError(#oauth2.hasScope('offline_access'))")
  public void updateDeployment(@PathVariable("deploymentId") String id,
      @Valid @RequestBody DeploymentRequest request) {

    LOG.info("Updating deployment {} with template\n{}", id, request.getTemplate());
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
  @PreAuthorize(OIDC_DISABLED_CONDITION
      + " || #oauth2.throwOnError(#oauth2.hasScope('offline_access'))")
  public void deleteDeployment(@PathVariable("deploymentId") String id) {

    deploymentService.deleteDeployment(id);
  }
}
