/*
 * Copyright © 2015-2018 Santer Reply S.p.A.
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

import lombok.Data;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.validator.constraints.URL;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class DeploymentRequest {

  @NotNull(message = "A TOSCA template must be provided")
  private String template;

  @NotNull
  private Map<String, Object> parameters = new HashMap<>();

  @Nullable
  @URL(message = "Callback value, if provided, must be a valid HTTP or HTTPS URL",
      regexp = "^https?\\:.*")
  private String callback;

  @Nullable
  @Min(value = 1, message = "Timeout value, if provided, must be at least of 1 minute")
  private Integer timeoutMins;

}
