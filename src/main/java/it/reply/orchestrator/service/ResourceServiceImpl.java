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

package it.reply.orchestrator.service;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.exception.http.NotFoundException;
import it.reply.orchestrator.utils.MdcUtils;

import lombok.AllArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ResourceServiceImpl implements ResourceService {

  private ResourceRepository resourceRepository;

  private DeploymentService deploymentservice;

  @Override
  @Transactional(readOnly = true)
  public Page<Resource> getResources(String deploymentId, Pageable pageable) {
    // check if deploymentExists
    Deployment deployment = deploymentservice.getDeployment(deploymentId);
    MdcUtils.setDeploymentId(deployment.getId());
    return resourceRepository.findByDeployment_id(deploymentId, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Resource getResource(String uuid, String deploymentId) {
    // check if deploymentExists
    Deployment deployment = deploymentservice.getDeployment(deploymentId);
    MdcUtils.setDeploymentId(deployment.getId());
    return resourceRepository.findByIdAndDeployment_id(uuid, deploymentId)
        .orElseThrow(() -> new NotFoundException(String
            .format("The resource <%s> in deployment <%s> doesn't exist", uuid, deploymentId)));
  }

}
