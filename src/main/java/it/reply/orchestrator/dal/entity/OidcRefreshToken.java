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

package it.reply.orchestrator.dal.entity;

import com.google.common.collect.Lists;

import com.fasterxml.jackson.core.type.TypeReference;

import it.reply.orchestrator.utils.JwtUtils;
import it.reply.utils.json.JsonUtility;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hibernate.annotations.GenericGenerator;
import org.springframework.hateoas.Identifiable;
import org.springframework.social.oauth2.AccessGrant;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Data
@NoArgsConstructor
@EqualsAndHashCode(of = { "vaule" })
@Entity
@Slf4j
@Table(indexes = { @Index(name = "indexOriginalJtiAndEntity",
    columnList = "ORIGINAL_TOKEN_ID" + "," + "OIDC_ENTITY_ID") })
public class OidcRefreshToken implements Identifiable<String>, Serializable {

  private static final long serialVersionUID = 2472751784540925184L;

  /**
   * Generate a OidcRefreshToken from a token id and a grant.
   * 
   * @param currentTokenId
   *          the token id
   * @param grant
   *          the grant
   * @return the OidcRefreshToken
   */
  public static OidcRefreshToken fromAccessGrant(OidcTokenId currentTokenId, AccessGrant grant) {
    OidcRefreshToken token = new OidcRefreshToken();
    token.setOriginalTokenId(currentTokenId.getJti());
    token.setVaule(grant.getRefreshToken());
    token.setCreationDate(new Date());

    Optional.ofNullable(grant.getScope())
        .map(scopeAsString -> scopeAsString.split(" +"))
        .map(scopes -> Lists.newArrayList(scopes))
        .ifPresent(scopes -> token.setScopes(scopes));

    JwtUtils.getExpirationTimeFromJwt(token.getVaule())
        .ifPresent(expirationDate -> token.setExpirationDate(expirationDate));

    return token;
  }

  @Id
  @GeneratedValue(generator = "uuid")
  @GenericGenerator(name = "uuid", strategy = "uuid2")
  @Column(name = AbstractResourceEntity.ID_COLUMN_NAME, unique = true)
  private String id;

  @Column(name = "REFRESH_TOKEN_VALUE")
  private String vaule;

  @Column(name = "ORIGINAL_TOKEN_ID")
  private String originalTokenId;

  @Column(name = "EXPIRATION")
  private Date expirationDate;

  @Column(name = "ISSUED_AT")
  private Date creationDate;

  @Column(name = "SCOPES")
  private String scopes;

  @OneToOne(
      cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
  @JoinColumn(name = "OIDC_ENTITY_ID", unique = true, nullable = true)
  private OidcEntity entity;

  /**
   * Get the scopes of the refresh token.
   * 
   * @return the scopes
   */
  public List<String> getScopes() {
    Optional<List<String>> deserializedScopes = Optional.empty();
    try {
      deserializedScopes = Optional
          .ofNullable(JsonUtility.deserializeJson(scopes, new TypeReference<List<String>>() {
          }));
    } catch (Exception ex) {
      // swallow it
    }
    return deserializedScopes.orElse(new ArrayList<>());
  }

  /**
   * Set the scopes of the refresh token.
   * 
   * @param scopes
   *          the scopes
   */
  public void setScopes(List<String> scopes) {
    try {
      this.scopes = JsonUtility.serializeJson(scopes);
    } catch (Exception ex) {
      // swallow it
    }
  }

}
