/*
 * Copyright © 2015-2017 Santer Reply S.p.A.
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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(indexes = { @Index(columnList = AbstractResourceEntity.CREATED_COLUMN_NAME) })
@Getter
@Setter
@NoArgsConstructor
public class Deployment extends AbstractResourceEntity {

  private static final long serialVersionUID = 3866893436735377053L;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 500)
  private Status status;

  @Column(name = "statusReason", columnDefinition = "LONGTEXT")
  private String statusReason;

  @Enumerated(EnumType.STRING)
  @Column(name = "task")
  private Task task;

  @Enumerated(EnumType.STRING)
  @Column(name = "deploymentProvider")
  private DeploymentProvider deploymentProvider;

  @Column(name = "endpoint")
  private String endpoint;

  @Column(name = "callback")
  private String callback;

  @Column(name = "template", columnDefinition = "LONGTEXT")
  private String template;

  @Column(name = "cloudProviderEndpoint", columnDefinition = "TEXT")
  @Convert(converter = CloudProviderEndpointToJsonConverter.class)
  private CloudProviderEndpoint cloudProviderEndpoint;

  @ElementCollection(fetch = FetchType.EAGER, targetClass = String.class)
  @MapKeyColumn(name = "name")
  @Column(name = "value", columnDefinition = "TEXT")
  @Convert(attributeName = "value.", converter = ObjectToJsonConverter.class)
  private Map<String, Object> parameters = new HashMap<>();

  @ElementCollection(fetch = FetchType.EAGER, targetClass = String.class)
  @MapKeyColumn(name = "name")
  @Column(name = "value", columnDefinition = "TEXT")
  @Convert(attributeName = "value.", converter = ObjectToJsonConverter.class)
  private Map<String, Object> outputs = new HashMap<>();

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "deployment", orphanRemoval = true)
  private List<Resource> resources = new ArrayList<>();

  @Column(name = "cloudProviderName", length = 128)
  private String cloudProviderName;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "deployment", orphanRemoval = true)
  private List<WorkflowReference> workflowReferences = new ArrayList<>();

  @ManyToOne(
      cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
  @JoinColumn(name = "OWNER_ID")
  private OidcEntity owner;

  @Transient
  public void addWorkflowReferences(WorkflowReference workflowReference) {
    workflowReference.setDeployment(this);
    this.workflowReferences.add(workflowReference);
  }

}
