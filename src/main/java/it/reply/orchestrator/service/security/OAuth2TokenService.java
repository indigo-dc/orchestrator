package it.reply.orchestrator.service.security;

import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.dto.security.IndigoOAuth2Authentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

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
}
