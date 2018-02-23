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

package it.reply.orchestrator.config.properties;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Data
@ConfigurationProperties(prefix = "mesos")
@NoArgsConstructor
@Component
public class MesosProperties implements InitializingBean {

  @NotNull
  @NonNull
  @Valid
  @NestedConfigurationProperty
  private Map<String, MesosInstanceProperties> instances = new HashMap<>();

  public Optional<MesosInstanceProperties> getInstance(String cloudProvidername) {
    return Optional.ofNullable(instances.get(cloudProvidername));
  }

  @Data
  @Validated
  @NoArgsConstructor
  public static class MesosInstanceProperties {

    @NotNull
    @NonNull
    @Valid
    @NestedConfigurationProperty
    private MarathonProperties marathon;

    @NotNull
    @NonNull
    @Valid
    @NestedConfigurationProperty
    private ChronosProperties chronos;

  }

  @Override
  public void afterPropertiesSet() throws Exception {
    instances.forEach((key, value) -> {
      value.marathon.afterPropertiesSet();
      value.chronos.afterPropertiesSet();
    });

  }
}
