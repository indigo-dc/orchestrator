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

import lombok.Getter;
import lombok.Setter;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.hateoas.Identifiable;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

@Entity
@Getter
@Setter
@Table(name = "oidc_refresh_token",
    uniqueConstraints = { @UniqueConstraint(name = "uq_issuer_subject_clients_id",
        columnNames = { "issuer", "subject", "clients_id" }) })
@SuppressWarnings("null")
public class OidcRefreshToken implements Identifiable<Long> {

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
    token.setVaule(grant.getRefreshToken());
    token.setCreationDate(new Date());
    token.setScopes(grant.getScope());
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
    setVaule(grant.getRefreshToken());
    setCreationDate(new Date());
    JwtUtils
        .getExpirationTime(JwtUtils.parseJwt(grant.getRefreshToken()))
        .ifPresent(newExpirationDate -> setExpirationDate(newExpirationDate));
  }

  @Id
  @GeneratedValue(
      strategy = GenerationType.AUTO,
      generator = "native")
  @GenericGenerator(
      name = "native",
      strategy = "native")
  @Column(name = "id", unique = true, nullable = false, updatable = false)
  private Long id;

  @Column(name = "refresh_token_value", nullable = false, updatable = true)
  @NotNull
  @NonNull
  private String vaule;

  @Column(name = "expires_at", nullable = true, updatable = true)
  @Nullable
  private Date expirationDate;

  @Column(name = "issued_at", nullable = false, updatable = true)
  @NotNull
  @NonNull
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
