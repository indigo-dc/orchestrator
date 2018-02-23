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

package it.reply.orchestrator.resource;

import it.reply.orchestrator.dal.entity.OidcEntityId;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.resource.common.AbstractResource;
import it.reply.orchestrator.utils.CommonUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

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
public class DeploymentResource extends AbstractResource {

  @Nullable
  private Status status;

  @Nullable
  private String statusReason;

  @NotNull
  @NonNull
  private Map<String, Object> outputs = new HashMap<>();

  @Nullable
  private Task task;

  @Nullable
  private String callback;

  @Nullable
  private String cloudProviderName;

  @Nullable
  private OidcEntityId createdBy;

  @Builder
  protected DeploymentResource(@NonNull String uuid, @Nullable Date creationTime,
      @Nullable Date updateTime, @Nullable String physicalId, @Nullable Status status,
      @Nullable String statusReason, @Nullable Map<String, Object> outputs, @Nullable Task task,
      @Nullable String callback, @Nullable String cloudProviderName,
      @Nullable OidcEntityId createdBy) {
    super(uuid, creationTime, updateTime, physicalId);
    this.status = status;
    this.statusReason = statusReason;
    this.outputs = CommonUtils.notNullOrDefaultValue(outputs, HashMap::new);
    this.task = task;
    this.callback = callback;
    this.cloudProviderName = cloudProviderName;
    this.createdBy = createdBy;
  }

}
