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

import groovy.util.ResourceException;

import it.reply.orchestrator.dal.entity.DeploymentScheduler;
import it.reply.orchestrator.dto.request.SchedulerRequest;
import it.reply.orchestrator.service.DeploymentSchedulerService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeploymentSchedulerController {

  private static final String OFFLINE_ACCESS_REQUIRED_CONDITION =
      "#oauth2.throwOnError(#oauth2.hasScope('offline_access'))";

  @Autowired
  private DeploymentSchedulerService deploymentSchedulerService;

  /**
   * Create and save Deployment scheduler.
   * Add a user storage path and template
   * for deployment schedule
   * @throws ResourceException
   *
   */
  @ResponseStatus(HttpStatus.CREATED)
  @RequestMapping(value = "/scheduler", method = RequestMethod.POST,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public DeploymentScheduler addStoragePath(@RequestBody SchedulerRequest schedulerRequest)
      throws ResourceException {

    DeploymentScheduler deploymentScheduler = deploymentSchedulerService
        .addDeploymentScheduler(schedulerRequest);
    if (deploymentScheduler != null) {
      return deploymentScheduler;
    } else {
      throw new ResourceException("already exists");
    }
  }

  /**
   * Delete the scheduler.
   *
   * @param id
   *          the scheduler id
   */
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @RequestMapping(value = "/scheduler/{schedulerId}", method = RequestMethod.DELETE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(OFFLINE_ACCESS_REQUIRED_CONDITION)
  public void deleteDeployment(@PathVariable("schedulerId") String id) {
    deploymentSchedulerService.deleteDeploymentScheduler(id);
  }

}
