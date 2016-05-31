package it.reply.orchestrator.dto.security;

import org.mitre.openid.connect.model.UserInfo;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

public class IndigoOAuth2Authentication extends OAuth2Authentication {

  private static final long serialVersionUID = -1868480964470168415L;

  OAuth2AccessToken token;
  UserInfo userInfo;

  /**
   * Generate an {@link IndigoOAuth2Authentication}.
   * 
   * @param authentication
   *          the {@link OAuth2Authentication}.
   * @param token
   *          the {@link OAuth2AccessToken}.
   * @param userInfo
   *          the {@link UserInfo}. Can be null (i.e. the client is authenticating with its own
   *          credentials).
   */
  public IndigoOAuth2Authentication(OAuth2Authentication authentication, OAuth2AccessToken token,
      UserInfo userInfo) {
    super(authentication.getOAuth2Request(), authentication.getUserAuthentication());
    this.token = token;
    this.userInfo = userInfo;
  }

}
