package it.reply.orchestrator.service;

import alien4cloud.tosca.model.ArchiveRoot;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.DeploymentSchedule;
import it.reply.orchestrator.dal.entity.DeploymentScheduleEvent;
import it.reply.orchestrator.dal.entity.OidcEntity;
import it.reply.orchestrator.dal.entity.OidcEntityId;
import it.reply.orchestrator.dal.entity.OidcRefreshToken;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.entity.WorkflowReference;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dal.repository.DeploymentScheduleEventRepository;
import it.reply.orchestrator.dal.repository.DeploymentScheduleRepository;
import it.reply.orchestrator.dal.repository.OidcTokenRepository;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.policies.ToscaPolicy;
import it.reply.orchestrator.dto.request.DeploymentRequest;
import it.reply.orchestrator.dto.request.DeploymentScheduleRequest;
import it.reply.orchestrator.dto.security.IndigoOAuth2Authentication;
import it.reply.orchestrator.dto.security.IndigoUserInfo;
import it.reply.orchestrator.enums.DeploymentScheduleStatus;
import it.reply.orchestrator.enums.DeploymentType;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.exception.http.BadRequestException;
import it.reply.orchestrator.exception.http.NotFoundException;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderServiceRegistry;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.orchestrator.utils.CommonUtils;
import it.reply.orchestrator.utils.MdcUtils;
import it.reply.orchestrator.utils.ToscaConstants;
import it.reply.orchestrator.utils.WorkflowConstants;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class DeploymentScheduleServiceImpl {

  @Autowired
  private DeploymentService deploymentService;

  @Autowired
  private ToscaService toscaService;

  @Autowired
  private OidcProperties oidcProperties;

  @Autowired
  private DeploymentScheduleEventRepository deploymentScheduleEventRepository;


  private static final Pattern OWNER_PATTERN = Pattern.compile("([^@]+)@([^@]+)");

  private boolean isAdmin() {
    boolean isAdmin = false;
    if (oidcProperties.isEnabled()) {
      OidcEntity requester = oAuth2TokenService.generateOidcEntityFromCurrentAuth();
      String issuer = requester.getOidcEntityId().getIssuer();
      String group = oidcProperties.getIamProperties().get(issuer).getAdmingroup();
      IndigoOAuth2Authentication authentication = oAuth2TokenService.getCurrentAuthentication();
      IndigoUserInfo userInfo = (IndigoUserInfo) authentication.getUserInfo();
      if (userInfo != null) {
        isAdmin = userInfo.getGroups().contains(group);
      }
    }
    return isAdmin;
  }

  @Autowired
  private DeploymentScheduleRepository deploymentScheduleRepository;

  @Autowired
  private OAuth2TokenService oAuth2TokenService;

  @Autowired
  private OidcTokenRepository oidcTokenRepository;


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
      deploymentSchedule.setOwner(oAuth2TokenService.getOrGenerateOidcEntityFromCurrentAuth());
      Optional<OidcRefreshToken> oidcRefreshToken = oidcTokenRepository.findByOidcTokenId(oAuth2TokenService.exchangeCurrentAccessToken());
      deploymentSchedule.setRequestedWithToken(oidcRefreshToken.orElse(null));
    }
    deploymentSchedule = deploymentScheduleRepository.save(deploymentSchedule);
    return deploymentSchedule;
  }



  @Transactional
  public DeploymentScheduleEvent createDeploymentScheduleEvent(DeploymentSchedule schedule, String fileScope, String fileName) {
    try(MdcUtils.MdcCloseable requestId = MdcUtils.setRequestIdCloseable(UUID.randomUUID().toString())) {
      DeploymentScheduleRequest deploymentRequest = DeploymentScheduleRequest
        .deploymentScheduleBuilder()
        .callback(schedule.getCallback())
        .parameters(new HashMap<>(schedule.getParameters()))
        .template(schedule.getTemplate())
        .build();

      OidcTokenId requestedWithToken = Optional.ofNullable(schedule.getRequestedWithToken()).map(OidcRefreshToken::getOidcTokenId).orElse(null);
      Deployment deployment = deploymentService.createDeployment(deploymentRequest, schedule.getOwner(), requestedWithToken);

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

  @Transactional
  public void createDeploymentScheduleEvents(String scope, String name) {
    String did = String.format("%s:%s", scope, name);
    deploymentScheduleRepository
      .findAll()
      .stream()
      // TODO filter with a query DB-side
      .filter(s -> did.matches(s.getFileExpression()) && s.getStatus() == DeploymentScheduleStatus.RUNNING)
      .filter(s -> s.getScheduleEvents().stream().noneMatch(e -> scope.equals(e.getScope()) && name.equals(e.getName())))
      .forEach(deploymentSchedule -> this.createDeploymentScheduleEvent(deploymentSchedule, scope, name));
  }

    @Transactional(readOnly = true)
    public Page<DeploymentSchedule> getDeploymentSchedules(Pageable pageable, String owner) {
      if (owner == null) {
        if (oidcProperties.isEnabled() && isAdmin()) {
          OidcEntity requester = oAuth2TokenService.generateOidcEntityFromCurrentAuth();
          return deploymentScheduleRepository.findAll(requester, pageable);
        }
        owner = "me";
      }
      OidcEntityId ownerId;
      if ("me".equals(owner)) {
        ownerId = oAuth2TokenService.generateOidcEntityIdFromCurrentAuth();
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
        OidcEntity requester = oAuth2TokenService.generateOidcEntityFromCurrentAuth();
        return deploymentScheduleRepository.findAllByOwner(requester, ownerId, pageable);
      } else {
        return deploymentScheduleRepository.findAllByOwner(ownerId, pageable);
      }
    }

    @Transactional(readOnly = true)
  public DeploymentSchedule getDeploymentSchedule(String id) {
      DeploymentSchedule deploymentSchedule = null;
      if (oidcProperties.isEnabled()) {
        OidcEntity requester = oAuth2TokenService.generateOidcEntityFromCurrentAuth();
        deploymentSchedule = deploymentScheduleRepository.findOne(requester, id);
      } else {
        deploymentSchedule = deploymentScheduleRepository.findOne(id);
      }
      return Optional.ofNullable(deploymentSchedule).orElseThrow(() ->new NotFoundException("The deployment <" + id + "> doesn't exist"));
      }

  public Page<DeploymentScheduleEvent> getDeploymentScheduleEvents(String id, Pageable pageable) {
    return deploymentScheduleEventRepository.findByDeploymentSchedule_Id(id, pageable);
  }

}

