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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Version;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.hibernate.annotations.GenericGenerator;
import org.springframework.hateoas.Identifiable;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@ToString
public abstract class AbstractResourceEntity implements Identifiable<String> {

  public static final String ID_COLUMN_NAME = "uuid";
  public static final String CREATED_COLUMN_NAME = "created";

  @Id
  @GeneratedValue(generator = "uuid")
  @GenericGenerator(name = "uuid", strategy = "uuid2")
  @Column(name = ID_COLUMN_NAME, unique = true)
  private String id;

  @Column(name = CREATED_COLUMN_NAME)
  private Date created;

  @Column(name = "updated")
  private Date updated;

  @Version
  private Long version;

  @PrePersist
  protected void onCreate() {
    this.created = new Date();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updated = new Date();
  }
}
