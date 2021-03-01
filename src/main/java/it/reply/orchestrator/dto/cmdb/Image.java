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

package it.reply.orchestrator.dto.cmdb;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.checkerframework.checker.nullness.qual.Nullable;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Image implements CmdbIdentifiable {

  @JsonProperty("id")
  @Nullable
  private String id;

  @JsonProperty("image_id")
  @Nullable
  private String imageId;

  @JsonProperty("image_name")
  @Nullable
  private String imageName;

  @JsonProperty("image_description")
  @Nullable
  private String imageDescription;

  @JsonProperty("architecture")
  @Nullable
  private String architecture;

  @JsonProperty("type")
  @Nullable
  private String type;

  @JsonProperty("distribution")
  @Nullable
  private String distribution;

  @JsonProperty("version")
  @Nullable
  private String version;

  @JsonProperty("service")
  @Nullable
  private String service;

  @JsonProperty("user_name")
  @Nullable
  private String userName;

  @JsonProperty("gpu_driver")
  @Nullable
  private Boolean gpuDriver;

  @JsonProperty("gpu_driver_version")
  @Nullable
  private String gpuDriverVersion;

  @JsonProperty("cuda_support")
  @Nullable
  private Boolean cudaSupport;

  @JsonProperty("cuda_version")
  @Nullable
  private String cudaVersion;

  @JsonProperty("cuDNN_version")
  @Nullable
  private String cuDnnVersion;
}
