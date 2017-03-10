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

import com.nimbusds.jwt.JWTParser;

import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.config.properties.OidcProperties.OidcClientProperties;
import it.reply.orchestrator.dto.security.IndigoOAuth2Authentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Optional;

@Service
public class OAuth2TokenService {

  @Autowired
  private OidcProperties oidcProperties;

  /**
   * Get the current OAuth2 token.
   * 
   * @return the OAuth2 token.
   * @throws IllegalStateException
   *           if the security is disabled, the user is not authenticated or the call is made of an
   *           HTTP session.
   */
  public String getOAuth2Token() {
    if (!oidcProperties.isEnabled()) {
      throw new IllegalStateException("Security is not enabled");
    }
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth instanceof IndigoOAuth2Authentication)) {
      throw new IllegalStateException("User is not authenticated");
    }
    IndigoOAuth2Authentication indigoAuth = (IndigoOAuth2Authentication) auth;
    return indigoAuth.getToken().getValue();
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
    if (!oidcProperties.isEnabled()) {
      throw new IllegalStateException("Security is not enabled");
    }
    String iss = JWTParser.parse(accessToken).getJWTClaimsSet().getIssuer();
    return Optional.ofNullable(oidcProperties.getIamConfiguration(iss))
        .map(configuration -> configuration.getClues());
  }

}
