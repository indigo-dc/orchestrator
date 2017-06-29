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

package it.reply.orchestrator.resource;

import it.reply.orchestrator.dal.entity.OidcEntityId;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.resource.common.AbstractResource;
import it.reply.orchestrator.utils.CommonUtils;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeploymentResource extends AbstractResource {

  private Status status;

  private String statusReason;

  @NonNull
  private Map<String, Object> outputs = new HashMap<>();

  private Task task;

  private String callback;

  private String cloudProviderName;

  private OidcEntityId createdBy;

  @Builder
  protected DeploymentResource(String uuid, Date creationTime, Date updateTime, Status status,
      String statusReason, Map<String, Object> outputs, Task task, String callback,
      String cloudProviderName, OidcEntityId createdBy) {
    super(uuid, creationTime, updateTime);
    this.status = status;
    this.statusReason = statusReason;
    this.outputs = CommonUtils.notNullOrDefaultValue(outputs, HashMap::new);
    this.task = task;
    this.callback = callback;
    this.cloudProviderName = cloudProviderName;
    this.createdBy = createdBy;
  }

}
