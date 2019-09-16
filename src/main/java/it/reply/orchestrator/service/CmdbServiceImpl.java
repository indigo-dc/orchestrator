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

import it.reply.orchestrator.config.properties.CmdbProperties;
import it.reply.orchestrator.dto.cmdb.CloudProvider;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.CmdbIdentifiable;
import it.reply.orchestrator.dto.cmdb.ComputeService;
import it.reply.orchestrator.dto.cmdb.Flavor;
import it.reply.orchestrator.dto.cmdb.Image;
import it.reply.orchestrator.dto.cmdb.wrappers.CmdbDataWrapper;
import it.reply.orchestrator.dto.cmdb.wrappers.CmdbHasManyList;
import it.reply.orchestrator.dto.cmdb.wrappers.CmdbRow;
import it.reply.orchestrator.exception.service.DeploymentException;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.core.UriBuilder;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@EnableConfigurationProperties(CmdbProperties.class)
public class CmdbServiceImpl implements CmdbService {

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

  private CmdbProperties cmdbProperties;

  private RestTemplate restTemplate;

  public CmdbServiceImpl(CmdbProperties cmdbProperties, RestTemplateBuilder restTemplateBuilder) {
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

    URI requestUri = UriBuilder
        .fromUri(cmdbProperties.getUrl() + cmdbProperties.getProviderByIdPath())
        .build(providerId)
        .normalize();

    try {
      return get(requestUri, PROVIDER_RESPONSE_TYPE);
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error loading info for provider <" + providerId + "> from CMDB.", ex);
    }
  }

  @Override
  public CloudService getServiceById(String serviceId) {

    URI requestUri = UriBuilder
        .fromUri(cmdbProperties.getUrl() + cmdbProperties.getServiceByIdPath())
        .build(serviceId)
        .normalize();

    try {
      return get(requestUri, CLOUD_SERVICE_RESPONSE_TYPE);
    } catch (RestClientException ex) {
      throw new DeploymentException("Error loading info for service <" + serviceId + "> from CMDB.",
          ex);
    }
  }

  @Override
  public List<CloudService> getServicesByProvider(String providerId) {

    URI requestUri = UriBuilder
        .fromUri(cmdbProperties.getUrl() + cmdbProperties.getServicesByProviderIdPath())
        .build(providerId)
        .normalize();

    try {
      return getAll(requestUri, CLOUD_SERVICES_LIST_RESPONSE_TYPE);
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error loading services list for provider <" + providerId + "> from CMDB.", ex);
    }
  }

  @Override
  public Image getImageById(String imageId) {

    URI requestUri = UriBuilder
        .fromUri(cmdbProperties.getUrl() + cmdbProperties.getImageByIdPath())
        .build(imageId)
        .normalize();

    try {
      return get(requestUri, IMAGE_RESPONSE_TYPE);
    } catch (RestClientException ex) {
      throw new DeploymentException("Error loading info for image <" + imageId + "> from CMDB.",
          ex);
    }
  }

  @Override
  public List<Image> getImagesByService(String serviceId) {

    URI requestUri = UriBuilder
        .fromUri(cmdbProperties.getUrl() + cmdbProperties.getImagesByServiceIdPath())
        .build(serviceId)
        .normalize();

    try {
      return getAll(requestUri, IMAGES_LIST_RESPONSE_TYPE);
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error loading images list for service <" + serviceId + "> from CMDB.", ex);
    }
  }

  @Override
  public Flavor getFlavorById(String flavorId) {

    URI requestUri = UriBuilder
        .fromUri(cmdbProperties.getUrl() + cmdbProperties.getFlavorByIdPath())
        .build(flavorId)
        .normalize();

    try {
      return get(requestUri, FLAVOR_RESPONSE_TYPE);
    } catch (RestClientException ex) {
      throw new DeploymentException("Error loading info for flavor <" + flavorId + "> from CMDB.",
          ex);
    }
  }

  @Override
  public List<Flavor> getFlavorsByService(String serviceId) {

    URI requestUri = UriBuilder
        .fromUri(cmdbProperties.getUrl() + cmdbProperties.getFlavorsByServiceIdPath())
        .build(serviceId)
        .normalize();

    try {
      return getAll(requestUri, FLAVORS_LIST_RESPONSE_TYPE);
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error loading flavor list for service <" + serviceId + "> from CMDB.", ex);
    }
  }

  @Override
  public CloudProvider fillCloudProviderInfo(String providerId, Set<String> servicesWithSla) {
    // Get provider's data
    CloudProvider provider = getProviderById(providerId);

    Map<String, CloudService> services = getServicesByProvider(providerId)
        .stream()
        .filter(cs -> !(cs instanceof ComputeService) || servicesWithSla.contains(cs.getId()))
        .map(this::fillComputeServiceData)
        .collect(Collectors.toMap(CloudService::getId, Function.identity()));

    provider.setServices(services);
    return provider;
  }

  private CloudService fillComputeServiceData(CloudService cloudService) {
    if (cloudService instanceof ComputeService) {
      String providerId = cloudService.getProviderId();
      String serviceId = cloudService.getId();
      ComputeService computeService = (ComputeService) cloudService;
      List<Image> imageList = getImagesByService(serviceId);
      LOG.debug("Image list for service <{}> of provider <{}>: <{}>",
          Arrays.toString(imageList.toArray()), serviceId, providerId);
      computeService.setImages(imageList);
      List<Flavor> flavorList = getFlavorsByService(serviceId);
      LOG.debug("Flavor list for service <{}> of provider <{}>: <{}>",
          Arrays.toString(flavorList.toArray()), serviceId, providerId);
      computeService.setFlavors(flavorList);
    }
    return cloudService;
  }
}
