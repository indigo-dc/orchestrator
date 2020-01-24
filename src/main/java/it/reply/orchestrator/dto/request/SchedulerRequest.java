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

package it.reply.orchestrator.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.validator.constraints.URL;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SchedulerRequest {

  @NotNull(message = "A Storage Path must be provided")
  @NonNull
  private String userStoragePath;

  @NotNull(message = "A TOSCA template must be provided")
  @NonNull
  private String template;

  @NotNull
  @NonNull
  @Builder.Default
  private Map<String, Object> parameters = new HashMap<>();

  @Nullable
  @URL(message = "Callback value, if provided, must be a valid HTTP or HTTPS URL",
      regexp = "^https?\\:.*")
  private String callback;

  @SuppressWarnings("null")
  @Deprecated
  protected SchedulerRequest() {
    parameters = new HashMap<>();
  }

}
