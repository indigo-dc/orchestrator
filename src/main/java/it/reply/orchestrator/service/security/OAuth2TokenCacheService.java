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

package it.reply.orchestrator.service.security;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.dal.entity.OidcRefreshToken;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dal.repository.OidcTokenRepository;
import it.reply.orchestrator.dto.security.AccessGrant;
import it.reply.orchestrator.dto.security.TokenIntrospectionResponse;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.utils.JwtUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class OAuth2TokenCacheService {

  private CustomOAuth2TemplateFactory customOAuth2TemplateFactory;

  private OidcTokenRepository oidcTokenRepository;

  private final TransactionTemplate transactionTemplate;

  /**
   * Creates a new OAuth2TokenCacheService.
   * 
   * @param customOAuth2TemplateFactory
   *          the customOAuth2TemplateFactory
   * @param oidcTokenRepository
   *          the oidcTokenRepository.
   * @param transactionManager
   *          the transactionManager.
   */
  public OAuth2TokenCacheService(CustomOAuth2TemplateFactory customOAuth2TemplateFactory,
      OidcTokenRepository oidcTokenRepository, PlatformTransactionManager transactionManager) {
    this.customOAuth2TemplateFactory = customOAuth2TemplateFactory;
    this.oidcTokenRepository = oidcTokenRepository;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);

  }

  private LoadingCache<OidcTokenId, AccessGrant> oauth2TokensCache = CacheBuilder
      .newBuilder()
      .maximumWeight(0)
      .<OidcTokenId, AccessGrant>weigher((key, grant) -> grant.isExpired() ? 1 : 0)
      .refreshAfterWrite(1, TimeUnit.MINUTES)
      .removalListener(
          removalEvent -> LOG.trace("Access token {} evicted from cache.%nEviction cause {}",
              removalEvent.getValue().getAccessToken(), removalEvent.getCause()))
      .build(new CacheLoader<OidcTokenId, AccessGrant>() {

        @Override
        public ListenableFuture<AccessGrant> reload(OidcTokenId key, AccessGrant oldGrant) {
          if (oldGrant.isExpiringIn(Duration.ofMinutes(2))) {
            // reload if expiring in 2 mins
            // TODO should the timing be configurable?
            return Futures.immediateFuture(load(key));
          } else {
            return Futures.immediateFuture(oldGrant);
          }
        }

        @Override
        public AccessGrant load(OidcTokenId key) {
          return refreshAccessToken(key);
        }

      });

  /**
   * Refresh an access token.
   * 
   * @param id
   *          the id of the token
   * @param scopes
   *          the scopes to request
   * @return the exchanged grant
   */
  protected AccessGrant refreshAccessToken(OidcTokenId id) {
    CustomOAuth2Template template = customOAuth2TemplateFactory.generateOAuth2Template(id);
    return transactionTemplate.execute(transactionStatus -> {
      OidcRefreshToken refreshToken =
          oidcTokenRepository
              .findByOidcTokenId(id)
              .orElseThrow(() -> new OrchestratorException("No refresh token suitable found"));
      AccessGrant newGrant =
          template.refreshToken(refreshToken.getVaule(), OidcProperties.REQUIRED_SCOPES);
      LOG.info("Access token for {} refreshed", id);
      if (newGrant.getRefreshToken() != null
          && !newGrant.getRefreshToken().equals(refreshToken.getVaule())) {
        LOG.info("New refesh token received for {}", id);
        refreshToken.updateFromAccessGrant(newGrant);
      }
      return newGrant;
    });
  }

  protected AccessGrant exchangeAccessToken(OidcTokenId id, String accessToken) {
    CustomOAuth2Template template = customOAuth2TemplateFactory.generateOAuth2Template(id);
    transactionTemplate.execute(transactionStatus -> {
      return oauth2TokensCache.asMap().compute(id, (tokenId, oldGrant) -> {
        Optional<OidcRefreshToken> refreshToken = oidcTokenRepository
            .findByOidcTokenId(id);
        if (refreshToken.isPresent()) {
          boolean isActive = refreshToken
              .map(token -> template.introspectToken(token.getVaule()))
              .filter(TokenIntrospectionResponse::isActive)
              .isPresent();
          if (!isActive) {
            LOG.info(
                "Refresh token for {} isn't active anymore."
                    + " Getting a new one exchanging access token with jti={}",
                id, JwtUtils.getJti(JwtUtils.parseJwt(accessToken)));
            AccessGrant grant = template.exchangeToken(accessToken, OidcProperties.REQUIRED_SCOPES);
            refreshToken.get().updateFromAccessGrant(grant);
            return grant;
          } else {
            return oldGrant;
          }
        } else {
          LOG.info("No refresh token found for {}. Exchanging access token with jti={}",
              id, JwtUtils.getJti(JwtUtils.parseJwt(accessToken)));
          AccessGrant grant = template.exchangeToken(accessToken, OidcProperties.REQUIRED_SCOPES);
          OidcRefreshToken token = OidcRefreshToken.createFromAccessGrant(grant, id);
          oidcTokenRepository.save(token);
          return grant;
        }
      });
    });
    return oauth2TokensCache.getUnchecked(id);
  }

  /**
   * Put a grant in the cache.
   * 
   * @param id
   *          the id of token from which the grant originates.
   * @param accessToken
   *          the grant
   */
  public void put(OidcTokenId id, AccessGrant accessToken) {
    Preconditions.checkNotNull(id);
    Preconditions.checkNotNull(accessToken);
    LOG.debug("Putting new access token for {} into cache", id);
    oauth2TokensCache.put(id, accessToken);
  }

  /**
   * Get a grant from the cache.
   * 
   * @param id
   *          the id of token from which the grant must originates.
   * @return the grant
   */
  public AccessGrant get(OidcTokenId id) {
    Preconditions.checkNotNull(id);
    LOG.debug("Retrieving access token for {} from cache", id);
    return oauth2TokensCache.getUnchecked(id);
  }

  /**
   * Get a refreshed grant from the cache.
   * 
   * @param id
   *          the id of token from which the grant must originates.
   * @return the grant
   */
  public AccessGrant getNew(OidcTokenId id) {
    Preconditions.checkNotNull(id);
    // TODO make it atomic
    evict(id);
    return get(id);
  }

  /**
   * Evict a grant from the cache.
   * 
   * @param id
   *          the id of token from which the grant must originates.
   */
  public void evict(OidcTokenId id) {
    Preconditions.checkNotNull(id);
    LOG.debug("Evicting access token for {} from cache", id);
    oauth2TokensCache.invalidate(id);
  }

}
