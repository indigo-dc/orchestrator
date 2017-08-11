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

package it.reply.orchestrator.config.properties;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Validated
@Data
@ConfigurationProperties(prefix = "orchestrator")
@NoArgsConstructor
public class OrchestratorProperties {

  @NotNull
  @NonNull
  private URI url;

  @Min(1)
  private int jobChunkSize = 1;

  @NotNull
  @NonNull
  @Valid
  @NestedConfigurationProperty
  private ExecutorServiceProperties executorService = new ExecutorServiceProperties();

  @Validated
  @Data
  @NoArgsConstructor
  public static class ExecutorServiceProperties {

    @Min(0)
    private int threadPoolSize = 10;

    @Min(1)
    private int interval = 2;

    @Min(0)
    private int retries = 3;

  }
}
