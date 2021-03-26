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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import org.checkerframework.checker.nullness.qual.NonNull;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class CloudProvider implements CmdbIdentifiable {

  @JsonProperty("id")
  @NonNull
  @NotNull
  private String id;

  @JsonProperty("name")
  @NonNull
  @NotNull
  private String name;

  @JsonProperty("services")
  @NonNull
  @NotNull
  @Builder.Default
  Map<String, CloudService> services = new HashMap<>();

  @Deprecated
  private CloudProvider() {
    services = new HashMap<>();
  }

  /**
   * Get all services instance of a specified class.
   *
   * @param <T>
   *     the object type
   * @param clazz
   *     the base class required
   * @return the list of services
   */
  @JsonIgnore
  public <T extends CloudService> List<T> getServicesOfType(Class<T> clazz) {
    return services
        .values()
        .stream()
        .filter(clazz::isInstance)
        .map(clazz::cast)
        .collect(Collectors.toList());
  }

  /**
   * Get all services instance of a specified {@link CloudServiceType}.
   *
   * @param type
   *     the required type
   * @return the list of services
   */
  @JsonIgnore
  public List<CloudService> getServicesOfType(CloudServiceType type) {
    return services
        .values()
        .stream()
        .filter(cs -> cs.getType().equals(type))
        .collect(Collectors.toList());
  }
}
