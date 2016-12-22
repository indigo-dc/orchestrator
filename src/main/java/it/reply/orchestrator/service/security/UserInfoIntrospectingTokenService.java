package it.reply.orchestrator.service.security;

import com.google.common.base.Strings;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;

import it.reply.orchestrator.dto.security.IndigoOAuth2Authentication;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import java.text.ParseException;
import java.util.Date;

public class UserInfoIntrospectingTokenService extends IntrospectingTokenService {

  static final Logger LOG = LogManager.getLogger(UserInfoIntrospectingTokenService.class);

  private ServerConfigurationService serverConfigurationService;
  private UserInfoFetcher userInfoFetcher;
  private JWKSetCacheService validationServices;

  @Override
  public OAuth2Authentication loadAuthentication(String accessToken)
      throws AuthenticationException {
    IndigoOAuth2Authentication auth = null;
    try {
      JWT jwtToken = JWTParser.parse(accessToken);
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
    } catch (OAuth2Exception ex) {
      LOG.info("Error validating access token, {}", ex.getMessage());
      return null;
    } catch (Exception ex) {
      // if there is an exception return a null authentication
      // (this will translate to an "invalid_token" response)
      LOG.info("Error validating access token", ex);
      return null;
    }
    return auth;
  }

  private void preValidate(JWT jwtToken) throws ParseException {
    Date expirationTime = jwtToken.getJWTClaimsSet().getExpirationTime();
    if (expirationTime != null) {
      if (expirationTime.before(new Date())) {
        throw new InvalidTokenException("access token is expired");
      }
    }
    if (jwtToken instanceof SignedJWT) {
      String issuer = getIssuer(jwtToken);
      ServerConfiguration serverConfiguration = getServerConfiguration(issuer);
      JWTSigningAndValidationService validationService =
          validationServices.getValidator(serverConfiguration.getJwksUri());
      if (validationService != null) {
        if (!validationService.validateSignature((SignedJWT) jwtToken)) {
          throw new InvalidTokenException("access token has an invalid signature");
        }
      } else {
        LOG.warn("Couldn't retrive validator for issuer {}, validation of access token skipped",
            issuer);
      }
    }
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

  public JWKSetCacheService getValidationServices() {
    return validationServices;
  }

  public void setValidationServices(JWKSetCacheService validationServices) {
    this.validationServices = validationServices;
  }

  private UserInfo getUserInfo(OAuth2Authentication authentication, JWT jwtToken)
      throws ParseException {
    String accessToken = jwtToken.getParsedString();
    String issuer = getIssuer(jwtToken);
    ServerConfiguration serverConfiguration = getServerConfiguration(issuer);
    PendingOIDCAuthenticationToken infoKey =
        new PendingOIDCAuthenticationToken(authentication.getPrincipal().toString(), issuer,
            serverConfiguration, null, accessToken, null);
    UserInfo userInfo = userInfoFetcher.loadUserInfo(infoKey);
    if (userInfo == null) {
      throw new AuthorizationServiceException("Error retrieving user info");
    }
    return userInfo;
  }

  private String getIssuer(JWT jwt) throws ParseException {
    String issuer = jwt.getJWTClaimsSet().getIssuer();
    if (!Strings.isNullOrEmpty(issuer)) {
      return issuer;
    } else {
      throw new IllegalArgumentException("No issuer claim found in JWT");
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
