/*
 * Copyright © 2015-2019 Santer Reply S.p.A.
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

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dto.request.DeploymentRequest;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeploymentService {

  public Page<Deployment> getDeployments(Pageable pageable, @Nullable String owner);

  public Deployment getDeployment(String id);

  public Deployment createDeployment(DeploymentRequest request);

  public void updateDeployment(String id, DeploymentRequest request);

  public void deleteDeployment(String id);
}
