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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.springframework.hateoas.Identifiable;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

@Entity
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = { "processId", "requestId" })
@ToString(of = { "processId", "requestId" })
public class WorkflowReference implements Identifiable<String>, Serializable {

  private static final long serialVersionUID = -610233480056664663L;

  @Id
  @Column(name = "process_id", unique = true, nullable = false, updatable = false)
  private String processId;

  @Column(name = "request_id", unique = true, nullable = false, updatable = false)
  private String requestId;

  @ManyToOne
  @JoinColumn(name = "deployment_id")
  private Deployment deployment;

  public WorkflowReference(String processId, String requestId) {
    this.processId = processId;
    this.requestId = requestId;
  }

  @Override
  @Transient
  public String getId() {
    return this.getProcessId();
  }

}
