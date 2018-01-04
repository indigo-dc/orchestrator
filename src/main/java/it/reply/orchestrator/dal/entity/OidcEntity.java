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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.hateoas.Identifiable;

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "oidcEntityId")
@Table(name = "oidc_entity", uniqueConstraints = {
    @UniqueConstraint(name = "uq_issuer_subject", columnNames = { "issuer", "subject" }) })
@Entity
public class OidcEntity implements Identifiable<String> {

  @Id
  @GeneratedValue(generator = "uuid")
  @GenericGenerator(name = "uuid", strategy = "uuid2")
  @Column(name = AbstractResourceEntity.ID_COLUMN_NAME, unique = true, nullable = false,
      updatable = false)
  private String id;

  @NonNull
  @Nonnull
  @Embedded
  private OidcEntityId oidcEntityId;

  @NonNull
  @Nonnull
  @Column(name = "organization", nullable = false, updatable = false)
  private String organization;

}
