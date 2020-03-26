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

package it.reply.orchestrator.dto.cmdb;

import com.fasterxml.jackson.annotation.JsonProperty;

import it.reply.orchestrator.exception.service.ToscaException;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Data
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class MesosFrameworkServiceProperties {

  @Nullable
  @JsonProperty("local_volumes_host_base_path")
  private String localVolumesHostBasePath;

  @JsonProperty("gpu_support")
  private boolean gpuSupport;

  @NonNull
  @NotNull
  @JsonProperty("persistent_storage_drivers")
  private List<String> persistentStorageDrivers = new ArrayList<>();

  @Deprecated
  protected MesosFrameworkServiceProperties() {
    persistentStorageDrivers = new ArrayList<>();
  }

  /**
   * Generate the local volume path.
   *
   * @param deploymentId
   *     the deployment ID
   * @return the path
   */
  public String generateLocalVolumesHostPath(String deploymentId) {
    if (StringUtils.isBlank(localVolumesHostBasePath)) {
      throw new ToscaException("Error generating host path: no base host path has been provided");
    } else {
      return localVolumesHostBasePath.concat(deploymentId);
    }
  }
}
