package it.reply.orchestrator.service;

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
import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.Image;
import it.reply.orchestrator.dto.cmdb.Provider;

import java.util.List;

public interface CmdbService {

  public CloudService getServiceById(String id);

  public Provider getProviderById(String id);

  // /**
  // * Complex method to retrieve {@link Image}s with all metadata for a given provider. <br/>
  // * Currently, it is needed to extract the correct service from the provider and the query the
  // CMDB
  // * to retrieve metadata of each single image (because only ID and Name are listed in the API).
  // *
  // * @param providerId
  // * .
  // * @return .
  // */
  // public List<Image> getImagesByProvider(String providerId);

  public List<Image> getImagesByService(String serviceId);

  public Image getImageById(String imageId);

  public String getUrl();

  public CloudProvider fillCloudProviderInfo(CloudProvider cp);

  public List<CloudService> getServicesByProvider(String providerId);
}
