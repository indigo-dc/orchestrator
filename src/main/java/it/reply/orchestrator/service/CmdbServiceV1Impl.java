/*
 * Copyright © 2015-2021 Santer Reply S.p.A.
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
import it.reply.orchestrator.config.properties.CmdbProperties;
import it.reply.orchestrator.dto.cmdb.CloudProvider;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.CmdbIdentifiable;
import it.reply.orchestrator.dto.cmdb.Flavor;
import it.reply.orchestrator.dto.cmdb.Image;
import it.reply.orchestrator.dto.cmdb.Tenant;
import it.reply.orchestrator.dto.cmdb.wrappers.CmdbDataWrapper;
import it.reply.orchestrator.dto.cmdb.wrappers.CmdbHasManyList;
import it.reply.orchestrator.dto.cmdb.wrappers.CmdbRow;
import it.reply.orchestrator.exception.service.DeploymentException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
@ServiceVersion("v1")
public class CmdbServiceV1Impl extends AbstractCmdbServiceImpl {

  private static final ParameterizedTypeReference<CmdbDataWrapper<CloudProvider>>
      PROVIDER_RESPONSE_TYPE =
      new ParameterizedTypeReference<CmdbDataWrapper<CloudProvider>>() {
      };

  private static final ParameterizedTypeReference<CmdbHasManyList<CloudService>>
      CLOUD_SERVICES_LIST_RESPONSE_TYPE =
      new ParameterizedTypeReference<CmdbHasManyList<CloudService>>() {
      };

  private static final ParameterizedTypeReference<CmdbDataWrapper<CloudService>>
      CLOUD_SERVICE_RESPONSE_TYPE =
      new ParameterizedTypeReference<CmdbDataWrapper<CloudService>>() {
      };

  private static final ParameterizedTypeReference<CmdbHasManyList<Image>>
      IMAGES_LIST_RESPONSE_TYPE =
      new ParameterizedTypeReference<CmdbHasManyList<Image>>() {
      };

  private static final ParameterizedTypeReference<CmdbDataWrapper<Image>>
      IMAGE_RESPONSE_TYPE =
      new ParameterizedTypeReference<CmdbDataWrapper<Image>>() {
      };

  private static final ParameterizedTypeReference<CmdbHasManyList<Flavor>>
      FLAVORS_LIST_RESPONSE_TYPE =
      new ParameterizedTypeReference<CmdbHasManyList<Flavor>>() {
      };

  private static final ParameterizedTypeReference<CmdbDataWrapper<Flavor>>
      FLAVOR_RESPONSE_TYPE =
      new ParameterizedTypeReference<CmdbDataWrapper<Flavor>>() {
      };

  private static final ParameterizedTypeReference<CmdbHasManyList<Tenant>>
      TENANTS_LIST_RESPONSE_TYPE =
      new ParameterizedTypeReference<CmdbHasManyList<Tenant>>() {
      };

  private static final ParameterizedTypeReference<CmdbDataWrapper<Tenant>>
      TENANT_RESPONSE_TYPE =
      new ParameterizedTypeReference<CmdbDataWrapper<Tenant>>() {
      };

  private CmdbProperties cmdbProperties;

  private RestTemplate restTemplate;

  public CmdbServiceV1Impl(CmdbProperties cmdbProperties, RestTemplateBuilder restTemplateBuilder) {
    this.cmdbProperties = cmdbProperties;
    this.restTemplate = restTemplateBuilder.build();
  }

  private <T extends CmdbIdentifiable> T unwrap(CmdbDataWrapper<T> wrapped) {
    String id = wrapped.getId();
    T unwrapped = wrapped.getData();
    unwrapped.setId(id);
    return unwrapped;
  }

  private <T extends CmdbIdentifiable> T get(URI from,
      ParameterizedTypeReference<CmdbDataWrapper<T>> type) {
    return unwrap(restTemplate
        .exchange(from, HttpMethod.GET, null, type)
        .getBody());
  }

  private <T extends CmdbIdentifiable> List<T> getAll(URI from,
      ParameterizedTypeReference<CmdbHasManyList<T>> type) {
    return restTemplate
        .exchange(from, HttpMethod.GET, null, type)
        .getBody()
        .getRows()
        .stream()
        .map(CmdbRow::getDoc)
        .map(this::unwrap)
        .collect(Collectors.toList());
  }

  @Override
  public CloudProvider getProviderById(String providerId) {

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(cmdbProperties.getUrl() + cmdbProperties.getProviderByIdPath())
        .buildAndExpand(providerId)
        .normalize()
        .toUri();

    try {
      return get(requestUri, PROVIDER_RESPONSE_TYPE);
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error loading info for provider <" + providerId + "> from CMDB.", ex);
    }
  }

  @Override
  public CloudService getServiceById(String serviceId) {

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(cmdbProperties.getUrl() + cmdbProperties.getServiceByIdPath())
        .buildAndExpand(serviceId)
        .normalize()
        .toUri();

    try {
      return get(requestUri, CLOUD_SERVICE_RESPONSE_TYPE);
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error loading info for service <" + serviceId + "> from CMDB.",
          ex);
    }
  }

  @Override
  public List<CloudService> getServicesByProvider(String providerId) {

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(cmdbProperties.getUrl() + cmdbProperties.getServicesByProviderIdPath())
        .buildAndExpand(providerId)
        .normalize()
        .toUri();

    try {
      return getAll(requestUri, CLOUD_SERVICES_LIST_RESPONSE_TYPE);
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error loading services list for provider <" + providerId + "> from CMDB.", ex);
    }
  }

  @Override
  public Image getImageById(String imageId) {

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(cmdbProperties.getUrl() + cmdbProperties.getImageByIdPath())
        .buildAndExpand(imageId)
        .normalize()
        .toUri();

    try {
      return get(requestUri, IMAGE_RESPONSE_TYPE);
    } catch (RestClientException ex) {
      throw new DeploymentException("Error loading info for image <" + imageId + "> from CMDB.",
          ex);
    }
  }

  @Override
  public List<Image> getImagesByTenant(String tenantId) {

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(cmdbProperties.getUrl() + cmdbProperties.getImagesByTenantIdPath())
        .buildAndExpand(tenantId)
        .normalize()
        .toUri();

    try {
      return getAll(requestUri, IMAGES_LIST_RESPONSE_TYPE);
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error loading images list for tenant <" + tenantId + "> from CMDB.", ex);
    }
  }

  @Override
  public Flavor getFlavorById(String flavorId) {

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(cmdbProperties.getUrl() + cmdbProperties.getFlavorByIdPath())
        .buildAndExpand(flavorId)
        .normalize()
        .toUri();

    try {
      return get(requestUri, FLAVOR_RESPONSE_TYPE);
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error loading info for flavor <" + flavorId + "> from CMDB.",
          ex);
    }
  }

  @Override
  public List<Flavor> getFlavorsByTenant(String tenantId) {

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(cmdbProperties.getUrl() + cmdbProperties.getFlavorsByTenantIdPath())
        .buildAndExpand(tenantId)
        .normalize()
        .toUri();

    try {
      return getAll(requestUri, FLAVORS_LIST_RESPONSE_TYPE);
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error loading flavor list for tenant <" + tenantId + "> from CMDB.", ex);
    }
  }

  @Override
  public List<Tenant> getTenantsByService(String serviceId) {

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(cmdbProperties.getUrl() + cmdbProperties.getTenantsByServiceIdPath())
        .buildAndExpand(serviceId)
        .normalize()
        .toUri();

    try {
      return getAll(requestUri, TENANTS_LIST_RESPONSE_TYPE);
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error loading tenant list for service <" + serviceId + "> from CMDB.", ex);
    }
  }

  /*  @Override
  public List<Tenant> getTenantsByOrganisation(String organisationId) {

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(cmdbProperties.getUrl() + cmdbProperties.getTenantsByOrganizationIdPath())
        .buildAndExpand(organisationId)
        .normalize()
        .toUri();

    try {
      return getAll(requestUri, TENANTS_LIST_RESPONSE_TYPE);
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error loading tenant list for organisation <" + organisationId + "> from CMDB.", ex);
    }
  }
  */

  /**
   * Temporary hack: cmdbProperties.getTenantsByOrganizationIdPath() does not work with
   * organization names that contain slash, e.g. kube/users. Therefore, as a workaround
   * here we first get the full list of tenants and then we filter the list
   */
  @Override
  public List<Tenant> getTenantsByOrganisation(String organisationId) {

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(cmdbProperties.getUrl() + cmdbProperties.getTenantsListPath())
        .build()
        .normalize()
        .toUri();

    try {
      List<Tenant> tenants = getAll(requestUri, TENANTS_LIST_RESPONSE_TYPE);
      return tenants.stream().filter(t -> Objects.nonNull(t.getIamOrganisation())
                  && t.getIamOrganisation().equals(organisationId)).collect(Collectors.toList());
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error loading tenant list for organisation <" + organisationId + "> from CMDB.", ex);
    }
  }

  @Override
  public Tenant getTenantById(String tenantId) {

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(cmdbProperties.getUrl() + cmdbProperties.getTenantByIdPath())
        .buildAndExpand(tenantId)
        .normalize()
        .toUri();

    try {
      return get(requestUri, TENANT_RESPONSE_TYPE);
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error loading info for tenant <" + tenantId + "> from CMDB.",
          ex);
    }
  }

}
