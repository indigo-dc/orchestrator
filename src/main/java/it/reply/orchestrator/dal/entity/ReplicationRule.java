/*
 * Copyright © 2015-2020 Santer Reply S.p.A.
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

import it.reply.orchestrator.enums.ReplicationRuleStatus;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.Nullable;

@Entity
@Table(indexes = {
    @Index(columnList = "rucioId", unique = true),
    @Index(
        columnList = "scope, name, replicationExpression, numberOfReplicas, rucioAccount, deleted",
        unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class ReplicationRule extends AbstractResourceEntity {

  @Column(nullable = false, updatable = false)
  private String rucioId;

  @Column(nullable = false)
  private String rucioAccount;

  @Column(nullable = false)
  private String scope;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private int usageCount = 1;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReplicationRuleStatus status;

  @Lob
  @Basic
  @Nullable
  private String statusReason;

  @Column(nullable = false)
  private String replicationExpression;

  @Column(nullable = false)
  private Integer numberOfReplicas;

  @ManyToOne(cascade = {
      CascadeType.DETACH,
      CascadeType.MERGE,
      CascadeType.PERSIST,
      CascadeType.REFRESH
  })
  @JoinColumn(name = "owner_id")
  @Nullable
  private OidcEntity owner;

  @Getter(value = AccessLevel.NONE)
  @Setter(value = AccessLevel.NONE)
  private Boolean deleted = false;

  public Boolean getDeleted() {
    return deleted != null;
  }

  public void setDeleted(Boolean deleted) {
    this.deleted = deleted ? null : false;
  }
}
