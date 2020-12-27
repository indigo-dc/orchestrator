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

import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.DeploymentSchedule;
import it.reply.orchestrator.dal.entity.DeploymentScheduleEvent;
import it.reply.orchestrator.dal.entity.OidcEntity;
import it.reply.orchestrator.dal.entity.OidcEntityId;
import it.reply.orchestrator.dal.entity.OidcRefreshToken;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dal.repository.DeploymentScheduleEventRepository;
import it.reply.orchestrator.dal.repository.DeploymentScheduleRepository;
import it.reply.orchestrator.dal.repository.OidcTokenRepository;
import it.reply.orchestrator.dto.request.DeploymentScheduleRequest;
import it.reply.orchestrator.dto.security.IamUserInfo;
import it.reply.orchestrator.dto.security.IndigoOAuth2Authentication;
import it.reply.orchestrator.enums.DeploymentScheduleStatus;
import it.reply.orchestrator.exception.http.BadRequestException;
import it.reply.orchestrator.exception.http.NotFoundException;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.orchestrator.utils.MdcUtils;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class DeploymentScheduleServiceImpl {

  private static final Pattern OWNER_PATTERN = Pattern.compile("([^@]+)@([^@]+)");

  @Autowired
  private DeploymentService deploymentService;

  @Autowired
  private ToscaService toscaService;

  @Autowired
  private OidcProperties oidcProperties;

  @Autowired
  private DeploymentScheduleEventRepository deploymentScheduleEventRepository;

  @Autowired
  private DeploymentScheduleRepository deploymentScheduleRepository;

  @Autowired
  private OAuth2TokenService oauth2Tokenservice;

  @Autowired
  private OidcTokenRepository oidcTokenRepository;

  private boolean isAdmin() {
    boolean isAdmin = false;
    if (oidcProperties.isEnabled()) {
      OidcEntity requester = oauth2Tokenservice.generateOidcEntityFromCurrentAuth();
      String issuer = requester.getOidcEntityId().getIssuer();
      String group = oidcProperties.getIamProperties().get(issuer).getAdmingroup();
      IndigoOAuth2Authentication authentication = oauth2Tokenservice.getCurrentAuthentication();
      IamUserInfo userInfo = (IamUserInfo) authentication.getUserInfo();
      if (userInfo != null) {
        isAdmin = userInfo.getGroups().contains(group);
      }
    }
    return isAdmin;
  }

  /**
   * Create a new Deployment schedule.
   * @param request the request
   * @return the deployment schedule
   */
  @Transactional
  public DeploymentSchedule createDeploymentSchedule(DeploymentScheduleRequest request) {
    DeploymentSchedule deploymentSchedule = new DeploymentSchedule();
    deploymentSchedule.setStatus(DeploymentScheduleStatus.RUNNING);

    deploymentSchedule.setTemplate(request.getTemplate());
    deploymentSchedule.setParameters(request.getParameters());
    deploymentSchedule.setCallback(request.getCallback());
    deploymentSchedule.setFileExpression(request.getFileExpression());
    deploymentSchedule.setReplicationExpression(request.getReplicationExpression());
    deploymentSchedule.setNumberOfReplicas(request.getNumberOfReplicas());

    LOG.debug("Creating deployment schedule with template\n{}", request.getTemplate());
    // Parse once, validate structure and user's inputs, replace user's input
    toscaService.prepareTemplate(request.getTemplate(), request.getParameters());

    if (oidcProperties.isEnabled()) {
      deploymentSchedule.setOwner(oauth2Tokenservice.getOrGenerateOidcEntityFromCurrentAuth());
      Optional<OidcRefreshToken> oidcRefreshToken =
          oidcTokenRepository.findByOidcTokenId(oauth2Tokenservice.exchangeCurrentAccessToken());
      deploymentSchedule.setRequestedWithToken(oidcRefreshToken.orElse(null));
    }
    deploymentSchedule = deploymentScheduleRepository.save(deploymentSchedule);
    return deploymentSchedule;
  }

  /**
   * Create a new Deployment schedule event.
   * @param schedule the Deployment schedule
   * @param fileName the name of the file that triggered the event
   * @param fileScope the scope of the file that triggered the event
   * @return the deployment schedule event
   */
  @Transactional
  public DeploymentScheduleEvent createDeploymentScheduleEvent(DeploymentSchedule schedule,
      String fileScope, String fileName) {
    try (MdcUtils.MdcCloseable requestId = MdcUtils
        .setRequestIdCloseable(UUID.randomUUID().toString())) {
      DeploymentScheduleRequest deploymentRequest = DeploymentScheduleRequest
          .deploymentScheduleBuilder()
          .callback(schedule.getCallback())
          .parameters(new HashMap<>(schedule.getParameters()))
          .template(schedule.getTemplate())
          .build();

      OidcTokenId requestedWithToken = Optional.ofNullable(schedule.getRequestedWithToken())
          .map(OidcRefreshToken::getOidcTokenId).orElse(null);
      Deployment deployment = deploymentService
          .createDeployment(deploymentRequest, schedule.getOwner(), requestedWithToken);

      DeploymentScheduleEvent event = new DeploymentScheduleEvent();
      event.setScope(fileScope);
      event.setName(fileName);
      event.setDeployment(deployment);
      deployment.setDeploymentScheduleEvent(event);
      event.setDeploymentSchedule(schedule);
      event.setOwner(schedule.getOwner());
      return deploymentScheduleEventRepository.save(event);
    }
  }

  /**
   * Create a DeploymentScheduleEvent for each DeploymentSchedule listening for the file.
   * @param scope the scope of the file
   * @param name the name of the file
   */
  @Transactional
  public void createDeploymentScheduleEvents(String scope, String name) {
    String did = String.format("%s:%s", scope, name);
    deploymentScheduleRepository
        .findAll()
        .stream()
        // TODO filter with a query DB-side
        .filter(s -> did.matches(s.getFileExpression())
            && s.getStatus() == DeploymentScheduleStatus.RUNNING)
        .filter(s -> s.getScheduleEvents().stream()
            .noneMatch(e -> scope.equals(e.getScope()) && name.equals(e.getName())))
        .forEach(deploymentSchedule -> this
            .createDeploymentScheduleEvent(deploymentSchedule, scope, name));
  }

  /**
   * Get all the deployment schedules of a user.
   * @param pageable the page information
   * @param owner the owner
   * @return the deployment schedules
   */
  @Transactional(readOnly = true)
  public Page<DeploymentSchedule> getDeploymentSchedules(Pageable pageable, String owner) {
    if (owner == null) {
      if (oidcProperties.isEnabled() && isAdmin()) {
        OidcEntity requester = oauth2Tokenservice.generateOidcEntityFromCurrentAuth();
        return deploymentScheduleRepository.findAll(requester, pageable);
      }
      owner = "me";
    }
    OidcEntityId ownerId;
    if ("me".equals(owner)) {
      ownerId = oauth2Tokenservice.generateOidcEntityIdFromCurrentAuth();
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
      OidcEntity requester = oauth2Tokenservice.generateOidcEntityFromCurrentAuth();
      return deploymentScheduleRepository.findAllByOwner(requester, ownerId, pageable);
    } else {
      return deploymentScheduleRepository.findAllByOwner(ownerId, pageable);
    }
  }

  /**
   * Returns the DeploymentSchedule with that id.
   * @param id the id
   * @return the
   */
  @Transactional(readOnly = true)
  public DeploymentSchedule getDeploymentSchedule(String id) {
    DeploymentSchedule deploymentSchedule = null;
    if (oidcProperties.isEnabled()) {
      OidcEntity requester = oauth2Tokenservice.generateOidcEntityFromCurrentAuth();
      deploymentSchedule = deploymentScheduleRepository.findOne(requester, id);
    } else {
      deploymentSchedule = deploymentScheduleRepository.findOne(id);
    }
    return Optional.ofNullable(deploymentSchedule)
        .orElseThrow(() -> new NotFoundException("The deployment <" + id + "> doesn't exist"));
  }

  public Page<DeploymentScheduleEvent> getDeploymentScheduleEvents(String id, Pageable pageable) {
    return deploymentScheduleEventRepository.findByDeploymentSchedule_Id(id, pageable);
  }

}

