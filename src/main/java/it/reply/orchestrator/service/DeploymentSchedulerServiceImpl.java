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
import it.reply.orchestrator.dal.entity.OidcEntity;
import it.reply.orchestrator.dal.entity.OidcEntityId;
import it.reply.orchestrator.dal.entity.OidcRefreshToken;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dal.repository.DeploymentSchedulerRepository;
import it.reply.orchestrator.dal.repository.OidcTokenRepository;
import it.reply.orchestrator.dto.request.SchedulerRequest;
import it.reply.orchestrator.dto.security.IndigoOAuth2Authentication;
import it.reply.orchestrator.dto.security.IndigoUserInfo;
import it.reply.orchestrator.exception.http.BadRequestException;
import it.reply.orchestrator.exception.http.NotFoundException;
import it.reply.orchestrator.service.security.OAuth2TokenService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DeploymentSchedulerServiceImpl implements DeploymentSchedulerService {

  private static final Pattern OWNER_PATTERN = Pattern.compile("([^@]+)@([^@]+)");

  @Autowired
  DeploymentSchedulerRepository deploymentSchedulerRepository;

  @Autowired
  OidcTokenRepository oidcTokenRepository;

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

      //TODO replace temporary with static token
      // OidcTokenId token = oauth2TokenService.exchangeCurrentAccessToken();
      String id = "11ea4129-fa93-dc7b-a227-d0577b460825";
      OidcRefreshToken refreshToken =  oidcTokenRepository.findAll().get(0);

      deploymentScheduler.setRequestedWithToken(refreshToken);
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

  @Override
  public DeploymentScheduler getDeploymentScheduler(String schedulerId) {
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
  public Page<DeploymentScheduler> getDeploymentSchedulers(Pageable pageable, String owner) {
    if (owner == null) {
      if (oidcProperties.isEnabled() && isAdmin()) {
        OidcEntity requester = oauth2TokenService.generateOidcEntityFromCurrentAuth();
        return deploymentSchedulerRepository.findAll(requester, pageable);
      }
      owner = "me";
    }
    OidcEntityId ownerId;
    if ("me".equals(owner)) {
      ownerId = oauth2TokenService.generateOidcEntityIdFromCurrentAuth();
    } else {
      Matcher matcher = OWNER_PATTERN.matcher(owner);
      if (isAdmin() && matcher.matches()) {
        ownerId = new OidcEntityId();
        ownerId.setSubject(matcher.group(1));
        ownerId.setIssuer(matcher.group(2));
      } else {
        throw new BadRequestException("Value " + owner + " for param createdBy is illegal");
      }
    }
    if (oidcProperties.isEnabled()) {
      OidcEntity requester = oauth2TokenService.generateOidcEntityFromCurrentAuth();
      return deploymentSchedulerRepository.findAllByOwner(requester, ownerId, pageable);
    } else {
      return deploymentSchedulerRepository.findAllByOwner(ownerId, pageable);
    }
  }

  @Override
  public DeploymentScheduler getEntityByPath(String storagePath) {
    //TODO must be unique
    return deploymentSchedulerRepository.findByUserStoragePath(storagePath);
  }

  private boolean isAdmin() {
    boolean isAdmin = false;
    if (oidcProperties.isEnabled()) {
      OidcEntity requester = oauth2TokenService.generateOidcEntityFromCurrentAuth();
      String issuer = requester.getOidcEntityId().getIssuer();
      String group = oidcProperties.getIamProperties().get(issuer).getAdmingroup();
      IndigoOAuth2Authentication authentication = oauth2TokenService.getCurrentAuthentication();
      IndigoUserInfo userInfo = (IndigoUserInfo) authentication.getUserInfo();
      if (userInfo != null) {
        isAdmin = userInfo.getGroups().contains(group);
      }
    }
    return isAdmin;
  }

}
