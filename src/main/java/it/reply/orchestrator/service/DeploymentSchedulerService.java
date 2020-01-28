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

import groovy.util.ResourceException;

import it.reply.orchestrator.dal.entity.DeploymentScheduler;
import it.reply.orchestrator.dto.request.SchedulerRequest;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeploymentSchedulerService {

  public DeploymentScheduler addDeploymentScheduler(SchedulerRequest schedulerRequest)
      throws ResourceException;

  public DeploymentScheduler getDeploymentScheduler(String schedulerId);

  public void deleteDeploymentScheduler(String schedulerId);

  public boolean existsByStoragePath(String storagePath);

  public DeploymentScheduler getEntityByPath(String storagePath);

  public Page<DeploymentScheduler> getDeploymentSchedulers(
      Pageable pageable,
      @Nullable String owner
      );

}
