/*
 * Copyright © 2019 I.N.F.N.
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

package it.reply.orchestrator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

import org.checkerframework.checker.nullness.qual.NonNull;

@Data
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class SystemEndpoints {

  @NonNull
  @JsonProperty("cpr_url")
  private URI cprurl;

  @NonNull
  @JsonProperty("slam_url")
  private URI slamurl;

  @NonNull
  @JsonProperty("cdb_url")
  private URI cmdburl;

  @NonNull
  @JsonProperty("im_url")
  private URI imurl;

  @NonNull
  @JsonProperty("monitoring_url")
  private URI monitoringurl;

  @NonNull
  @JsonProperty("vault_url")
  private URI vaulturl;

}
