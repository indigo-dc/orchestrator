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

import it.reply.orchestrator.dal.util.CloudProviderEndpointToJsonConverter;
import it.reply.orchestrator.dal.util.ObjectToJsonConverter;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.checkerframework.checker.nullness.qual.Nullable;

@Entity
@Table(indexes = {
    @Index(columnList = "createdAt"),
    @Index(columnList = "owner_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Deployment extends AbstractResourceEntity {

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status;

  @Lob
  @Basic
  @Nullable
  private String statusReason;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Task task;

  @Enumerated(EnumType.STRING)
  @Nullable
  private DeploymentProvider deploymentProvider;

  @Nullable
  private String endpoint;

  @Nullable
  private String callback;

  @Lob
  @Basic
  @Column(nullable = false)
  private String template;

  @Column(columnDefinition = "TEXT")
  @Convert(converter = CloudProviderEndpointToJsonConverter.class)
  @Nullable
  private CloudProviderEndpoint cloudProviderEndpoint;

  @ElementCollection(fetch = FetchType.EAGER, targetClass = String.class)
  @MapKeyColumn(name = "name")
  @Column(name = "value", table = "deployment_parameter", nullable = false,
      columnDefinition = "TEXT")
  @Convert(attributeName = "value.", converter = ObjectToJsonConverter.class)
  private Map<String, Object> parameters = new HashMap<>();

  @ElementCollection(fetch = FetchType.EAGER, targetClass = String.class)
  @MapKeyColumn(name = "name")
  @Column(name = "value", table = "deployment_output", nullable = false, columnDefinition = "TEXT")
  @Convert(attributeName = "value.", converter = ObjectToJsonConverter.class)
  private Map<String, Object> outputs = new HashMap<>();

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "deployment", orphanRemoval = true)
  private Set<Resource> resources = new HashSet<>();

  @Nullable
  private String cloudProviderName;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "deployment", orphanRemoval = true)
  private Set<WorkflowReference> workflowReferences = new HashSet<>();

  @ManyToOne(cascade = {
      CascadeType.DETACH,
      CascadeType.MERGE,
      CascadeType.PERSIST,
      CascadeType.REFRESH
  })
  @JoinColumn(name = "owner_id")
  @Nullable
  private OidcEntity owner;

  @Transient
  public void addWorkflowReferences(WorkflowReference workflowReference) {
    workflowReference.setDeployment(this);
    this.workflowReferences.add(workflowReference);
  }

  @OneToOne(mappedBy = "deployment", cascade = CascadeType.ALL, fetch = FetchType.LAZY,
      optional = true)
  @Nullable
  private DeploymentScheduleEvent deploymentScheduleEvent;
}
