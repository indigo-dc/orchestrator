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
import lombok.NoArgsConstructor;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@MappedSuperclass
@Embeddable
public abstract class OidcIssuerAwareId implements Serializable {

  private static final long serialVersionUID = -8057514331507625686L;

  @NonNull
  @NotNull
  @Column(name = "issuer", nullable = false, updatable = false)
  private String issuer;

}
