/*
 * Copyright © 2015-2021 I.N.F.N.
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

package it.reply.orchestrator.dto.ranker;

import com.fasterxml.jackson.annotation.JsonProperty;

import it.reply.monitoringpillar.domain.dsl.monitoring.pillar.wrapper.paas.PaaSMetric;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class MonitoringService {

  @JsonProperty("service_id")
  @NonNull
  @NotNull
  private String serviceId;

  @JsonProperty("service_parent_id")
  @Nullable
  private String parentServiceId;

  @JsonProperty("type")
  @NonNull
  @NotNull
  private String type;

  @JsonProperty("metrics")
  @Builder.Default
  private List<PaaSMetric> metrics = new ArrayList<>();

  @Deprecated
  protected MonitoringService() {
    metrics = new ArrayList<>();
  }
}
