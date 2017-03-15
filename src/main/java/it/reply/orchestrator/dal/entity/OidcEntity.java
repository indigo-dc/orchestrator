package it.reply.orchestrator.dal.entity;

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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.GenericGenerator;
import org.springframework.hateoas.Identifiable;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "oidcEntityId")
@Table(uniqueConstraints = {
    @UniqueConstraint(name = "uniqueIssuerAndSubject", columnNames = { "ISSUER", "SUBJECT" }) })
@Entity
public class OidcEntity implements Identifiable<String>, Serializable {

  private static final long serialVersionUID = 2472751784540925184L;

  @Id
  @GeneratedValue(generator = "uuid")
  @GenericGenerator(name = "uuid", strategy = "uuid2")
  @Column(name = AbstractResourceEntity.ID_COLUMN_NAME, unique = true)
  private String id;

  @Embedded
  private OidcEntityId oidcEntityId;

  @Column(name = "ORGANIZATION")
  private String organization;

  @OneToOne(mappedBy = "entity", cascade = { CascadeType.ALL })
  private OidcRefreshToken refreshToken;

  public void setRefreshToken(OidcRefreshToken refreshToken) {
    refreshToken.setEntity(this);
    this.refreshToken = refreshToken;
  }

}
