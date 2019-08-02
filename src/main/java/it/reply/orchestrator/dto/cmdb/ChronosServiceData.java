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

import it.reply.orchestrator.dto.cmdb.ChronosServiceData.ChronosServiceProperties;
import it.reply.orchestrator.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChronosServiceData extends MesosFrameworkServiceData<ChronosServiceProperties> {

  @Builder(builderMethodName = "chronosBuilder")
  public ChronosServiceData(
      @NonNull String serviceType,
      @NonNull String endpoint,
      @NonNull String providerId,
      @NonNull Type type,
      boolean publicService,
      @Nullable String region,
      @NonNull String hostname,
      @NonNull ChronosServiceProperties properties) {
    super(serviceType, endpoint, providerId, type, publicService, region, hostname, properties);
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public static class ChronosServiceProperties extends MesosFrameworkServiceProperties {

    @Builder
    public ChronosServiceProperties(
        @Nullable String localVolumesHostBasePath,
        boolean gpuSupport,
        @NonNull List<String> persistentStorageDrivers) {
      super(localVolumesHostBasePath, gpuSupport, CommonUtils
          .notNullOrDefaultValue(persistentStorageDrivers, ArrayList::new));
    }
  }
}
