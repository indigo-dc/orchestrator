package it.reply.orchestrator.service;

import com.google.common.collect.Lists;
import it.infn.ba.xdc.rucio.client.Rucio;
import it.infn.ba.xdc.rucio.client.RucioClient;
import it.infn.ba.xdc.rucio.client.exceptions.DuplicateRuleException;
import it.infn.ba.xdc.rucio.client.exceptions.NotFoundException;
import it.infn.ba.xdc.rucio.client.exceptions.UnauthorizedException;
import it.infn.ba.xdc.rucio.client.model.AccountInformation;
import it.infn.ba.xdc.rucio.client.model.Did;
import it.infn.ba.xdc.rucio.client.model.ReplicaInformation;
import it.infn.ba.xdc.rucio.client.model.RuleCreation;
import it.infn.ba.xdc.rucio.client.model.RuleInformation;
import it.infn.ba.xdc.rucio.client.model.RuleUpdate;
import it.reply.orchestrator.config.properties.RucioProperties;
import it.reply.orchestrator.dal.entity.DeploymentSchedule;
import it.reply.orchestrator.dal.entity.DeploymentScheduleEvent;
import it.reply.orchestrator.dal.entity.OidcEntity;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dal.entity.ReplicationRule;
import it.reply.orchestrator.dal.repository.DeploymentScheduleEventRepository;
import it.reply.orchestrator.dal.repository.ReplicationRuleRepository;
import it.reply.orchestrator.enums.ReplicationRuleStatus;
import it.reply.orchestrator.function.RuntimeThrowingFunction;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@EnableConfigurationProperties(RucioProperties.class)
public class RucioService {

  public static final String WHO_AM_I = "whoami";
  public static final String ENABLE_NOTIFICATIONS = "Y";

  @Autowired
  private RucioProperties rucioProperties;

  @Autowired
  private OAuth2TokenService oAuth2TokenService;

  @Autowired
  private DeploymentScheduleEventRepository deploymentScheduleEventRepository;

  @Autowired
  private ReplicationRuleRepository replicationRuleRepository;

  private <R> R executeWithClientForResult(OidcTokenId oidcTokenId, RuntimeThrowingFunction<Rucio, R> function) {
    return oAuth2TokenService.executeWithClientForResult(
      oidcTokenId,
      accessToken -> function.apply(RucioClient.getInstanceWithOidcAuth(rucioProperties.getUrl().toString(), accessToken)),
      UnauthorizedException.class::isInstance);
  }

  public String createReplicationRule(OidcTokenId oidcTokenId, String account, String scope, String name, String replicationExpression, int copies) {
    ArrayList<Did> did = Lists.newArrayList(new Did(scope, name));
    RuleCreation ruleCreation = new RuleCreation(did, account , copies, replicationExpression, ENABLE_NOTIFICATIONS, true);
    List<String> ruleIds = executeWithClientForResult(oidcTokenId, client -> client.createRule(ruleCreation));
    return ruleIds.get(0);
  }

  public RuleInformation getReplicationRule(OidcTokenId oidcTokenId, String ruleId) {
    return executeWithClientForResult(oidcTokenId, client -> client.getRule(ruleId));
  }

  public List<RuleInformation> getReplicationRules(OidcTokenId oidcTokenId, String account, String scope, String name, String replicationExpression, int copies) {
    Map<String, String> params = new HashMap<>();
    params.put("account", account);
    params.put("name", name);
    params.put("scope", scope);
    params.put("copies", String.valueOf(copies));
    params.put("rse_expression", replicationExpression);
    return executeWithClientForResult(oidcTokenId, client -> client.getRules(params));
  }

  public void deleteReplicationRule(OidcTokenId oidcTokenId, String ruleId) {
//    setLifetime(oidcTokenId, ruleId, 1);
  }

  public void restoreReplicationRule(OidcTokenId oidcTokenId, String ruleId) {
   // setLifetime(oidcTokenId, ruleId, null);
  }

  public void setLifetime(OidcTokenId oidcTokenId, String ruleId, Integer lifetime) {
    executeWithClientForResult(oidcTokenId, client -> {
      client.updateRule(ruleId, new RuleUpdate(lifetime));
      return null;
    });
  }

  public ReplicationRule syncTempReplicationRule(OidcTokenId oidcTokenId, String deploymentId) {
    DeploymentScheduleEvent deploymentScheduleEvent = deploymentScheduleEventRepository.findByDeploymentId(deploymentId);
    return syncReplicationRule(oidcTokenId, deploymentScheduleEvent.getTempReplicationRule());
  }

  public ReplicationRule syncMainReplicationRule(OidcTokenId oidcTokenId, String deploymentId) {
    DeploymentScheduleEvent deploymentScheduleEvent = deploymentScheduleEventRepository.findByDeploymentId(deploymentId);
    return syncReplicationRule(oidcTokenId, deploymentScheduleEvent.getMainReplicationRule());
  }

  public ReplicationRule syncReplicationRule(OidcTokenId oidcTokenId, ReplicationRule replicationRule) {
    String ruleId = replicationRule.getRucioId();
    RuleInformation ruleInformation = executeWithClientForResult(oidcTokenId, client -> client.getRule(ruleId));
    fillFromRuleInformation(replicationRule, ruleInformation);
    return replicationRule;
  }

  @Transactional
  public ReplicationRule getOrCreateMainReplicationRule(OidcTokenId oidcTokenId, String deploymentId) {
    DeploymentScheduleEvent deploymentScheduleEvent = deploymentScheduleEventRepository.findByDeploymentId(deploymentId);
    DeploymentSchedule deploymentSchedule = deploymentScheduleEvent.getDeploymentSchedule();
    String rucioAccount = getRucioAccount(oidcTokenId).getAccount();
    String scope = deploymentScheduleEvent.getScope();
    String name = deploymentScheduleEvent.getName();
    String replicationExpression = deploymentSchedule.getReplicationExpression();
    Integer numberOfReplicas = deploymentSchedule.getNumberOfReplicas();
    OidcEntity owner = deploymentScheduleEvent.getOwner();
    ReplicationRule replicationRule = getOrCreateReplicationRule(oidcTokenId, rucioAccount, scope, name, replicationExpression, numberOfReplicas, owner);
    deploymentScheduleEvent.setMainReplicationRule(replicationRule);
    return replicationRule;
  }

  @NotNull
  private AccountInformation getRucioAccount(OidcTokenId oidcTokenId) {
    return executeWithClientForResult(oidcTokenId, client -> client.getAccount(WHO_AM_I));
  }

  @Transactional
  public ReplicationRule getOrCreateTempReplicationRule(OidcTokenId oidcTokenId, String deploymentId, String rse) {
    DeploymentScheduleEvent deploymentScheduleEvent = deploymentScheduleEventRepository.findByDeploymentId(deploymentId);
    String rucioAccount = getRucioAccount(oidcTokenId).getAccount();
    String scope = deploymentScheduleEvent.getScope();
    String name = deploymentScheduleEvent.getName();
    Integer numberOfReplicas = 1;
    OidcEntity owner = deploymentScheduleEvent.getOwner();
    ReplicationRule replicationRule = getOrCreateReplicationRule(oidcTokenId, rucioAccount, scope, name, rse, numberOfReplicas, owner);
    deploymentScheduleEvent.setTempReplicationRule(replicationRule);
    return replicationRule;
  }

  @Transactional
  protected ReplicationRule getOrCreateReplicationRule(OidcTokenId oidcTokenId, String whoAmI, String scope, String name, String replicationExpression, Integer numberOfReplicas, OidcEntity owner) {
    try {
      String ruleId = createReplicationRule(oidcTokenId, whoAmI, scope, name, replicationExpression, numberOfReplicas);
      RuleInformation ruleInformation = getReplicationRule(oidcTokenId, ruleId);
      ReplicationRule replicationRule = new ReplicationRule();
      fillFromRuleInformation(replicationRule, ruleInformation);
      replicationRule.setOwner(owner);
      return replicationRuleRepository.save(replicationRule);
    } catch (DuplicateRuleException ex) {
      RuleInformation ruleInformation = getReplicationRules(oidcTokenId, whoAmI, scope, name, replicationExpression, numberOfReplicas).get(0);
      String ruleId = ruleInformation.getId();
      if (ruleInformation.getExpiresAt() != null) {
        restoreReplicationRule(oidcTokenId, ruleId);
      }
      ReplicationRule replicationRule = replicationRuleRepository.findByRucioId(ruleId);
      if (replicationRule == null) {
        replicationRule = new ReplicationRule();
        fillFromRuleInformation(replicationRule, ruleInformation);
        replicationRule.setOwner(owner);
        replicationRuleRepository.save(replicationRule);
      } else {
        fillFromRuleInformation(replicationRule, ruleInformation);
        replicationRule.setUsageCount(replicationRule.getUsageCount() + 1);
        replicationRule.setDeleted(false);
      }
      return replicationRule;
    }
  }

  public void deleteReplicationRuleIfUnused(OidcTokenId oidcTokenId, ReplicationRule replicationRule) {
    replicationRule.setUsageCount(Math.max(replicationRule.getUsageCount() - 1, 0));
    String ruleId = replicationRule.getRucioId();
    if (replicationRule.getUsageCount() == 0) {
      try {
        deleteReplicationRule(oidcTokenId, ruleId);
      } catch (NotFoundException ex) {
        LOG.info("Rule {} was already deleted", ruleId);
      }
      replicationRule.setDeleted(true);
    }
  }

  private void fillFromRuleInformation(ReplicationRule replicationRule, RuleInformation ruleInformation) {
    replicationRule.setRucioId(ruleInformation.getId());
    replicationRule.setScope(ruleInformation.getScope());
    replicationRule.setName(ruleInformation.getName());
    replicationRule.setNumberOfReplicas(ruleInformation.getCopies());
    replicationRule.setRucioAccount(ruleInformation.getAccount());
    replicationRule.setReplicationExpression(ruleInformation.getRseExpression());
    switch (ruleInformation.getState()) {
      case OK:
        replicationRule.setStatus(ReplicationRuleStatus.OK);
        replicationRule.setStatusReason(null);
        break;
      case STUCK:
        replicationRule.setStatus(ReplicationRuleStatus.STUCK);
        replicationRule.setStatusReason(ruleInformation.getError());
        break;
      case REPLICATING:
        replicationRule.setStatus(ReplicationRuleStatus.REPLICATING);
        replicationRule.setStatusReason(null);
        break;
    }
  }

  public Optional<String> getPfnFromReplica(OidcTokenId oidcTokenId, String scope, String name, String desiredRse) {
    ReplicaInformation replicaInformation = executeWithClientForResult(oidcTokenId, client -> client.getReplica(scope, name));
    return replicaInformation
      .getRses()
      .entrySet()
      .stream()
      .filter(entry -> desiredRse.equals(entry.getKey()))
      .flatMap(entry -> entry.getValue().stream())
      .findFirst();
  }
}
