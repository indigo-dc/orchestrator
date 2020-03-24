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

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

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
public abstract class UuidIdentifiable implements Identifiable<String> {

  @Id
  @GenericGenerator(
      name = "sequential-uuid",
      strategy = "it.reply.orchestrator.dal.util.StrongSequentialUuidGenerator")
  @GeneratedValue(generator = "sequential-uuid")
  @Column(unique = true, nullable = false, updatable = false, length = 36)
  private String id;

}
