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

package it.reply.orchestrator.tosca.cache;

import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingContextExecution;
import it.reply.orchestrator.config.properties.ToscaProperties;
import it.reply.orchestrator.service.cache.EntryProcessorFactory;
import java.util.concurrent.TimeUnit;
import javax.cache.Cache;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.normative.ToscaNormativeImports;
import org.apache.ignite.Ignite;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.eviction.lru.LruEvictionPolicy;
import org.apache.ignite.configuration.CacheConfiguration;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TemplateCacheServiceImpl implements TemplateCacheService {

  public static final String CACHE_NAME = "tosca_templates";

  private Cache<CSARDependency, ArchiveRoot> templateCache;

  private EntryProcessorFactory entryProcessorFactory;

  private ToscaProperties toscaProperties;

  /**
   * Creates a new TemplateCacheService.
   *
   * @param ignite
   *          the {@link Ignite} instance.
   * @param entryProcessorFactory
   *          the {@link EntryProcessorFactory} instance.
   * @param toscaProperties
   *          the {@link ToscaProperties} instance.
   */
  public TemplateCacheServiceImpl(@NonNull Ignite ignite,
                                  EntryProcessorFactory entryProcessorFactory,
                                  ToscaProperties toscaProperties) {
    this.entryProcessorFactory = entryProcessorFactory;
    this.toscaProperties = toscaProperties;

    long durationInMillis = toscaProperties.getCacheDuration() * 1000;
    Duration cacheDuration = new Duration(TimeUnit.MILLISECONDS, durationInMillis);
    CacheConfiguration<CSARDependency, ArchiveRoot> oauth2CacheCfg =
        new CacheConfiguration<CSARDependency, ArchiveRoot>(CACHE_NAME)
            .setEvictionPolicy(new LruEvictionPolicy<>(1_000))
            .setAtomicityMode(CacheAtomicityMode.ATOMIC)
            .setOnheapCacheEnabled(true)
            .setCacheMode(CacheMode.PARTITIONED)
            .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(cacheDuration))
            .setCopyOnRead(true);

    templateCache = ignite.getOrCreateCache(oauth2CacheCfg);
  }

  @Override
  public ArchiveRoot get(CSARDependency id) {
    return templateCache.invoke(id, entryProcessorFactory.get(TemplateProcessor.class));
  }

}
