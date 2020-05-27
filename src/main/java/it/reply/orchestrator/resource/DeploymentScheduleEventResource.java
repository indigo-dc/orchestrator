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

package it.reply.orchestrator.resource;

import it.reply.orchestrator.dal.entity.OidcEntityId;
import it.reply.orchestrator.enums.DeploymentScheduleStatus;
import it.reply.orchestrator.resource.common.AbstractResource;
import java.util.Date;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeploymentScheduleEventResource extends AbstractResource {

  private DeploymentResource deployment;

  private String scope;

  private String name;

  @Nullable
  private String replicationStatus;

  @Builder
  public DeploymentScheduleEventResource(@NonNull String uuid, @Nullable Date creationTime, @Nullable Date updateTime, @Nullable String physicalId, DeploymentResource deployment, String scope, String name, @Nullable String replicationStatus) {
    super(uuid, creationTime, updateTime, physicalId);
    this.deployment = deployment;
    this.scope = scope;
    this.name = name;
    this.replicationStatus = replicationStatus;
  }

}
