/*
 * Copyright © 2015-2019 Santer Reply S.p.A.
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

import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@Data
@ConfigurationProperties(prefix = "cmdb")
@NoArgsConstructor
public class CmdbProperties {

  @NotNull
  @NonNull
  private URI url;

  @NotNull
  @NonNull
  private String providerByIdPath = "/provider/id/{providerId}?include_docs=true";

  @NotNull
  @NonNull
  private String servicesByProviderIdPath =
      "/provider/id/{providerId}/has_many/services?include_docs=true";

  @NotNull
  @NonNull
  private String serviceByIdPath = "/service/id/{serviceId}?include_docs=true";

  @NotNull
  @NonNull
  private String imagesByServiceIdPath =
      "/service/id/{serviceId}/has_many/images?include_docs=true";

  @NotNull
  @NonNull
  private String imageByIdPath = "/image/id/{imageId}?include_docs=true";

  @NotNull
  @NonNull
  private String flavorsByServiceIdPath =
      "/service/id/{serviceId}/has_many/flavors?include_docs=true";

  @NotNull
  @NonNull
  private String flavorByIdPath = "/flavor/id/{flavorId}?include_docs=true";

}
