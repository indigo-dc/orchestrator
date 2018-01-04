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

import com.nimbusds.jwt.JWT;

import it.reply.orchestrator.utils.JwtUtils;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Embeddable
public class OidcEntityId extends OidcIssuerAwareId implements Serializable {

  private static final long serialVersionUID = -8303859464474981940L;

  @NonNull
  @Nonnull
  @Column(name = "subject", nullable = false, updatable = false)
  private String subject;

  /**
   * Generate a OidcEntityId from an access token.
   *
   * @return the OidcEntityId
   */
  public static OidcEntityId fromAccesToken(String accessToken) {
    JWT jwt = JwtUtils.parseJwt(accessToken);
    OidcEntityId id = new OidcEntityId();
    id.setIssuer(JwtUtils.getIssuer(jwt));
    id.setSubject(JwtUtils.getSubject(jwt));
    return id;
  }

}
