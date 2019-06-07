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

package it.reply.orchestrator.dto.policies;

import com.fasterxml.jackson.annotation.JsonProperty;

import it.reply.orchestrator.utils.ToscaConstants.Policies.Types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.checkerframework.checker.nullness.qual.NonNull;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class SlaPlacementPolicy extends GenericToscaPolicy {

  @JsonProperty("sla_id")
  @NonNull
  @NotNull
  private String slaId;

  @JsonProperty("services_id")
  @NonNull
  @NotNull
  private List<String> servicesId = new ArrayList<>();

  @Deprecated
  protected SlaPlacementPolicy() {
    this(Types.SLA_PLACEMENT);
  }

  protected SlaPlacementPolicy(@NonNull String type) {
    super(type);
    servicesId = new ArrayList<>();
  }

  protected SlaPlacementPolicy(
      @NonNull String type,
      @NonNull Set<String> targets,
      @NonNull String slaId
  ) {
    super(type, targets);
    this.slaId = Objects.requireNonNull(slaId);
    servicesId = new ArrayList<>();
  }

  public SlaPlacementPolicy(
      @NonNull Set<String> targets,
      @NonNull String slaId
  ) {
    this(Types.SLA_PLACEMENT, targets, slaId);
  }

}
