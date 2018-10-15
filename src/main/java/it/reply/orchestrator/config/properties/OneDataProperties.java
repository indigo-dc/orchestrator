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

import java.net.URI;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

@Validated
@Data
@ConfigurationProperties(prefix = "onedata")
@NoArgsConstructor
public class OneDataProperties {

  @NotNull
  @NonNull
  private URI onezoneUrl;

  @NotNull
  @NonNull
  private String onezoneBasePath = "/api/v3/onezone/";

  @NotNull
  @NonNull
  private String oneproviderBasePath = "/api/v3/oneprovider/";

  @NotNull
  @NonNull
  @Valid
  @NestedConfigurationProperty
  private ServiceSpaceProperties serviceSpace;

  @Validated
  @Data
  @NoArgsConstructor
  @ToString(exclude = "token")
  public static class ServiceSpaceProperties {

    @Nullable
    private URI onezoneUrl;

    @NotNull
    @NonNull
    private String token;

    @NotNull
    @NonNull
    private String name;

    @NotNull
    @NonNull
    @NotBlank
    private String baseFolderPath = "/";

  }
}
