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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.mitre.jwt.signer.service.JWTSigningAndValidationService;
import org.mitre.jwt.signer.service.impl.JWKSetCacheService;
import org.mitre.oauth2.introspectingfilter.IntrospectingTokenService;
import org.mitre.openid.connect.client.UserInfoFetcher;
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
@AllArgsConstructor
public class UserInfoIntrospectingTokenService extends IntrospectingTokenService {

  private OAuth2ConfigurationsService oauth2ConfigurationsService;
  private UserInfoFetcher userInfoFetcher;
  private JWKSetCacheService validationServices;

  @Override
  public OAuth2Authentication loadAuthentication(String accessToken)
      throws AuthenticationException {
    try {
      // check if a JWT and signed
      SignedJWT jwtToken = SignedJWT.parse(accessToken);
      // check if not expired and with valid signature
      preValidate(jwtToken);
      OAuth2Authentication authentication = super.loadAuthentication(accessToken);
      OAuth2AccessToken token = super.readAccessToken(accessToken);
      UserInfo userInfo = token.getScope().contains("openid") ? getUserInfo(jwtToken) : null;
      return new IndigoOAuth2Authentication(authentication, token, userInfo);
    } catch (ParseException ex) {
      LOG.info("Invalid access token, access token <{}> is not a signed JWT", accessToken, ex);
      return null;
    } catch (InvalidTokenException ex) {
      LOG.info("Invalid access token, {}", ex.getMessage());
      return null;
    } catch (RuntimeException ex) {
      // if there is an exception return a null authentication
      // (this will translate to an "invalid_token" response)
      LOG.info("Error validating access token", ex);
      return null;
    }
  }

  private void preValidate(SignedJWT jwtToken) {
    if (JwtUtils.isJtwTokenExpired(jwtToken)) {
      throw new InvalidTokenException("access token is expired");
    }

    String issuer = JwtUtils.getIssuer(jwtToken);
    ServerConfiguration serverConfiguration = oauth2ConfigurationsService
        .getServerConfiguration(issuer);
    JWTSigningAndValidationService validationService = Optional
        .ofNullable(validationServices.getValidator(serverConfiguration.getJwksUri()))
        .orElseThrow(() -> new AuthorizationServiceException(String
            .format("Couldn't retrive validator for issuer %s", issuer)));

    if (!validationService.validateSignature(jwtToken)) {
      throw new InvalidTokenException("access token has an invalid signature");
    }
  }

  private UserInfo getUserInfo(JWT jwtToken) {
    String subject = JwtUtils.getSubject(jwtToken);
    String issuer = JwtUtils.getIssuer(jwtToken);
    ServerConfiguration serverConfiguration = oauth2ConfigurationsService
        .getServerConfiguration(issuer);
    PendingOIDCAuthenticationToken infoKey =
        new PendingOIDCAuthenticationToken(subject, issuer,
            serverConfiguration, null, jwtToken.getParsedString(), null);

    return Optional
        .ofNullable(userInfoFetcher.loadUserInfo(infoKey))
        .orElseThrow(() -> new AuthorizationServiceException("Error retrieving user info"));
  }

}
