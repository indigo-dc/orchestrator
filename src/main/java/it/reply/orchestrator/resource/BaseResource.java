/*
 * Copyright © 2015-2019 Santer Reply S.p.A.
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

import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.resource.common.AbstractResource;
import it.reply.orchestrator.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
public class BaseResource extends AbstractResource {

  @Nullable
  private NodeStates state;

  @Nullable
  private String toscaNodeType;

  @Nullable
  private String toscaNodeName;

  @NotNull
  @NonNull
  private List<String> requiredBy = new ArrayList<>();

  @Builder
  protected BaseResource(@NonNull String uuid, @Nullable Date creationTime,
      @Nullable Date updateTime, @Nullable String physicalId,
      @Nullable NodeStates state, @Nullable String toscaNodeType, @Nullable String toscaNodeName,
      @Nullable List<String> requiredBy) {
    super(uuid, creationTime, updateTime, physicalId);
    this.state = state;
    this.toscaNodeType = toscaNodeType;
    this.toscaNodeName = toscaNodeName;
    this.requiredBy = CommonUtils.notNullOrDefaultValue(requiredBy, ArrayList::new);
  }

}
