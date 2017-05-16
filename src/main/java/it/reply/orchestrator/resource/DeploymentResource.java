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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import it.reply.orchestrator.dal.entity.OidcEntityId;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.resource.common.AbstractResource;
import it.reply.orchestrator.resource.common.CustomSerializer;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DeploymentResource extends AbstractResource {

  private Status status;
  private String statusReason;

  @JsonSerialize(using = CustomSerializer.class)
  private Map<String, String> outputs;
  private Task task;
  private String callback;
  private List<BaseResource> resources;
  private String cloudProviderName;

  private OidcEntityId createdBy;

}
