package it.reply.orchestrator.service;

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

import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.exception.http.NotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceServiceImpl implements ResourceService {

  @Autowired
  private ResourceRepository resourceRepository;

  @Autowired
  private DeploymentRepository deploymentRepository;

  @Override
  @Transactional(readOnly = true)
  public Page<Resource> getResources(String deploymentId, Pageable pageable) {
    if (deploymentRepository.exists(deploymentId)) {
      return resourceRepository.findByDeployment_id(deploymentId, pageable);
    } else {
      throw new NotFoundException("The deployment <" + deploymentId + "> doesn't exist");
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Resource getResource(String uuid, String deploymentId) {
    Resource resource = resourceRepository.findByIdAndDeployment_id(uuid, deploymentId);
    if (resource != null) {
      return resource;
    } else {
      throw new NotFoundException(
          "The resource <" + uuid + "> in deployment <" + deploymentId + "> doesn't exist");
    }
  }

}
