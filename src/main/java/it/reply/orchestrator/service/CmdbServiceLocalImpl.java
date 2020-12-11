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

import com.fasterxml.jackson.databind.ObjectMapper;
import it.reply.orchestrator.annotation.ServiceVersion;
import it.reply.orchestrator.config.properties.CmdbProperties;
import it.reply.orchestrator.dto.cmdb.CloudProvider;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.Flavor;
import it.reply.orchestrator.dto.cmdb.Image;
import it.reply.orchestrator.dto.cmdb.Tenant;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.exception.service.DeploymentException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
@ServiceVersion(SlamServiceLocalImpl.SERVICE_VERSION)
public class CmdbServiceLocalImpl extends AbstractCmdbServiceImpl {

  public static final String SERVICE_VERSION = "local";

  private final CmdbProperties cmdbProperties;
  private final ObjectMapper objectMapper;
  private final ResourceLoader resourceLoader;

  /**
   * Creates a new CmdbServiceLocalImpl.
   * @param cmdbProperties the cmdbProperties
   * @param objectMapper the objectMapper
   * @param resourceLoader the ResourceLoader
   */
  public CmdbServiceLocalImpl(CmdbProperties cmdbProperties, ObjectMapper objectMapper,
      ResourceLoader resourceLoader)  {
    this.cmdbProperties = cmdbProperties;
    this.objectMapper = objectMapper;
    this.resourceLoader = resourceLoader;
  }

  private LocalCmdbInfoHolder loadData() {
    String location = cmdbProperties.getUrl().toString();
    Resource serializedValues = resourceLoader
        .getResource(location);
    try (InputStream is = serializedValues.getInputStream()) {
      return objectMapper.readValue(is, LocalCmdbInfoHolder.class);
    } catch (IOException e) {
      throw new OrchestratorException("Error loading local CMDB info from " + location, e);
    }
  }

  @Override
  public CloudProvider getProviderById(String id) {
    return loadData()
        .getProviders()
        .stream()
        .filter(cloudProvider -> id.equals(cloudProvider.getId()))
        .findFirst()
        .orElseThrow(() -> new DeploymentException("Cloud provider not found: " + id));
  }

  @Override
  public List<CloudService> getServicesByProvider(String providerId) {
    return new ArrayList<>(getProviderById(providerId).getServices().values());
  }

  @Override
  public CloudService getServiceById(String id) {
    return loadData()
        .getProviders()
        .stream()
        .flatMap(cloudProvider -> cloudProvider.getServices().values().stream())
        .filter(cloudService -> id.equals(cloudService.getId()))
        .findFirst()
        .orElseThrow(() -> new DeploymentException("Cloud service not found: " + id));
  }

  @Override
  public List<Image> getImagesByTenant(String tenantId) {
    return Optional.ofNullable(loadData()
        .getTenants()
        .get(tenantId))
        .map(LocalCmdbTenantHolder::getImages)
        .orElseGet(Collections::emptyList);
  }

  @Override
  public Image getImageById(String imageId) {
    return loadData()
        .getTenants()
        .values()
        .stream()
        .flatMap(tenant -> tenant.getImages().stream())
        .filter(image -> imageId.equals(image.getId()))
        .findFirst()
        .orElseThrow(() -> new DeploymentException("Image not found: " + imageId));
  }

  @Override
  public List<Flavor> getFlavorsByTenant(String tenantId) {
    return Optional.ofNullable(loadData()
        .getTenants()
        .get(tenantId))
        .map(LocalCmdbTenantHolder::getFlavors)
        .orElseGet(Collections::emptyList);
  }

  @Override
  public List<Tenant> getTenantsByService(String serviceId) {
    return loadData()
        .getTenants()
        .values()
        .stream()
        .map(LocalCmdbTenantHolder::getTenant)
        .filter(tenant -> serviceId.equals(tenant.getService()))
        .collect(Collectors.toList());
  }

  @Override
  public List<Tenant> getTenantsByOrganisation(String organisationId) {
    return loadData()
        .getTenants()
        .values()
        .stream()
        .map(LocalCmdbTenantHolder::getTenant)
        .filter(tenant -> organisationId.equals(tenant.getIamOrganisation()))
        .collect(Collectors.toList());
  }

  @Override
  public Tenant getTenantById(String tenantId) {
    return Optional.ofNullable(loadData()
        .getTenants()
        .get(tenantId))
        .map(LocalCmdbTenantHolder::getTenant)
        .orElseThrow(() -> new DeploymentException("Tenant not found: " + tenantId));
  }

  @Override
  public Flavor getFlavorById(String flavorId) {
    return loadData()
        .getTenants()
        .values()
        .stream()
        .flatMap(tenant -> tenant.getFlavors().stream())
        .filter(flavor -> flavorId.equals(flavor.getFlavorId()))
        .findFirst()
        .orElseThrow(() -> new DeploymentException("Flavor not found: " + flavorId));
  }

  @Data
  @Builder
  @AllArgsConstructor(access = AccessLevel.PROTECTED)
  public static class LocalCmdbInfoHolder {
    @Builder.Default
    private List<CloudProvider> providers = new ArrayList<>();
    @Builder.Default
    private Map<String, LocalCmdbTenantHolder> tenants = new HashMap<>();

    @Deprecated
    protected LocalCmdbInfoHolder() {
      providers = new ArrayList<>();
      tenants = new HashMap<>();
    }
  }

  @Data
  @Builder
  @AllArgsConstructor(access = AccessLevel.PROTECTED)
  public static class LocalCmdbTenantHolder {

    private Tenant tenant;
    @Builder.Default
    private List<Image> images = new ArrayList<>();
    @Builder.Default
    private List<Flavor> flavors = new ArrayList<>();

    @Deprecated
    protected LocalCmdbTenantHolder() {
      images = new ArrayList<>();
      flavors = new ArrayList<>();
    }
  }
}
