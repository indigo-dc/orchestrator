package it.reply.orchestrator.config.properties;

import lombok.AccessLevel;

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

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

import javax.validation.constraints.NotNull;

@Data
@ToString(exclude = "password")
@PropertySource(value = { "classpath:application.properties", "${marathon.conf.file.path}" })
@ConfigurationProperties(prefix = "marathon")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MarathonProperties {

  @NotNull
  @NonNull
  private String url;

  @NotNull
  @NonNull
  private String username;

  @NotNull
  @NonNull
  private String password;

}
