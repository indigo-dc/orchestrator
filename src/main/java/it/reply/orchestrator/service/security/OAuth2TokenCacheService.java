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

package it.reply.orchestrator.service.security;

import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.dal.entity.OidcRefreshToken;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dal.repository.OidcTokenRepository;
import it.reply.orchestrator.dto.security.AccessGrant;
import it.reply.orchestrator.dto.security.TokenIntrospectionResponse;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.utils.JwtUtils;
import it.reply.orchestrator.utils.MdcUtils;
import it.reply.orchestrator.utils.MdcUtils.MdcCloseable;

import java.io.Serializable;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import javax.cache.Cache;
import javax.cache.Cache.Entry;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;

import lombok.extern.slf4j.Slf4j;

import org.apache.ignite.Ignite;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.eviction.EvictionFilter;
import org.apache.ignite.cache.eviction.lru.LruEvictionPolicy;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.resources.SpringApplicationContextResource;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class OAuth2TokenCacheService {

  public static final String CACHE_NAME = "oauth2_tokens";

  private Cache<OidcTokenId, AccessGrant> oauth2TokensCache;

  public static class TokenEvictionFilter implements EvictionFilter<OidcTokenId, AccessGrant> {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evictAllowed(Entry<OidcTokenId, AccessGrant> entry) {
      return entry.getValue() == null || entry.getValue().isExpired();
    }
  }

  /**
   * Creates a new OAuth2TokenCacheService.
   *
   * @param ignite
   *          the {@link Ignite} instance.
   */
  public OAuth2TokenCacheService(@NonNull Ignite ignite) {

    CacheConfiguration<OidcTokenId, AccessGrant> oauth2CacheCfg =
        new CacheConfiguration<OidcTokenId, AccessGrant>(CACHE_NAME)
            .setEvictionFilter(new TokenEvictionFilter())
            .setEvictionPolicy(new LruEvictionPolicy<OidcTokenId, AccessGrant>(1_000))
            .setAtomicityMode(CacheAtomicityMode.ATOMIC)
            .setOnheapCacheEnabled(true)
            .setCacheMode(CacheMode.PARTITIONED)
            .setBackups(2)
            .setReadFromBackup(true);

    oauth2TokensCache = ignite.getOrCreateCache(oauth2CacheCfg);
  }

  /**
   * Exchanges an access token, puts the refresh token in DB and the exchanged access token in
   * cache.
   *
   * @param id
   *          the ID of the access token in the cache
   * @param accessToken
   *          the access token to exchange
   * @return the received access grant
   */
  public AccessGrant exchangeAccessToken(OidcTokenId id, String accessToken) {
    oauth2TokensCache.invoke(id, exchangeEntryProcessor(), MdcUtils.getRequestId(),
        MdcUtils.getDeploymentId(), accessToken);
    return get(id);
  }

  public AccessGrant get(OidcTokenId id) {
    return oauth2TokensCache.invoke(id, getEntryProcessor(), MdcUtils.getRequestId(),
        MdcUtils.getDeploymentId());
  }

  public AccessGrant getNew(OidcTokenId id) {
    return oauth2TokensCache.invoke(id, getNewEntryProcessor(), MdcUtils.getRequestId(),
        MdcUtils.getDeploymentId());
  }

  protected EntryProcessor<OidcTokenId, AccessGrant, AccessGrant> exchangeEntryProcessor() {
    return new EntryProcessorMdcDecorator<>(ExchangeEntryProcessor.class);
  }

  protected EntryProcessor<OidcTokenId, AccessGrant, AccessGrant> getEntryProcessor() {
    return new EntryProcessorMdcDecorator<>(GetEntryProcessor.class);
  }

  protected EntryProcessor<OidcTokenId, AccessGrant, AccessGrant> getNewEntryProcessor() {
    return new EntryProcessorMdcDecorator<>(GetNewEntryProcessor.class);
  }

  @Component
  public static class ExchangeEntryProcessor extends AbstractGetEntryProcessor {

    @Override
    public AccessGrant processInternal(@NonNull MutableEntry<OidcTokenId, AccessGrant> entry,
        Object... arguments) {
      String accessToken = (String) arguments[0];
      return exchange(entry, accessToken);
    }
  }

  @Component
  public static class GetEntryProcessor extends AbstractGetEntryProcessor {

    @Override
    public AccessGrant processInternal(@NonNull MutableEntry<OidcTokenId, AccessGrant> entry,
        Object... arguments) {
      OidcTokenId id = entry.getKey();
      LOG.debug("Retrieving access token for {} from cache", id);
      AccessGrant oldGrant = entry.getValue();
      Duration expirationDuration = Duration.ofMinutes(5);
      if (oldGrant == null) {
        LOG.info("No access token for {} available. Refeshing and populating cache", id);
        return refresh(entry);
      } else if (oldGrant.isExpiringIn(expirationDuration)) {
        LOG.info(
            "Refreshing access token for {} because the one in cache is expiring in less than {}",
            id, expirationDuration);
        return refresh(entry);
      } else {
        return oldGrant;
      }
    }
  }

  @Component
  public static class GetNewEntryProcessor extends AbstractGetEntryProcessor {

    @Override
    public AccessGrant processInternal(@NonNull MutableEntry<OidcTokenId, AccessGrant> entry,
        Object... arguments) {
      OidcTokenId id = entry.getKey();
      LOG.info("Force refesh of access token for {}", id);
      entry.remove();
      return refresh(entry);
    }
  }

  public abstract static class AbstractGetEntryProcessor
      implements EntryProcessor<OidcTokenId, AccessGrant, AccessGrant> {

    @Autowired
    private OidcTokenRepository oidcTokenRepository;

    @Autowired
    private CustomOAuth2TemplateFactory customOAuth2TemplateFactory;

    @Autowired
    private PlatformTransactionManager transactionManager;

    protected AccessGrant refresh(@NonNull MutableEntry<OidcTokenId, AccessGrant> entry) {
      OidcTokenId id = entry.getKey();
      CustomOAuth2Template template = customOAuth2TemplateFactory.generateOAuth2Template(id);
      AccessGrant newGrant = generateTransactionTemplate().execute(transactionStatus -> {
        OidcRefreshToken refreshToken =
            oidcTokenRepository
                .findByOidcTokenId(id)
                .orElseThrow(
                    () -> new OrchestratorException("No refresh token found for " + id));
        AccessGrant grant =
            template.refreshToken(refreshToken.getValue(), OidcProperties.REQUIRED_SCOPES);
        LOG.info("Access token for {} refreshed", id);
        String newRefreshToken = grant.getRefreshToken();
        if (newRefreshToken != null && !newRefreshToken.equals(refreshToken.getValue())) {
          LOG.info("New refesh token received for {}", id);
          refreshToken.updateFromAccessGrant(grant);
        }
        return grant;
      });
      entry.setValue(newGrant);
      return newGrant;
    }

    protected AccessGrant exchange(@NonNull MutableEntry<OidcTokenId, AccessGrant> entry,
        String accessToken) {
      OidcTokenId id = entry.getKey();
      AccessGrant oldGrant = entry.getValue();
      CustomOAuth2Template template = customOAuth2TemplateFactory.generateOAuth2Template(id);
      return generateTransactionTemplate().execute(transactionStatus -> {
        Optional<OidcRefreshToken> refreshToken = oidcTokenRepository.findByOidcTokenId(id);
        if (refreshToken.isPresent()) {
          boolean isActive = refreshToken
              .map(token -> template.introspectToken(token.getValue()))
              .filter(TokenIntrospectionResponse::isActive)
              .isPresent();
          if (!isActive) {
            LOG.info(
                "Refresh token for {} isn't active anymore."
                    + " Getting a new one exchanging access token with jti={}",
                id, JwtUtils.getJti(JwtUtils.parseJwt(accessToken)));
            AccessGrant grant = template.exchangeToken(accessToken, OidcProperties.REQUIRED_SCOPES);
            refreshToken.get().updateFromAccessGrant(grant);
            entry.setValue(grant);
            return grant;
          } else {
            LOG.info(
                "A valid refresh token for {} is already available. Not exchanging the current one",
                id);
            return oldGrant;
          }
        } else {
          LOG.info("No refresh token found for {}. Exchanging access token with jti={}",
              id, JwtUtils.getJti(JwtUtils.parseJwt(accessToken)));
          AccessGrant grant = template.exchangeToken(accessToken, OidcProperties.REQUIRED_SCOPES);
          OidcRefreshToken token = OidcRefreshToken.createFromAccessGrant(grant, entry.getKey());
          oidcTokenRepository.save(token);
          entry.setValue(grant);
          return grant;
        }
      });
    }

    private TransactionTemplate generateTransactionTemplate() {
      TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
      transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
      transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
      return transactionTemplate;
    }

    @Override
    public AccessGrant process(@NonNull MutableEntry<OidcTokenId, AccessGrant> entry,
        Object... arguments) throws EntryProcessorException {
      return processInternal(entry, arguments);
    }

    public abstract AccessGrant processInternal(
        @NonNull MutableEntry<OidcTokenId, AccessGrant> entry,
        Object... arguments) throws EntryProcessorException;
  }

  // TODO transform in aspect
  @Deprecated
  public static class EntryProcessorMdcDecorator<E extends EntryProcessor<K, V, T>, K, V, T>
      implements EntryProcessor<K, V, T>, Serializable {

    private static final long serialVersionUID = 1L;

    private final Class<E> delegateClass;

    @SpringApplicationContextResource
    private transient ApplicationContext springCtx;

    public EntryProcessorMdcDecorator(@NonNull Class<E> delegateClass) {
      this.delegateClass = Objects.requireNonNull(delegateClass);
    }

    private E getDelegate() {
      return springCtx.getBean(delegateClass);
    }

    @Override
    public T process(@NonNull MutableEntry<K, V> entry, Object... arguments)
        throws EntryProcessorException {
      String requestId = (String) arguments[0];
      String deploymentId = (String) arguments[1];

      try (MdcCloseable oldDeploymentId = MdcUtils.setDeploymentIdCloseable(deploymentId)) {
        try (MdcCloseable oldRequestId = MdcUtils.setRequestIdCloseable(requestId)) {
          return getDelegate().process(entry, Arrays.copyOfRange(arguments, 2, arguments.length));
        }
      }
    }

  }
}
