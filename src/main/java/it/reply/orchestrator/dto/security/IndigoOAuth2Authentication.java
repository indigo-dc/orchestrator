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

package it.reply.orchestrator.dto.security;

import com.google.common.base.Preconditions;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.mitre.openid.connect.model.UserInfo;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class IndigoOAuth2Authentication extends OAuth2Authentication {

  private static final long serialVersionUID = -1868480964470168415L;

  @NonNull
  @NotNull
  private OAuth2AccessToken token;

  @NonNull
  @NotNull
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
    this.token = Preconditions.checkNotNull(token);
    this.userInfo = Preconditions.checkNotNull(userInfo);
  }

}
