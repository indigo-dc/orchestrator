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

import it.reply.orchestrator.dal.util.ListStringToJsonConverter;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.validation.constraints.NotNull;

@Data
@Embeddable
@NoArgsConstructor
public class OidcTokenId implements Serializable {

  private static final long serialVersionUID = -8303859464474981940L;

  @Column(name = "clients_id", nullable = false, updatable = false)
  @NotNull
  @NonNull
  // Hibernate is not handling the converter correctly in JPA queries
  @Convert(converter = ListStringToJsonConverter.class)
  private List<String> clientsId = new ArrayList<>();
  // private String clientsId;

  @NotNull
  @NonNull
  @Embedded
  private OidcEntityId oidcEntityId;
}
