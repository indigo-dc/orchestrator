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

import it.reply.workflowmanager.orchestrator.bpm.BusinessProcessManager;
import it.reply.workflowmanager.orchestrator.bpm.BusinessProcessManager.RUNTIME_STRATEGY;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.springframework.hateoas.Identifiable;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

@Entity
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = { "processId" })
@ToString(of = { "processId", "runtimeStrategy" })
public class WorkflowReference implements Identifiable<Long>, Serializable {

  private static final long serialVersionUID = -610233480056664663L;

  @Id
  @Column(name = "process_id", unique = true, nullable = false)
  private long processId;

  @Enumerated(EnumType.STRING)
  @Column(name = "runtime_strategy", length = 100, nullable = false)
  private BusinessProcessManager.RUNTIME_STRATEGY runtimeStrategy;

  @ManyToOne
  @JoinColumn(name = "deployment_uuid")
  private Deployment deployment;

  /**
   * Constructor with fields.
   * 
   * @param processId
   *          the process id
   * @param runtimeStrategy
   *          the strategy {@Link RUNTIME_STRATEGY}
   */
  public WorkflowReference(long processId, RUNTIME_STRATEGY runtimeStrategy) {
    super();
    this.processId = processId;
    this.runtimeStrategy = runtimeStrategy;
  }

  @Override
  @Transient
  public Long getId() {
    return this.getProcessId();
  }

}
