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

package it.reply.orchestrator.dto.cmdb;

import javax.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class MesosFrameworkServiceData<T extends MesosFrameworkServiceProperties> extends
    CloudServiceData {

  @NonNull
  @NotNull
  private T properties;

  public MesosFrameworkServiceData(
      @NonNull String serviceType,
      @NonNull String endpoint,
      @NonNull String providerId,
      @NonNull Type type,
      boolean publicService,
      @Nullable String region,
      @NonNull String hostname,
      @NonNull T properties) {
    super(serviceType, endpoint, providerId, type, publicService, region, hostname);
    this.properties = properties;
  }
}
