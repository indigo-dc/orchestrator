package it.reply.orchestrator.dto.security;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.mitre.openid.connect.model.UserInfo;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

public class IndigoOAuth2Authentication extends OAuth2Authentication {

  private static final long serialVersionUID = -1868480964470168415L;

  private OAuth2AccessToken token;
  private UserInfo userInfo;

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
    this.setToken(token);
    this.setUserInfo(userInfo);
  }

  public OAuth2AccessToken getToken() {
    return token;
  }

  public void setToken(OAuth2AccessToken token) {
    this.token = token;
  }

  public UserInfo getUserInfo() {
    return userInfo;
  }

  public void setUserInfo(UserInfo userInfo) {
    this.userInfo = userInfo;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().appendSuper(super.hashCode()).append(this.getToken())
        .append(this.getUserInfo()).build();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof IndigoOAuth2Authentication)) {
      return false;
    }
    IndigoOAuth2Authentication other = (IndigoOAuth2Authentication) obj;
    return new EqualsBuilder().appendSuper(super.equals(other))
        .append(this.getToken(), other.getToken()).append(this.getUserInfo(), other.getUserInfo())
        .build();
  }

}
