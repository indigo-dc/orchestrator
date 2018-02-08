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

package it.reply.orchestrator.dto;

import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.ImageData;
import it.reply.orchestrator.dto.cmdb.Provider;
import it.reply.orchestrator.dto.cmdb.Type;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

/**
 * Internal CloudProvider representation (to contain joined data about the provider - from
 * SLAM/CMDB/etc)
 * 
 * @author l.biava
 *
 */
@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class CloudProvider implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  @NonNull
  @NotNull
  private String id;

  @Nullable
  private Provider cmdbProviderData;

  @NonNull
  @NotNull
  @Builder.Default
  private Map<String, CloudService> cmdbProviderServices = new HashMap<>();

  @NonNull
  @NotNull
  @Builder.Default
  private Map<String, List<ImageData>> cmdbProviderImages = new HashMap<>();

  @SuppressWarnings("null")
  @Deprecated
  protected CloudProvider() {
    cmdbProviderServices = new HashMap<>();
    cmdbProviderImages = new HashMap<>();
  }

  /**
   * Add the images of the cloud service.
   * 
   * @param cloudServiceId
   *          the cloud service Id
   * @param cmdbServiceImages
   *          the images of the compute cloud service
   */
  public void addCmdbCloudServiceImages(String cloudServiceId,
      Collection<ImageData> cmdbServiceImages) {
    cmdbProviderImages
        .computeIfAbsent(cloudServiceId, key -> new ArrayList<>())
        .addAll(cmdbServiceImages);
  }

  /**
   * Look for a Service in the current Provider of the given Type.
   * 
   * @param type
   *          the type.
   * @return the Service if found, <tt>null</tt> otherwise.
   */
  public List<CloudService> getCmbdProviderServicesByType(Type type) {
    return cmdbProviderServices
        .values()
        .stream()
        .filter(service -> type == service.getData().getType())
        .collect(Collectors.toList());
  }

}
