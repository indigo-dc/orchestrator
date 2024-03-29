/*
 * Copyright © 2015-2021 I.N.F.N.
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
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"processId", "requestId"}, callSuper = true)
@ToString(of = {"processId", "requestId"}, callSuper = true)
public class WorkflowReference extends UuidIdentifiable {

  public enum Action {
    CREATE,
    UPDATE,
    DELETE,
    EXECUTE
  }

  @Column(name = "process_id", unique = true, nullable = false, updatable = false)
  private String processId;

  @Column(name = "request_id", unique = true, nullable = false, updatable = false)
  private String requestId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Action action;

  @ManyToOne
  @JoinColumn(name = "deployment_id", nullable = false, updatable = false)
  private Deployment deployment;

  /**
   * Generate a WorkflowReference.
   *
   * @param processId
   *     the processId
   * @param requestId
   *     the requestId
   * @param action
   *     the action
   */
  public WorkflowReference(String processId, String requestId, Action action) {
    this.processId = processId;
    this.requestId = requestId;
    this.action = action;
  }

}
