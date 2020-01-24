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

import it.reply.orchestrator.dal.util.ObjectToJsonConverter;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.checkerframework.checker.nullness.qual.Nullable;

@Entity
@Table(indexes = {
    @Index(columnList = "createdAt"),
    @Index(columnList = "owner_id"),
    @Index(columnList = "requested_with_token_id")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeploymentScheduler extends AbstractResourceEntity {

  @Column(nullable = false)
  private String userStoragePath;

  @Lob
  @Basic
  @Column(nullable = false)
  private String template;

  @Column
  @Nullable
  private String callback;

  @ElementCollection(fetch = FetchType.EAGER, targetClass = String.class)
  @MapKeyColumn(name = "name")
  @Column(name = "value", table = "deployment_scheduler_parameter", nullable = false,
      columnDefinition = "TEXT")
  @Convert(attributeName = "value.", converter = ObjectToJsonConverter.class)
  private Map<String, Object> parameters = new HashMap<>();

  @ManyToOne(cascade = {
      CascadeType.DETACH,
      CascadeType.MERGE,
      //CascadeType.PERSIST,
      CascadeType.REFRESH
  })
  @JoinColumn(name = "owner_id")
  @Nullable
  private OidcEntity owner;
  
  @ManyToOne(cascade = {
      CascadeType.DETACH,
      CascadeType.MERGE,
      //CascadeType.PERSIST,
      CascadeType.REFRESH
  })
  @JoinColumn(name = "requested_with_token_id")
  @Nullable
  private OidcRefreshToken requestedWithToken;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    DeploymentScheduler other = (DeploymentScheduler) obj;
    if (template == null) {
      if (other.template != null) {
        return false;
      }
    } else if (!template.equals(other.template)) {
      return false;
    }
    if (userStoragePath == null) {
      if (other.userStoragePath != null) {
        return false;
      }
    } else if (!userStoragePath.equals(other.userStoragePath)) {
      return false;
    }
    return true;
  }

}
