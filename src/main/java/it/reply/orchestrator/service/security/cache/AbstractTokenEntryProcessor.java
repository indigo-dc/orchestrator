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

import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.dal.entity.OidcRefreshToken;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dal.repository.OidcTokenRepository;
import it.reply.orchestrator.dto.security.AccessGrant;
import it.reply.orchestrator.dto.security.TokenIntrospectionResponse;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.service.security.CustomOAuth2Template;
import it.reply.orchestrator.service.security.CustomOAuth2TemplateFactory;
import it.reply.orchestrator.utils.JwtUtils;
import java.time.Duration;
import java.util.Optional;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StopWatch;

@Slf4j
public abstract class AbstractTokenEntryProcessor
    implements EntryProcessor<OidcTokenId, AccessGrant, AccessGrant> {

  @Autowired
  private OidcTokenRepository oidcTokenRepository;

  @Autowired
  private CustomOAuth2TemplateFactory customOAuth2TemplateFactory;

  @Autowired
  private OidcProperties oidcProperties;

  protected AccessGrant refreshIfExpired(@NonNull MutableEntry<OidcTokenId, AccessGrant> entry) {
    OidcTokenId id = entry.getKey();
    LOG.info("Retrieving access token for {} from cache", id);
    AccessGrant oldGrant = entry.getValue();
    Duration expirationDuration = Duration.ofMinutes(5);
    if (oldGrant == null) {
      LOG.info("No access token for {} available. Refreshing and populating cache", id);
      return refresh(entry);
    } else if (oldGrant.isExpiringIn(expirationDuration)) {
      LOG.info(
          "Refreshing access token for {} because the one in cache is expiring in less than {}",
          id, expirationDuration);
      return refresh(entry);
    } else {
      LOG.info("Access token for {} retrieved from cache", id);
      return oldGrant;
    }
  }

  protected AccessGrant refresh(@NonNull MutableEntry<OidcTokenId, AccessGrant> entry) {
    OidcTokenId id = entry.getKey();
    CustomOAuth2Template template = customOAuth2TemplateFactory.generateOAuth2Template(id);
    OidcRefreshToken refreshToken =
        oidcTokenRepository
            .findByOidcTokenId(id)
            .orElseThrow(() -> new OrchestratorException("No refresh token found for " + id));
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    AccessGrant grant =
        template.refreshToken(refreshToken.getValue(), OidcProperties.REQUIRED_SCOPES);
    stopWatch.stop();
    LOG.info("Access token for {} refreshed in {}ms", id, stopWatch.getTotalTimeMillis());
    String newRefreshToken = grant.getRefreshToken();
    if (newRefreshToken != null && !newRefreshToken.equals(refreshToken.getValue())) {
      LOG.info("New refresh token received for {}", id);
      refreshToken.updateFromAccessGrant(grant);
    }
    entry.setValue(grant);
    return grant;
  }

  protected AccessGrant exchange(@NonNull MutableEntry<OidcTokenId, AccessGrant> entry,
      String accessToken) {
    OidcTokenId id = entry.getKey();
    CustomOAuth2Template template = customOAuth2TemplateFactory.generateOAuth2Template(id);
    Optional<OidcRefreshToken> refreshToken = oidcTokenRepository.findByOidcTokenId(id);
    if (refreshToken.isPresent()) {
      if (oidcProperties.isForceRefreshTokensValidation()) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        boolean isActive = refreshToken
            .map(token -> template.introspectToken(token.getValue()))
            .filter(TokenIntrospectionResponse::isActive)
            .isPresent();
        stopWatch.stop();
        LOG.info("Refresh token for {} validated in {}ms", id, stopWatch.getTotalTimeMillis());
        if (!isActive) {
          LOG.info(
              "Refresh token for {} isn't active anymore."
                  + " Getting a new one exchanging access token with jti={}",
              id, JwtUtils.getJti(JwtUtils.parseJwt(accessToken)));
          AccessGrant grant = template.exchangeToken(accessToken, OidcProperties.REQUIRED_SCOPES);
          refreshToken.get().updateFromAccessGrant(grant);
          entry.setValue(grant);
        } else {
          LOG.info(
              "A valid refresh token for {} is already available. Not exchanging the current one",
              id);
        }
      } else {
        LOG.info("Skipping validation of refresh token for {}", id);
      }
    } else {
      LOG.info("No refresh token found for {}. Exchanging access token with jti={}",
          id, JwtUtils.getJti(JwtUtils.parseJwt(accessToken)));
      AccessGrant grant = template.exchangeToken(accessToken, OidcProperties.REQUIRED_SCOPES);
      OidcRefreshToken token = OidcRefreshToken.createFromAccessGrant(grant, entry.getKey());
      oidcTokenRepository.save(token);
      entry.setValue(grant);
    }
    return refreshIfExpired(entry);
  }

  @Override
  public abstract AccessGrant process(@NonNull MutableEntry<OidcTokenId, AccessGrant> entry,
      Object... arguments) throws EntryProcessorException;

}
