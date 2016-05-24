package it.reply.orchestrator.service.security;

import com.google.common.base.Strings;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

import it.reply.orchestrator.dto.security.IndigoOAuth2Authentication;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mitre.oauth2.introspectingfilter.IntrospectingTokenService;
import org.mitre.openid.connect.client.UserInfoFetcher;
import org.mitre.openid.connect.client.service.ServerConfigurationService;
import org.mitre.openid.connect.config.ServerConfiguration;
import org.mitre.openid.connect.model.PendingOIDCAuthenticationToken;
import org.mitre.openid.connect.model.UserInfo;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import java.text.ParseException;

public class UserInfoIntrospectingTokenService extends IntrospectingTokenService {

  static final Logger LOG = LogManager.getLogger(UserInfoIntrospectingTokenService.class);

  private ServerConfigurationService serverConfigurationService;
  private UserInfoFetcher userInfoFetcher;

  @Override
  public OAuth2Authentication loadAuthentication(String accessToken)
      throws AuthenticationException {
    IndigoOAuth2Authentication auth = null;
    try {
      OAuth2Authentication authentication = super.loadAuthentication(accessToken);
      OAuth2AccessToken token = super.readAccessToken(accessToken);
      if (authentication != null) {
        UserInfo userInfo = null;
        if (!authentication.isClientOnly()) {
          userInfo = getUserInfo(authentication, token);
        }
        auth = new IndigoOAuth2Authentication(authentication, token, userInfo);
      }
    } catch (AuthenticationException ex) {
      // if there is an exception return a null authentication
      // (this will translate to an "invalid_token" response)
      LOG.info(ex);
    }
    return auth;
  }

  /**
   * Get the serverConfigurationService used.
   * 
   * @return the serverConfigurationService
   */
  public ServerConfigurationService getServerConfigurationService() {
    return serverConfigurationService;
  }

  /**
   * Set the serverConfigurationService to use.
   * 
   * @param serverConfigurationService
   *          the serverConfigurationService to set
   */
  public void setServerConfigurationService(ServerConfigurationService serverConfigurationService) {
    this.serverConfigurationService = serverConfigurationService;
  }

  public UserInfoFetcher getUserInfoFetcher() {
    return userInfoFetcher;
  }

  public void setUserInfoFetcher(UserInfoFetcher userInfoFetcher) {
    this.userInfoFetcher = userInfoFetcher;
  }

  private UserInfo getUserInfo(OAuth2Authentication authentication, OAuth2AccessToken token) {
    String accessToken = token.getValue();
    String issuer = getIssuer(accessToken);
    ServerConfiguration serverConfiguration = getServerConfiguration(issuer);
    PendingOIDCAuthenticationToken infoKey =
        new PendingOIDCAuthenticationToken(authentication.getPrincipal().toString(), issuer,
            serverConfiguration, null, accessToken, null);
    UserInfo userInfo = userInfoFetcher.loadUserInfo(infoKey);
    return userInfo;
  }

  private String getIssuer(String accessToken) {
    try {
      JWT jwt = JWTParser.parse(accessToken);

      String issuer = jwt.getJWTClaimsSet().getIssuer();

      if (!Strings.isNullOrEmpty(issuer)) {
        return issuer;
      } else {
        throw new IllegalArgumentException("No issuer claim found in JWT");
      }
    } catch (ParseException ex) {
      throw new IllegalArgumentException("Unable to parse JWT", ex);
    }
  }

  private ServerConfiguration getServerConfiguration(String issuer) {
    ServerConfiguration server = serverConfigurationService.getServerConfiguration(issuer);
    if (server != null) {
      return server;
    } else {
      throw new IllegalArgumentException(
          "Could not find server configuration for issuer " + issuer);
    }
  }
}
