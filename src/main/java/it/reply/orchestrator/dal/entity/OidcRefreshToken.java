/*
 * Copyright © 2015-2018 Santer Reply S.p.A.
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

package it.reply.orchestrator.dal.entity;

import it.reply.orchestrator.dal.util.SetStringToJsonConverter;
import it.reply.orchestrator.dto.security.AccessGrant;
import it.reply.orchestrator.utils.JwtUtils;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Entity
@Getter
@Setter
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"issuer", "subject", "clients_id"})})
@NoArgsConstructor
@SuppressWarnings("null")
public class OidcRefreshToken extends UuidIdentifiable {

  /**
   * Generate a OidcRefreshToken from a token id and a grant.
   * 
   * @param grant
   *          the grant
   * @param id
   *          the token identifier
   * @return the new OidcRefreshToken
   */
  public static OidcRefreshToken createFromAccessGrant(AccessGrant grant, OidcTokenId id) {
    OidcRefreshToken token = new OidcRefreshToken();
    token.setValue(grant.getRefreshToken());
    token.setCreationDate(new Date());
    token.setScopes(grant.getScopes());
    token.setOidcTokenId(id);

    JwtUtils
        .getExpirationTime(JwtUtils.parseJwt(grant.getRefreshToken()))
        .ifPresent(expirationDate -> token.setExpirationDate(expirationDate));

    return token;
  }

  /**
   * Updates the refresh token using the information contained in the access grant.
   * 
   * @param grant
   *          the access grant
   */
  public void updateFromAccessGrant(AccessGrant grant) {
    setValue(grant.getRefreshToken());
    setCreationDate(new Date());
    JwtUtils
        .getExpirationTime(JwtUtils.parseJwt(grant.getRefreshToken()))
        .ifPresent(newExpirationDate -> setExpirationDate(newExpirationDate));
  }

  @Column(name = "refresh_token_value", nullable = false)
  @NotNull
  @NonNull
  private String value;

  @Column(name = "expires_at")
  @Nullable
  @Temporal(TemporalType.TIMESTAMP)
  private Date expirationDate;

  @Column(name = "issued_at", nullable = false)
  @NotNull
  @NonNull
  @Temporal(TemporalType.TIMESTAMP)
  private Date creationDate;

  @Column(name = "scopes", nullable = false, updatable = false)
  @Convert(converter = SetStringToJsonConverter.class)
  @NotNull
  @NonNull
  private Set<String> scopes = new HashSet<>();

  @NotNull
  @NonNull
  @Embedded
  private OidcTokenId oidcTokenId;

}
