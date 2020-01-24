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

import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.dal.entity.DeploymentScheduler;
import it.reply.orchestrator.dal.entity.OidcRefreshToken;
import it.reply.orchestrator.dal.repository.DeploymentSchedulerRepository;
import it.reply.orchestrator.dto.request.SchedulerRequest;
import it.reply.orchestrator.exception.http.NotFoundException;
import it.reply.orchestrator.service.security.OAuth2TokenService;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DeploymentSchedulerServiceImpl implements DeploymentSchedulerService {

  @Autowired
  DeploymentSchedulerRepository deploymentSchedulerRepository;

  @Autowired
  private OAuth2TokenService oauth2TokenService;

  @Autowired
  private OidcProperties oidcProperties;

  @Override
  public DeploymentScheduler addDeploymentScheduler(SchedulerRequest schedulerRequest)
      throws ResourceException {

    DeploymentScheduler deploymentScheduler = new DeploymentScheduler();
    deploymentScheduler.setUserStoragePath(schedulerRequest.getUserStoragePath());
    deploymentScheduler.setTemplate(schedulerRequest.getTemplate());
    deploymentScheduler.setCallback(schedulerRequest.getCallback());
    deploymentScheduler.setParameters(schedulerRequest.getParameters());
    if (oidcProperties.isEnabled()) {
      deploymentScheduler.setOwner(oauth2TokenService.getOrGenerateOidcEntityFromCurrentAuth());
      //TODO ?
      OidcRefreshToken newRft = new OidcRefreshToken();
      newRft.setOidcTokenId(oauth2TokenService.generateTokenIdFromCurrentAuth());
      deploymentScheduler.setRequestedWithToken(newRft);
    }

    LOG.debug("Creating deployment scheduler with user storage path:\n{}",
        schedulerRequest.getUserStoragePath());

    if (!existsByStoragePath(deploymentScheduler.getUserStoragePath())) {
      DeploymentScheduler outEntity = deploymentSchedulerRepository.save(deploymentScheduler);
      if (outEntity != null) {
        return outEntity;
      } else {
        throw new ResourceException();
      }
    }
    return null;
  }

  @Override
  public void deleteDeploymentScheduler(String schedulerId) {
    DeploymentScheduler entity = getDeploymentScheduler(schedulerId);
    deploymentSchedulerRepository.delete(entity);
  }

  private DeploymentScheduler getDeploymentScheduler(String schedulerId) {
    DeploymentScheduler entity = deploymentSchedulerRepository.findOne(schedulerId);
    if (entity != null) {
      return entity;
    } else {
      throw new NotFoundException("The scheduler <" + schedulerId + "> doesn't exist");
    }
  }

  @Override
  public boolean existsByStoragePath(String storagePath) {
    return deploymentSchedulerRepository.findByUserStoragePath(storagePath) == null ? false : true;
  }

  @Override
  public List<DeploymentScheduler> getDeploymentSchedulerList() {
    return deploymentSchedulerRepository.findAll();
  }

  @Override
  public DeploymentScheduler getEntityByPath(String storagePath) {
    //TODO
    String temmpDebug = "";
    return deploymentSchedulerRepository.findByUserStoragePath(storagePath);
  }

}
