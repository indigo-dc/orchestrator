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

import it.reply.orchestrator.dal.util.CloudProviderEndpointToJsonConverter;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.enums.NodeStates;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(indexes = { @Index(columnList = "toscaNodeName"), @Index(columnList = "deployment_uuid"),
    @Index(columnList = AbstractResourceEntity.CREATED_COLUMN_NAME) })
@Getter
@Setter
@NoArgsConstructor
public class Resource extends AbstractResourceEntity {

  private static final long serialVersionUID = -4916577635363604624L;

  @Enumerated(EnumType.STRING)
  @Column(name = "state", length = 500)
  private NodeStates state;

  @Column(name = "iaasId", length = 500)
  private String iaasId;

  @Column(name = "toscaNodeType")
  private String toscaNodeType;

  @Column(name = "toscaNodeName")
  private String toscaNodeName;

  @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
  @JoinTable(name = "Resource_requiredBy", joinColumns = @JoinColumn(name = "Resource_uuid"),
      inverseJoinColumns = @JoinColumn(name = "requiredBy"))
  private Set<Resource> requiredBy = new HashSet<>();

  @ManyToMany(mappedBy = "requiredBy")
  private Set<Resource> requires = new HashSet<>();

  @ManyToOne
  @JoinColumn(name = "deployment_uuid")
  private Deployment deployment;

  @Column(name = "cloudProviderEndpoint", columnDefinition = "TEXT")
  @Convert(converter = CloudProviderEndpointToJsonConverter.class)
  private CloudProviderEndpoint cloudProviderEndpoint;

  public void addRequiredResource(Resource resource) {
    requires.add(resource);
    resource.requiredBy.add(this);
  }

  public void removeRequiredResource(Resource resource) {
    requires.remove(resource);
    resource.requiredBy.remove(this);
  }
}
