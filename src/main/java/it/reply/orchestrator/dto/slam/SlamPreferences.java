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

package it.reply.orchestrator.dto.slam;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import org.checkerframework.checker.nullness.qual.NonNull;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class SlamPreferences {

  @JsonProperty("preferences")
  @NonNull
  @NotNull
  @Builder.Default
  private List<Preference> preferences = new ArrayList<>();

  @JsonProperty("sla")
  @NonNull
  @NotNull
  @Builder.Default
  private List<Sla> sla = new ArrayList<>();

  @Deprecated
  protected SlamPreferences() {
    preferences = new ArrayList<>();
    sla = new ArrayList<>();
  }

}
