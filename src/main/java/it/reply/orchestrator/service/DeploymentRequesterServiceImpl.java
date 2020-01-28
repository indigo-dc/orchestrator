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

import it.reply.orchestrator.dal.entity.DeploymentRequester;
import it.reply.orchestrator.dal.repository.DeploymentRequesterRepository;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DeploymentRequesterServiceImpl implements DeploymentRequesterService {

  @Autowired
  DeploymentRequesterRepository deploymentRequesterRepository;

  @Override
  public DeploymentRequester addDeploymentRequester(DeploymentRequester deploymentRequester)
      throws ResourceException {
    LOG.debug("Creating deployment requester with storage path:\n{}",
        deploymentRequester.getStoragePath());
    return deploymentRequesterRepository.save(deploymentRequester);
  }

  @Override
  public void deleteDeploymentRequester(String requesterId) {
    deploymentRequesterRepository.delete(requesterId);
  }

  @Override
  public boolean existsByStoragePath(String storagePath) {
    return deploymentRequesterRepository.findByStoragePath(storagePath) == null ? false : true;
  }

  @Override
  public DeploymentRequester getEntityByPath(String storagePath) {
    return deploymentRequesterRepository.findByStoragePath(storagePath);
  }

  @Override
  public List<DeploymentRequester> getDeploymentRequesterList() {
    return deploymentRequesterRepository.findAll();
  }

}
