package it.reply.orchestrator.dto.security;

import org.mitre.openid.connect.model.UserInfo;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

public class IndigoOAuth2Authentication extends OAuth2Authentication {

  private static final long serialVersionUID = -1868480964470168415L;

  OAuth2AccessToken token;
  UserInfo userInfo;

  public IndigoOAuth2Authentication(OAuth2Authentication authentication, OAuth2AccessToken token,
      UserInfo userInfo) {
    super(authentication.getOAuth2Request(), authentication.getUserAuthentication());
    this.token = token;
    this.userInfo = userInfo;
  }

}
