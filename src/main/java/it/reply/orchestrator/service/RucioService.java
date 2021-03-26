/*
 * Copyright © 2015-2021 I.N.F.N.
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
import it.reply.orchestrator.exception.OrchestratorException;
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
  private OAuth2TokenService oauth2Tokenservice;

  @Autowired
  private DeploymentScheduleEventRepository deploymentScheduleEventRepository;

  @Autowired
  private ReplicationRuleRepository replicationRuleRepository;

  private <R> R executeWithClientForResult(OidcTokenId oidcTokenId,
      RuntimeThrowingFunction<Rucio, R> function) {
    return oauth2Tokenservice.executeWithClientForResult(
        oidcTokenId,
        accessToken -> function.apply(
            RucioClient.getInstanceWithOidcAuth(rucioProperties.getUrl().toString(), accessToken)),
        UnauthorizedException.class::isInstance);
  }

  /**
   * Create a replication rule.
   * @param oidcTokenId the token id
   * @param account the rucio account
   * @param scope the scope
   * @param name the file nale
   * @param replicationExpression the rse espression
   * @param copies the numper of copies
   * @return the rule id
   */
  public String createReplicationRule(OidcTokenId oidcTokenId, String account, String scope,
      String name, String replicationExpression, int copies) {
    ArrayList<Did> did = Lists.newArrayList(new Did(scope, name));
    RuleCreation ruleCreation =
        new RuleCreation(did, account, copies, replicationExpression, ENABLE_NOTIFICATIONS, true);
    List<String> ruleIds =
        executeWithClientForResult(oidcTokenId, client -> client.createRule(ruleCreation));
    return ruleIds.get(0);
  }

  public RuleInformation getReplicationRule(OidcTokenId oidcTokenId, String ruleId) {
    return executeWithClientForResult(oidcTokenId, client -> client.getRule(ruleId));
  }

  /**
   * Get the replication rules.
   * @param oidcTokenId the token id
   * @param account the rucio account
   * @param scope the scope
   * @param name the file nale
   * @param replicationExpression the rse espression
   * @param copies the numper of copies
   * @return the rules
   */
  public List<RuleInformation> getReplicationRules(OidcTokenId oidcTokenId, String account,
      String scope, String name, String replicationExpression, int copies) {
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

  /**
   * Set the lifetime of a rule.
   * @param oidcTokenId the token id
   * @param ruleId the rule id
   * @param lifetime the lifetime
   */
  public void setLifetime(OidcTokenId oidcTokenId, String ruleId, Integer lifetime) {
    executeWithClientForResult(oidcTokenId, client -> {
      client.updateRule(ruleId, new RuleUpdate(lifetime));
      return null;
    });
  }

  /**
   * Sync the temp replication rule with the DB.
   * @param oidcTokenId the token id
   * @param deploymentId the deployment id
   * @return the DB entity
   */
  public ReplicationRule syncTempReplicationRule(OidcTokenId oidcTokenId, String deploymentId) {
    DeploymentScheduleEvent deploymentScheduleEvent =
        deploymentScheduleEventRepository.findByDeploymentId(deploymentId);
    return syncReplicationRule(oidcTokenId, deploymentScheduleEvent.getTempReplicationRule());
  }

  /**
   * Sync the main replication rule with the DB.
   * @param oidcTokenId the token id
   * @param deploymentId the deployment id
   * @return the DB entity
   */
  public ReplicationRule syncMainReplicationRule(OidcTokenId oidcTokenId, String deploymentId) {
    DeploymentScheduleEvent deploymentScheduleEvent =
        deploymentScheduleEventRepository.findByDeploymentId(deploymentId);
    return syncReplicationRule(oidcTokenId, deploymentScheduleEvent.getMainReplicationRule());
  }

  /**
   * Sync a replication rule with the DB.
   * @param oidcTokenId the token id
   * @param replicationRule the entity
   * @return the DB entity
   */
  public ReplicationRule syncReplicationRule(OidcTokenId oidcTokenId,
      ReplicationRule replicationRule) {
    String ruleId = replicationRule.getRucioId();
    RuleInformation ruleInformation =
        executeWithClientForResult(oidcTokenId, client -> client.getRule(ruleId));
    fillFromRuleInformation(replicationRule, ruleInformation);
    return replicationRule;
  }

  /**
   * Create a main replication rule.
   * @param oidcTokenId the token id
   * @param deploymentId the deployment id
   * @return the replication rule
   */
  @Transactional
  public ReplicationRule getOrCreateMainReplicationRule(OidcTokenId oidcTokenId,
      String deploymentId) {
    DeploymentScheduleEvent deploymentScheduleEvent =
        deploymentScheduleEventRepository.findByDeploymentId(deploymentId);
    DeploymentSchedule deploymentSchedule = deploymentScheduleEvent.getDeploymentSchedule();
    String rucioAccount = getRucioAccount(oidcTokenId).getAccount();
    String scope = deploymentScheduleEvent.getScope();
    String name = deploymentScheduleEvent.getName();
    String replicationExpression = deploymentSchedule.getReplicationExpression();
    Integer numberOfReplicas = deploymentSchedule.getNumberOfReplicas();
    OidcEntity owner = deploymentScheduleEvent.getOwner();
    ReplicationRule replicationRule =
        getOrCreateReplicationRule(oidcTokenId, rucioAccount, scope, name, replicationExpression,
            numberOfReplicas, owner);
    deploymentScheduleEvent.setMainReplicationRule(replicationRule);
    return replicationRule;
  }

  @NotNull
  private AccountInformation getRucioAccount(OidcTokenId oidcTokenId) {
    return executeWithClientForResult(oidcTokenId, client -> client.getAccount(WHO_AM_I));
  }

  /**
   * Create a temp replication rule.
   * @param oidcTokenId the token id
   * @param deploymentId the deployment id
   * @param rse the rse
   * @return the replication rule
   */
  @Transactional
  public ReplicationRule getOrCreateTempReplicationRule(OidcTokenId oidcTokenId,
      String deploymentId, String rse) {
    DeploymentScheduleEvent deploymentScheduleEvent =
        deploymentScheduleEventRepository.findByDeploymentId(deploymentId);
    String rucioAccount = getRucioAccount(oidcTokenId).getAccount();
    String scope = deploymentScheduleEvent.getScope();
    String name = deploymentScheduleEvent.getName();
    Integer numberOfReplicas = 1;
    OidcEntity owner = deploymentScheduleEvent.getOwner();
    ReplicationRule replicationRule =
        getOrCreateReplicationRule(oidcTokenId, rucioAccount, scope, name, rse, numberOfReplicas,
            owner);
    deploymentScheduleEvent.setTempReplicationRule(replicationRule);
    return replicationRule;
  }

  /**
   * Get or create a rule on Rucio.
   * @param oidcTokenId the token id
   * @param whoAmI the rucio account
   * @param scope the scope
   * @param name the name
   * @param replicationExpression the rse expression
   * @param numberOfReplicas the numper of replicas
   * @param owner the onwer
   * @return the replication rule
   */
  @Transactional
  protected ReplicationRule getOrCreateReplicationRule(OidcTokenId oidcTokenId, String whoAmI,
      String scope, String name, String replicationExpression, Integer numberOfReplicas,
      OidcEntity owner) {
    try {
      String ruleId = createReplicationRule(oidcTokenId, whoAmI, scope, name, replicationExpression,
          numberOfReplicas);
      RuleInformation ruleInformation = getReplicationRule(oidcTokenId, ruleId);
      ReplicationRule replicationRule = new ReplicationRule();
      fillFromRuleInformation(replicationRule, ruleInformation);
      replicationRule.setOwner(owner);
      return replicationRuleRepository.save(replicationRule);
    } catch (DuplicateRuleException ex) {
      RuleInformation ruleInformation =
          getReplicationRules(oidcTokenId, whoAmI, scope, name, replicationExpression,
              numberOfReplicas).get(0);
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

  /**
   * Decrease the usage count and delete a replication rule if unused.
   * @param oidcTokenId the token id
   * @param replicationRule the replication rule
   */
  public void deleteReplicationRuleIfUnused(OidcTokenId oidcTokenId,
      ReplicationRule replicationRule) {
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

  private void fillFromRuleInformation(ReplicationRule replicationRule,
      RuleInformation ruleInformation) {
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
      default:
        throw new OrchestratorException("Unknown replication rule status "
            + ruleInformation.getState());
    }
  }

  /**
   * Get the pfn of a replica in a rse if present.
   * @param oidcTokenId the token id
   * @param scope the scope
   * @param name the name
   * @param desiredRse the rse
   * @return the optional pfn
   */
  public Optional<String> getPfnFromReplica(OidcTokenId oidcTokenId, String scope, String name,
      String desiredRse) {
    ReplicaInformation replicaInformation =
        executeWithClientForResult(oidcTokenId, client -> client.getReplica(scope, name));
    return replicaInformation
        .getRses()
        .entrySet()
        .stream()
        .filter(entry -> desiredRse.equals(entry.getKey()))
        .flatMap(entry -> entry.getValue().stream())
        .findFirst();
  }
}
