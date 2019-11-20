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

package it.reply.orchestrator.service;

import it.reply.orchestrator.dto.cmdb.CloudProvider;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.Flavor;
import it.reply.orchestrator.dto.cmdb.Image;
import it.reply.orchestrator.dto.cmdb.Tenant;

import java.util.List;
import java.util.Set;

public interface CmdbService {

  public CloudProvider getProviderById(String id);

  public List<CloudService> getServicesByProvider(String providerId);

  public CloudService getServiceById(String id);

  public List<Image> getImagesByTenant(String tenantId);

  public Image getImageById(String imageId);

  public List<Flavor> getFlavorsByTenant(String tenantId);

  public List<Tenant> getTenantsByService(String serviceId);

  public List<Tenant> getTenantsByOrganisation(String organisationId);

  public Tenant getTenantById(String tenantId);

  public Flavor getFlavorById(String flavorId);

  public CloudProvider fillCloudProviderInfo(String providerId,
      Set<String> servicesWithSla, String organisation);

}
