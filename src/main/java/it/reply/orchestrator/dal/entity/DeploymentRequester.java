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
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(indexes = {
    @Index(columnList = "createdAt")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeploymentRequester extends AbstractResourceEntity  {

  @Column(nullable = false)
  private String storagePath;

  @OneToOne
  @JoinColumn(name = "scheduler_id", nullable = false, updatable = false)
  private DeploymentScheduler deploymentScheduler;

  @ManyToOne
  @JoinColumn(name = "deployment_id", nullable = false, updatable = false)
  private Deployment deployment;

}
