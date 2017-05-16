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

package it.reply.orchestrator.dto;

import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.CloudServiceData;
import it.reply.orchestrator.dto.cmdb.ImageData;
import it.reply.orchestrator.dto.cmdb.Provider;
import it.reply.orchestrator.dto.cmdb.Type;

import lombok.Data;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Internal CloudProvider representation (to contain joined data about the provider - from
 * SLAM/CMDB/etc)
 * 
 * @author l.biava
 *
 */
@Data
public class CloudProvider implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  private String name;
  private String id;

  private Provider cmdbProviderData;

  @NonNull
  private Map<String, CloudService> cmdbProviderServices = new HashMap<>();

  @NonNull
  private Map<String, List<ImageData>> cmdbProviderImages = new HashMap<>();

  public CloudProvider() {
  }

  public CloudProvider(String id) {
    this.id = id;
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
    cmdbProviderImages.computeIfAbsent(cloudServiceId, key -> new ArrayList<>())
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
    return cmdbProviderServices.values().stream().filter(service -> {
      CloudServiceData data = service.getData();
      return data != null && type == data.getType();
    }).collect(Collectors.toList());
  }

}
