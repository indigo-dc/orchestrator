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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.config.properties.OidcProperties.IamProperties;
import it.reply.orchestrator.config.properties.OidcProperties.OidcClientProperties;
import it.reply.orchestrator.dal.entity.OidcEntity;
import it.reply.orchestrator.dal.entity.OidcEntityId;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dal.repository.OidcEntityRepository;
import it.reply.orchestrator.dto.security.IndigoOAuth2Authentication;
import it.reply.orchestrator.dto.security.IndigoUserInfo;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.function.ThrowingConsumer;
import it.reply.orchestrator.function.ThrowingFunction;
import it.reply.orchestrator.service.security.cache.OAuth2TokenCacheService;
import it.reply.orchestrator.utils.CommonUtils;
import it.reply.orchestrator.utils.JwtUtils;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import javax.servlet.http.HttpServletRequest;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Service
public class OAuth2TokenService {

  @Autowired
  private HttpServletRequest request;

  @Autowired
  private OidcProperties oidcProperties;

  @Autowired
  private OAuth2TokenCacheService oauth2TokenCacheService;

  @Autowired
  private OidcEntityRepository oidcEntityRepository;

  private void handleSecurityDisabled() {
    oidcProperties.throwIfSecurityDisabled();
  }

  /**
   * Get the current authentication.
   *
   * @return the current authentication
   */
  public IndigoOAuth2Authentication getCurrentAuthentication() {
    handleSecurityDisabled();
    return Optional
        .ofNullable(SecurityContextHolder.getContext().getAuthentication())
        .filter(IndigoOAuth2Authentication.class::isInstance)
        .map(IndigoOAuth2Authentication.class::cast)
        .orElseThrow(() -> new IllegalStateException("User is not authenticated"));
  }

  public static String getOAuth2TokenFromAutentication(IndigoOAuth2Authentication authentication) {
    return authentication.getToken().getValue();
  }

  /**
   * Get the current OAuth2 token.
   *
   * @return the OAuth2 token.
   * @throws IllegalStateException
   *           if the security is disabled, the user is not authenticated or the call is made of an
   *           HTTP session.
   */
  public String getOAuth2TokenFromCurrentAuth() {
    return getOAuth2TokenFromAutentication(getCurrentAuthentication());
  }

  public static List<String>
      getOAuth2ClientsFromAutentication(IndigoOAuth2Authentication authentication) {
    return Lists.newArrayList(authentication.getOAuth2Request().getClientId());
  }

  public List<String> getOAuth2ClientsFromCurrentAutentication() {
    return getOAuth2ClientsFromAutentication(getCurrentAuthentication());
  }

  /**
   * Retrieve the CLUES IAM information from the OAuth2 access token.
   *
   * @param accessToken
   *          the accessToken
   * @return the CLUES IAM information
   */
  public Optional<OidcClientProperties> getCluesInfo(String accessToken) {
    String iss = JwtUtils.getIssuer(JwtUtils.parseJwt(accessToken));
    return oidcProperties
        .getIamConfiguration(iss)
        .flatMap(IamProperties::getClues);
  }

  /**
   * Generate a OidcTokenId from the current authentication.
   *
   * @return the OidcTokenId
   */
  public OidcTokenId generateTokenIdFromCurrentAuth() {
    OidcEntityId entityId = generateOidcEntityIdFromCurrentAuth();
    List<String> clientsId = getOAuth2ClientsFromCurrentAutentication();
    OidcTokenId tokenId = new OidcTokenId();
    tokenId.setClientsId(clientsId);
    tokenId.setOidcEntityId(entityId);
    return tokenId;
  }

  /**
   * Generate a OidcEntityId from he current authentication.
   *
   * @return the OidcEntityId
   */
  public OidcEntityId generateOidcEntityIdFromCurrentAuth() {
    OidcEntityId entityId = OidcEntityId.fromAccesToken(getOAuth2TokenFromCurrentAuth());
    String tmpUserOverride = request.getHeader("User");
    if (tmpUserOverride != null) {
      entityId.setSubject(tmpUserOverride);
    }
    return entityId;
  }

  /**
   * Generate a OidcEntity from he current authentication.
   *
   * @return the OidcEntity
   */
  public OidcEntity generateOidcEntityFromCurrentAuth() {
    OidcEntity newEntity = new OidcEntity();

    OidcEntityId id = generateOidcEntityIdFromCurrentAuth();
    newEntity.setOidcEntityId(id);

    IndigoOAuth2Authentication autentication = getCurrentAuthentication();
    IndigoUserInfo userInfo = (IndigoUserInfo) autentication.getUserInfo();
    if (userInfo != null) {
      String organization = Preconditions.checkNotNull(userInfo.getOrganizationName(),
          "Organization name not found between the user info claims");
      newEntity.setOrganization(organization);
    } else {
      throw new OrchestratorException("Client credentials grant not supported");
    }
    return newEntity;
  }

  /**
   * Gets the current OidcEntity if already exists or generates a new one.
   *
   * @return the current OidcEntity
   */
  public OidcEntity getOrGenerateOidcEntityFromCurrentAuth() {
    return oidcEntityRepository
        .findByOidcEntityId(generateOidcEntityIdFromCurrentAuth())
        // TODO update organization relationship?
        .orElseGet(this::generateOidcEntityFromCurrentAuth);
  }

  /**
   * Gets the user's organization from the token ID.
   *
   * @param oidcTokenId
   *     the token ID
   * @return the user's organization
   */
  public String getOrganization(OidcTokenId oidcTokenId) {
    return Optional
        .ofNullable(oidcTokenId)
        .map(OidcTokenId::getOidcEntityId)
        .flatMap(oidcEntityRepository::findByOidcEntityId)
        .map(OidcEntity::getOrganization)
        .orElseGet(() -> {
          if (oidcProperties.isEnabled()) {
            throw new DeploymentException("No user associated to deployment token found");
          } else {
            return "indigo-dc";
          }
        });
  }

  /**
   * Exchange an access token and put it in the cache.
   *
   * @return the exchanged grant
   */
  public OidcTokenId exchangeCurrentAccessToken() {
    OidcTokenId id = generateTokenIdFromCurrentAuth();
    oauth2TokenCacheService.exchangeAccessToken(id, getOAuth2TokenFromCurrentAuth());
    return id;
  }

  /**
   * Refresh an access token and put it into the cache.
   *
   * @param id
   *          the id of the token
   * @return the exchanged grant
   */
  public String getRefreshedAccessToken(OidcTokenId id) {
    handleSecurityDisabled();
    return oauth2TokenCacheService.getNew(id).getAccessToken();
  }

  public String getAccessToken(OidcTokenId id) {
    handleSecurityDisabled();
    return oauth2TokenCacheService.get(id).getAccessToken();
  }

  /**
   * Execute a {@link ThrowingFunction}, handling the OAuth2 token.
   *
   * @param <R> the result object type
   * @param <E> the thrown exception type
   * @param oidcTokenId
   *     the token ID
   * @param function
   *     the {@link ThrowingFunction}
   * @param tokenRefreshEvaluator
   *     function evaluating whether a refresh token needs to be retrieved
   * @return the {@link ThrowingFunction} result
   * @throws E
   *     the exception thrown by the {@link ThrowingFunction}
   */
  public <R, E extends Exception> R executeWithClientForResult(
      @Nullable OidcTokenId oidcTokenId,
      ThrowingFunction<String, R, E> function,
      Predicate<Exception> tokenRefreshEvaluator) throws E {
    if (!oidcProperties.isEnabled()) {
      return function.apply(null);
    } else {
      String accessToken = getAccessToken(CommonUtils.checkNotNull(oidcTokenId));
      try {
        return function.apply(accessToken);
      } catch (Exception ex) {
        if (tokenRefreshEvaluator.test(ex)) {
          String refreshedAccessToken = getRefreshedAccessToken(oidcTokenId);
          return function.apply(refreshedAccessToken);
        } else {
          throw ex;
        }
      }
    }
  }

  /**
   * Execute a {@link ThrowingConsumer}, handling the OAuth2 token.
   *
   * @param <E> the thrown exception type
   * @param oidcTokenId
   *     the token ID
   * @param consumer
   *     the {@link ThrowingConsumer}
   * @param tokenRefreshEvaluator
   *     function evaluating whether a refresh token needs to be retrieved
   * @throws E
   *     the exception thrown by the {@link ThrowingConsumer}
   */
  public <E extends Exception> void executeWithClient(
      @Nullable OidcTokenId oidcTokenId,
      ThrowingConsumer<String, E> consumer,
      Predicate<Exception> tokenRefreshEvaluator) throws E {
    executeWithClientForResult(oidcTokenId,
        accessToken -> consumer.asFunction().apply(accessToken), tokenRefreshEvaluator);
  }

  public static final Predicate<Exception> restTemplateTokenRefreshEvaluator =
      ex -> ex instanceof HttpClientErrorException && ((HttpClientErrorException) ex)
          .getRawStatusCode() == 401;
}
