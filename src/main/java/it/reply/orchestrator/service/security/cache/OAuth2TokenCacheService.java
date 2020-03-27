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

package it.reply.orchestrator.service.security.cache;

import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dto.security.AccessGrant;
import it.reply.orchestrator.service.cache.EntryProcessorFactory;
import javax.cache.Cache;
import javax.cache.Cache.Entry;
import lombok.extern.slf4j.Slf4j;
import org.apache.ignite.Ignite;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.eviction.EvictionFilter;
import org.apache.ignite.cache.eviction.lru.LruEvictionPolicy;
import org.apache.ignite.configuration.CacheConfiguration;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OAuth2TokenCacheService {

  public static final String CACHE_NAME = "oauth2_tokens";

  private Cache<OidcTokenId, AccessGrant> oauth2TokensCache;

  private EntryProcessorFactory entryProcessorFactory;

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
  public OAuth2TokenCacheService(@NonNull Ignite ignite,
                                 EntryProcessorFactory entryProcessorFactory) {

    this.entryProcessorFactory = entryProcessorFactory;

    CacheConfiguration<OidcTokenId, AccessGrant> oauth2CacheCfg =
        new CacheConfiguration<OidcTokenId, AccessGrant>(CACHE_NAME)
            .setEvictionFilter(new TokenEvictionFilter())
            .setEvictionPolicy(new LruEvictionPolicy<>(1_000))
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
    return oauth2TokensCache
        .invoke(id, entryProcessorFactory.get(ExchangeTokenProcessor.class), accessToken);
  }

  public AccessGrant get(OidcTokenId id) {
    return oauth2TokensCache.invoke(id, entryProcessorFactory.get(GetTokenProcessor.class));
  }

  public AccessGrant getNew(OidcTokenId id) {
    return oauth2TokensCache.invoke(id, entryProcessorFactory.get(GetNewTokenProcessor.class));
  }

}
