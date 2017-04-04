package it.reply.orchestrator.service.security;

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

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.exception.OrchestratorException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.oauth2.AccessGrant;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class OAuth2TokenCacheService {

  @Autowired
  private OAuth2TokenService oauth2TokenService;

  private LoadingCache<OidcTokenId, AccessGrant> oauth2TokensCache = CacheBuilder.newBuilder()
      .maximumWeight(0)
      .<OidcTokenId, AccessGrant>weigher((key, value) -> isGrantExpired(value) ? 1 : 0)
      .refreshAfterWrite(1, TimeUnit.MINUTES)
      .removalListener(
          removalEvent -> LOG.debug("Access token {} evicted from cache.%nEviction cause {}",
              removalEvent.getValue().getAccessToken(), removalEvent.getCause()))
      .build(new CacheLoader<OidcTokenId, AccessGrant>() {

        @Override
        public ListenableFuture<AccessGrant> reload(OidcTokenId key, AccessGrant oldValue)
            throws Exception {
          if (isGrantExpired(oldValue, 2)) {
            // reload if expiring in 2 mins
            // TODO should it be made configurable?
            return super.reload(key, oldValue);
          } else {
            return Futures.immediateFuture(oldValue);
          }
        }

        @Override
        public AccessGrant load(OidcTokenId key) throws Exception {
          return oauth2TokenService.refreshAccessToken(key, OAuth2TokenService.REQUIRED_SCOPES);
        }

      });

  /**
   * Check if a grant is expired.
   * 
   * @param grant
   *          the grant to check
   * @param skew
   *          the skew for the expiration evaluation
   * @return true if expired, false otherwise
   */
  public static boolean isGrantExpired(AccessGrant grant, long skew) {
    Preconditions.checkNotNull(grant);
    Preconditions.checkArgument(skew >= 0, "skew must be >= 0");

    return Optional.ofNullable(grant.getExpireTime())
        .map(expireTime -> Instant.now()
            .isAfter(Instant.ofEpochMilli(expireTime)
                // if expiring in ${skew} mins -> return true
                .minus(Duration.ofMinutes(skew))))
        // no expireTime -> return false
        .orElse(false);
  }

  public static boolean isGrantExpired(AccessGrant grant) {
    return isGrantExpired(grant, 0);
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

  protected AccessGrant get(OidcTokenId id, Callable<AccessGrant> grantLoader) {
    // it should be only used to put the exchanged token,
    // the refresh should be handled by the CacheLoader
    // Don't make it public
    Preconditions.checkNotNull(id);
    Preconditions.checkNotNull(grantLoader);
    Callable<? extends AccessGrant> loggingGrantLoader = () -> {
      LOG.debug("Loading and putting new access token for {} into cache", id);
      return grantLoader.call();
    };

    try {
      return oauth2TokensCache.get(id, loggingGrantLoader);
    } catch (ExecutionException ex) {
      throw new OrchestratorException(ex.getCause());
    }
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
    try {
      return oauth2TokensCache.get(id);
    } catch (ExecutionException ex) {
      throw new OrchestratorException(ex.getCause());
    }
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
