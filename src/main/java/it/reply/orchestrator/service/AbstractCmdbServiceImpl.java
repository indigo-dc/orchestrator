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

package it.reply.orchestrator.service;

import it.reply.orchestrator.annotation.ServiceVersion;
import it.reply.orchestrator.dto.cmdb.CloudProvider;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.CloudServiceType;
import it.reply.orchestrator.dto.cmdb.ComputeService;
import it.reply.orchestrator.dto.cmdb.Flavor;
import it.reply.orchestrator.dto.cmdb.Image;
import it.reply.orchestrator.dto.cmdb.Tenant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
public abstract class AbstractCmdbServiceImpl implements CmdbService {

  @Override
  public CloudProvider fillCloudProviderInfo(String providerId,
      Set<String> servicesWithSla, String organisation) {
    // Get provider's data
    CloudProvider provider = getProviderById(providerId);
    Map<String, CloudService> services = getServicesByProvider(providerId)
        .stream()
        .filter(cs -> servicesWithSla.contains(cs.getId())
            || cs.getType().equals(CloudServiceType.STORAGE))
        .map(cloudService -> {
          if (cloudService instanceof ComputeService) {
            String prId = cloudService.getProviderId();
            String serviceId = cloudService.getId();
            ComputeService computeService = (ComputeService) cloudService;
            List<Tenant> serviceTenants = getTenantsByService(serviceId);
            List<Tenant> organisationTenants = getTenantsByOrganisation(organisation);
            List<Tenant> tenantList = serviceTenants.stream()
                .distinct()
                .filter(organisationTenants::contains)
                .collect(Collectors.toList());
            List<Image> imageList = new ArrayList<>();
            List<Flavor> flavorList = new ArrayList<>();
            if (!tenantList.isEmpty()) {
              //only one element must be here
              Tenant tenant = tenantList.get(0);
              imageList.addAll(getImagesByTenant(tenant.getId()));
              flavorList.addAll(getFlavorsByTenant(tenant.getId()));
              computeService.setPublicNetworkName(tenant.getPublicNetworkName());
              computeService.setPrivateNetworkName(tenant.getPrivateNetworkName());
              computeService.setPrivateNetworkCidr(tenant.getPrivateNetworkCidr());
              computeService.setTenant(tenant.getTenantName());
            }
            LOG.debug("Image list for service <{}> of provider <{}>: <{}>",
                Arrays.toString(imageList.toArray()), serviceId, prId);
            computeService.setImages(imageList);
            LOG.debug("Flavor list for service <{}> of provider <{}>: <{}>",
                Arrays.toString(flavorList.toArray()), serviceId, prId);
            computeService.setFlavors(flavorList);
          }
          return cloudService;
        })
        .collect(Collectors.toMap(CloudService::getId, Function.identity()));

    provider.setServices(services);
    return provider;
  }
}
