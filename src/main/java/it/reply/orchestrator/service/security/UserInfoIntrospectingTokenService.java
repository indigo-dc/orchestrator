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

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.SignedJWT;

import it.reply.orchestrator.dto.security.IndigoOAuth2Authentication;
import it.reply.orchestrator.utils.JwtUtils;

import lombok.extern.slf4j.Slf4j;

import org.mitre.jwt.signer.service.JWTSigningAndValidationService;
import org.mitre.jwt.signer.service.impl.JWKSetCacheService;
import org.mitre.oauth2.introspectingfilter.IntrospectingTokenService;
import org.mitre.openid.connect.client.UserInfoFetcher;
import org.mitre.openid.connect.client.service.ServerConfigurationService;
import org.mitre.openid.connect.config.ServerConfiguration;
import org.mitre.openid.connect.model.PendingOIDCAuthenticationToken;
import org.mitre.openid.connect.model.UserInfo;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import java.text.ParseException;
import java.util.Optional;

@Slf4j
public class UserInfoIntrospectingTokenService extends IntrospectingTokenService {

  private ServerConfigurationService serverConfigurationService;
  private UserInfoFetcher userInfoFetcher;
  private JWKSetCacheService validationServices;

  /**
   * Generate a new UserInfoIntrospectingTokenService.
   * 
   * @param serverConfigurationService
   *          the serverConfigurationService
   * @param userInfoFetcher
   *          the userInfoFetcher
   * @param validationServices
   *          the validationServices
   */
  public UserInfoIntrospectingTokenService(ServerConfigurationService serverConfigurationService,
      UserInfoFetcher userInfoFetcher, JWKSetCacheService validationServices) {
    this.serverConfigurationService = serverConfigurationService;
    this.userInfoFetcher = userInfoFetcher;
    this.validationServices = validationServices;
  }

  @Override
  public OAuth2Authentication loadAuthentication(String accessToken)
      throws AuthenticationException {
    IndigoOAuth2Authentication auth = null;
    SignedJWT jwtToken = null;
    try {
      jwtToken = SignedJWT.parse(accessToken);
    } catch (Exception ex) {
      LOG.info("Invalid access token, access token <{}> is not a signed JWT", accessToken, ex);
      return null;
    }
    try {
      // check if expired or not signed
      preValidate(jwtToken);
      OAuth2Authentication authentication = super.loadAuthentication(accessToken);
      OAuth2AccessToken token = super.readAccessToken(accessToken);
      if (authentication != null) {
        UserInfo userInfo = null;
        if (!authentication.isClientOnly() && token.getScope().contains("openid")) {
          userInfo = getUserInfo(authentication, jwtToken);
        }
        auth = new IndigoOAuth2Authentication(authentication, token, userInfo);
      }
    } catch (InvalidTokenException ex) {
      LOG.info("Invalid access token, {}", ex.getMessage());
      return null;
    } catch (Exception ex) {
      // if there is an exception return a null authentication
      // (this will translate to an "invalid_token" response)
      LOG.info("Error validating access token", ex);
      return null;
    }
    return auth;
  }

  private void preValidate(SignedJWT jwtToken) throws ParseException {
    if (JwtUtils.isJtwTokenExpired(jwtToken)) {
      throw new InvalidTokenException("access token is expired");
    }

    String issuer = getIssuer(jwtToken);
    ServerConfiguration serverConfiguration = getServerConfiguration(issuer);
    JWTSigningAndValidationService validationService = Optional
        .ofNullable(validationServices.getValidator(serverConfiguration.getJwksUri())).orElseThrow(
            () -> new AuthorizationServiceException("Couldn't retrive validator for issuer "
                + issuer + ", validation of access token skipped"));

    if (!validationService.validateSignature((SignedJWT) jwtToken)) {
      throw new InvalidTokenException("access token has an invalid signature");
    }
  }

  private UserInfo getUserInfo(OAuth2Authentication authentication, JWT jwtToken)
      throws ParseException {
    String accessToken = jwtToken.getParsedString();
    String issuer = getIssuer(jwtToken);
    ServerConfiguration serverConfiguration = getServerConfiguration(issuer);
    PendingOIDCAuthenticationToken infoKey =
        new PendingOIDCAuthenticationToken(authentication.getPrincipal().toString(), issuer,
            serverConfiguration, null, accessToken, null);

    return Optional.ofNullable(userInfoFetcher.loadUserInfo(infoKey))
        .orElseThrow(() -> new AuthorizationServiceException("Error retrieving user info"));
  }

  private String getIssuer(JWT jwtToken) {
    return Optional.ofNullable(JwtUtils.getJwtClaimsSet(jwtToken).getIssuer())
        .orElseThrow(() -> new IllegalArgumentException("No issuer claim found in JWT"));
  }

  /**
   * Get a server configuration from a issuer.
   * 
   * @param issuer
   *          the issuer
   * @return the server configuration
   */
  public ServerConfiguration getServerConfiguration(String issuer) {
    return Optional.ofNullable(serverConfigurationService.getServerConfiguration(issuer))
        .orElseThrow(() -> new IllegalArgumentException(
            "Could not find server configuration for issuer " + issuer));

  }
}
