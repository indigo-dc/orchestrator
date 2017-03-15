package it.reply.orchestrator.service.security;

import com.google.common.collect.ImmutableList;

import com.nimbusds.jwt.JWTClaimsSet;

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

import com.nimbusds.jwt.JWTParser;

import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.config.properties.OidcProperties.IamProperties;
import it.reply.orchestrator.config.properties.OidcProperties.OidcClientProperties;
import it.reply.orchestrator.dal.entity.OidcEntity;
import it.reply.orchestrator.dal.entity.OidcEntityId;
import it.reply.orchestrator.dal.entity.OidcRefreshToken;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dal.repository.OidcTokenRepository;
import it.reply.orchestrator.dto.security.IndigoOAuth2Authentication;
import it.reply.orchestrator.dto.security.IndigoUserInfo;
import it.reply.orchestrator.exception.OrchestratorException;

import org.mitre.oauth2.model.RegisteredClient;
import org.mitre.openid.connect.config.ServerConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.social.oauth2.AccessGrant;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class OAuth2TokenService {

  public static final List<String> REQUIRED_SCOPES =
      ImmutableList.of("openid", "profile", "offline_access");

  @Autowired
  private OidcProperties oidcProperties;

  @Autowired
  private OAuth2ConfigurationsService oauth2cConfigurationsService;

  @Autowired
  private OidcTokenRepository oidcTokenRepository;

  @Autowired
  private OAuth2TokenCacheService oauth2TokenCacheService;

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
    return Optional.of(SecurityContextHolder.getContext())
        .map(SecurityContext::getAuthentication)
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

  /**
   * Retrieve the CLUES IAM information from the OAuth2 access token.
   * 
   * @param accessToken
   *          the accessToken
   * @return the CLUES IAM information
   * @throws ParseException
   *           if the access token is not a valid JWT
   */
  public Optional<OidcClientProperties> getCluesInfo(String accessToken) throws ParseException {
    handleSecurityDisabled();
    String iss = JWTParser.parse(accessToken).getJWTClaimsSet().getIssuer();
    return Optional.ofNullable(oidcProperties.getIamConfiguration(iss))
        .map(IamProperties::getClues);
  }

  /**
   * Generate a OidcTokenId from the current authentication.
   * 
   * @return the OidcTokenId
   */
  public OidcTokenId generateTokenIdFromCurrentAuth() {
    handleSecurityDisabled();
    try {
      JWTClaimsSet claims = JWTParser.parse(getOAuth2TokenFromCurrentAuth()).getJWTClaimsSet();
      OidcTokenId tokenId = new OidcTokenId();
      tokenId.setIssuer(claims.getIssuer());
      tokenId.setJti(claims.getJWTID());
      return tokenId;
    } catch (ParseException e) {
      throw new OrchestratorException("Access token in current authentication is not a valid JWT");
    }
  }

  /**
   * Generate a OidcEntityId from an access token.
   * 
   * @return the OidcEntityId
   */
  public static OidcEntityId generateOidcEntityIdFromToken(String accessToken)
      throws ParseException {
    JWTClaimsSet claims = JWTParser.parse(accessToken).getJWTClaimsSet();
    OidcEntityId id = new OidcEntityId();
    id.setIssuer(claims.getIssuer());
    id.setSubject(claims.getSubject());
    return id;
  }

  /**
   * Generate a OidcEntityId from he current authentication.
   * 
   * @return the OidcEntityId
   */
  public OidcEntityId generateOidcEntityIdFromCurrentAuth() {
    handleSecurityDisabled();
    try {
      return generateOidcEntityIdFromToken(getOAuth2TokenFromCurrentAuth());
    } catch (ParseException e) {
      throw new OrchestratorException("Access token in current authentication is not a valid JWT");
    }
  }

  /**
   * Generate a OidcEntity from he current authentication.
   * 
   * @return the OidcEntity
   */
  public OidcEntity generateOidcEntityFromCurrentAuth() {
    handleSecurityDisabled();
    OidcEntity newEntity = new OidcEntity();

    OidcEntityId id = generateOidcEntityIdFromCurrentAuth();
    newEntity.setOidcEntityId(id);

    IndigoOAuth2Authentication autentication = getCurrentAuthentication();
    IndigoUserInfo userInfo = (IndigoUserInfo) autentication.getUserInfo();
    String organization = userInfo.getOrganizationName();
    newEntity.setOrganization(organization);
    return newEntity;
  }

  /**
   * Exchange an access token and put it in the cache.
   * 
   * @param id
   *          the id of the token
   * @param accessToken
   *          the acces token
   * @param scopes
   *          the scopes to request
   * @return the exchanged grant
   */
  public AccessGrant exchangeAccessToken(OidcTokenId id, String accessToken, List<String> scopes) {
    handleSecurityDisabled();
    CustomOAuth2Template template = generateOAuth2Template(id.getIssuer());
    // only exchanged if not already present
    // concurrency is handled by the cache
    return oauth2TokenCacheService.get(id, () -> template.exchangeToken(accessToken, scopes));
  }

  /**
   * Refresh an access token and put it in the cache.
   * 
   * @param id
   *          the id of the token
   * @param scopes
   *          the scopes to request
   * @return the exchanged grant
   */
  public AccessGrant refreshAccessToken(OidcTokenId id, List<String> scopes) {
    handleSecurityDisabled();
    CustomOAuth2Template template = generateOAuth2Template(id.getIssuer());
    String refreshToken =
        oidcTokenRepository.findByOidcTokenId(id).map(OidcRefreshToken::getVaule).orElseThrow(
            () -> new OrchestratorException("No refresh token suitable found"));
    return template.refreshToken(refreshToken, scopes);
  }

  public String getAccessToken(OidcTokenId id, List<String> scopes) {
    handleSecurityDisabled();
    return oauth2TokenCacheService.get(id).getAccessToken();
  }

  private CustomOAuth2Template generateOAuth2Template(String issuer) {
    handleSecurityDisabled();

    ServerConfiguration serverConfiguration =
        oauth2cConfigurationsService.getServerConfiguration(issuer);

    RegisteredClient clientConfiguration =
        oauth2cConfigurationsService.getClientConfiguration(serverConfiguration);

    boolean headerAuthSupported =
        Optional.ofNullable(serverConfiguration.getTokenEndpointAuthMethodsSupported())
            .orElse(Collections.emptyList())
            .stream()
            .anyMatch("client_secret_basic"::equals);

    CustomOAuth2Template template = new CustomOAuth2Template(clientConfiguration.getClientId(),
        clientConfiguration.getClientSecret(), serverConfiguration.getAuthorizationEndpointUri(),
        serverConfiguration.getTokenEndpointUri());

    // use post sectret only if header auth is not supported
    template.setUseParametersForClientAuthentication(!headerAuthSupported);

    return template;
  }
}
