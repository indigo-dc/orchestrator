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

package it.reply.orchestrator.dto.cmdb;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Comparator;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Flavor implements CmdbIdentifiable, Comparable<Flavor> {

  private static final Comparator<Flavor> COMPARATOR = Comparator
      .comparing(Flavor::getNumGpus, Comparator.nullsFirst(Comparator.naturalOrder()))
      .thenComparing(Flavor::getNumCpus, Comparator.nullsFirst(Comparator.naturalOrder()))
      .thenComparing(Flavor::getMemSize, Comparator.nullsFirst(Comparator.naturalOrder()))
      .thenComparing(Flavor::getDiskSize, Comparator.nullsFirst(Comparator.naturalOrder()));

  @JsonProperty("id")
  @Nullable
  private String id;

  @JsonProperty("flavor_id")
  @Nullable
  private String flavorId;

  @JsonProperty("flavor_name")
  @Nullable
  private String flavorName;

  @JsonProperty("ram")
  @Nullable
  private Double memSize;

  @JsonProperty("num_vcpus")
  @Nullable
  private Integer numCpus;

  @JsonProperty("disk")
  @Nullable
  private Double diskSize;

  @JsonProperty("num_gpus")
  @Nullable
  private Integer numGpus;

  @JsonProperty("gpu_vendor")
  @Nullable
  private String gpuVendor;

  @JsonProperty("gpu_model")
  @Nullable
  private String gpuModel;

  @JsonProperty("infiniband_support")
  @Nullable
  private Boolean infinibandSupport;

  @Override
  public int compareTo(@NotNull Flavor other) {
    return COMPARATOR.compare(this, other);
  }
}
