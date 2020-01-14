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
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.checkerframework.checker.nullness.qual.Nullable;

@Entity
@Table(indexes = {
    @Index(columnList = "createdAt")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StoragePathEntity extends AbstractResourceEntity {

  @Column(nullable = false)
  private String storagePath;

  @Lob
  @Basic
  @Column(nullable = false)
  private String template;

  @Column
  @Nullable
  private String callback;

  @Column(nullable = false)
  private String deploymentId;

  @ElementCollection(fetch = FetchType.EAGER, targetClass = String.class)
  @MapKeyColumn(name = "name")
  @Column(name = "value", table = "deployment_parameter", nullable = false,
      columnDefinition = "TEXT")
  @Convert(attributeName = "value.", converter = ObjectToJsonConverter.class)
  private Map<String, Object> parameters = new HashMap<>();

}
