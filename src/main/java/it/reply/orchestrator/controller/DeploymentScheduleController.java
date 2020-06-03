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

package it.reply.orchestrator.controller;

import it.reply.orchestrator.dal.entity.DeploymentSchedule;
import it.reply.orchestrator.dal.entity.DeploymentScheduleEvent;
import it.reply.orchestrator.dto.request.DeploymentRequest;
import it.reply.orchestrator.dto.request.DeploymentScheduleRequest;
import it.reply.orchestrator.resource.DeploymentResource;
import it.reply.orchestrator.resource.DeploymentScheduleEventResource;
import it.reply.orchestrator.resource.DeploymentScheduleEventResourceAssembler;
import it.reply.orchestrator.resource.DeploymentScheduleResource;
import it.reply.orchestrator.resource.DeploymentScheduleResourceAssembler;
import it.reply.orchestrator.service.DeploymentScheduleServiceImpl;
import javax.validation.Valid;
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

@RestController
public class DeploymentScheduleController {

  private static final String OFFLINE_ACCESS_REQUIRED_CONDITION =
      "#oauth2.throwOnError(#oauth2.hasAnyScope('offline_access', 'fts:submit-transfer'))";

  @Autowired
  private DeploymentScheduleServiceImpl deploymentScheduleService;

  @Autowired
  private DeploymentScheduleResourceAssembler deploymentScheduleResourceAssembler;

  @Autowired
  private DeploymentScheduleEventResourceAssembler deploymentScheduleEventResourceAssembler;

  /**
   * Get all deployments.
   *
   * @param createdBy      created by name
   * @param pageable       {@link Pageable}
   * @param pagedAssembler {@link PagedResourcesAssembler}
   * @return {@link DeploymentResource}
   */
  @ResponseStatus(HttpStatus.OK)
  @RequestMapping(value = "/schedules", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public PagedResources<DeploymentScheduleResource> getDeploymentSchedules(
      @RequestParam(name = "createdBy", required = false) @Nullable String createdBy,
      @PageableDefault(sort = "createdAt",
          direction = Direction.DESC) Pageable pageable,
      PagedResourcesAssembler<DeploymentSchedule> pagedAssembler) {

    Page<DeploymentSchedule> deploymentSchedules =
        deploymentScheduleService.getDeploymentSchedules(pageable, createdBy);

    return pagedAssembler.toResource(deploymentSchedules, deploymentScheduleResourceAssembler,
        ControllerLinkBuilder
            .linkTo(
                DummyInvocationUtils
                    .methodOn(DeploymentScheduleController.class)
                    .getDeploymentSchedules(createdBy, pageable, pagedAssembler))
            .withSelfRel());

  }

  /**
   * Create a deployment.
   *
   * @param request {@link DeploymentRequest}
   * @return {@link DeploymentResource}
   */
  @ResponseStatus(HttpStatus.CREATED)
  @RequestMapping(value = "/schedules", method = RequestMethod.POST,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(OFFLINE_ACCESS_REQUIRED_CONDITION)
  public DeploymentScheduleResource createDeployment(
      @Valid @RequestBody DeploymentScheduleRequest request) {
    DeploymentSchedule deploymentSchedule =
        deploymentScheduleService.createDeploymentSchedule(request);
    return deploymentScheduleResourceAssembler.toResource(deploymentSchedule);

  }

  /**
   * Get the deployment.
   *
   * @param id the deployment id
   * @return {@link DeploymentResource}
   */
  @ResponseStatus(HttpStatus.OK)
  @RequestMapping(value = "/schedules/{scheduleId}", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public DeploymentScheduleResource getDeploymentSchedule(@PathVariable("scheduleId") String id) {

    DeploymentSchedule deploymentSchedule = deploymentScheduleService.getDeploymentSchedule(id);
    return deploymentScheduleResourceAssembler.toResource(deploymentSchedule);
  }

  /**
   * Get the deployment.
   *
   * @param id the deployment id
   * @return {@link DeploymentResource}
   */
  @ResponseStatus(HttpStatus.OK)
  @RequestMapping(value = "/schedules/{scheduleId}/events", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public PagedResources<DeploymentScheduleEventResource> getDeploymentScheduleEvents(
      @PathVariable("scheduleId") String id,
      @RequestParam(name = "createdBy", required = false) @Nullable String createdBy,
      @PageableDefault(sort = "createdAt",
          direction = Direction.DESC) Pageable pageable,
      PagedResourcesAssembler<DeploymentScheduleEvent> pagedAssembler) {
    Page<DeploymentScheduleEvent> scheduleEvents =
        deploymentScheduleService.getDeploymentScheduleEvents(id, pageable);

    PagedResources<DeploymentScheduleEventResource> pagedResources =
        pagedAssembler.toResource(scheduleEvents, deploymentScheduleEventResourceAssembler);
    return pagedResources;
  }
}
