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

package it.reply.orchestrator.service;

import it.reply.orchestrator.config.properties.CmdbProperties;
import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.CmdbHasManyList;
import it.reply.orchestrator.dto.cmdb.CmdbRow;
import it.reply.orchestrator.dto.cmdb.Image;
import it.reply.orchestrator.dto.cmdb.ImageData;
import it.reply.orchestrator.dto.cmdb.Provider;
import it.reply.orchestrator.dto.cmdb.Type;
import it.reply.orchestrator.exception.service.DeploymentException;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

  private static final ParameterizedTypeReference<CmdbHasManyList<CmdbRow<CloudService>>> 
      CLOUD_SERVICE_LIST_RESPONSE_TYPE = 
          new ParameterizedTypeReference<CmdbHasManyList<CmdbRow<CloudService>>>() {};

  private static final ParameterizedTypeReference<CmdbHasManyList<CmdbRow<Image>>>
      IMAGE_LIST_RESPONSE_TYPE =
          new ParameterizedTypeReference<CmdbHasManyList<CmdbRow<Image>>>() {};

  private CmdbProperties cmdbProperties;

  private RestTemplate restTemplate;

  public CmdbServiceImpl(CmdbProperties cmdbProperties, RestTemplateBuilder restTemplateBuilder) {
    this.cmdbProperties = cmdbProperties;
    this.restTemplate = restTemplateBuilder.build();
  }

  @Override
  public CloudService getServiceById(String serviceId) {

    URI requestUri = UriBuilder
        .fromUri(cmdbProperties.getUrl() + cmdbProperties.getServiceByIdPath())
        .build(serviceId)
        .normalize();

    try {
      return restTemplate.getForEntity(requestUri, CloudService.class).getBody();
    } catch (RestClientException ex) {
      throw new DeploymentException("Error loading info for service <" + serviceId + "> from CMDB.",
          ex);
    }
  }

  @Override
  public Provider getProviderById(String providerId) {

    URI requestUri = UriBuilder
        .fromUri(cmdbProperties.getUrl() + cmdbProperties.getProviderByIdPath())
        .build(providerId)
        .normalize();

    try {
      return restTemplate.getForEntity(requestUri, Provider.class).getBody();
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error loading info for provider <" + providerId + "> from CMDB.", ex);
    }
  }

  @Override
  public Image getImageById(String imageId) {

    URI requestUri = UriBuilder
        .fromUri(cmdbProperties.getUrl() + cmdbProperties.getImageByIdPath())
        .build(imageId)
        .normalize();

    try {
      return restTemplate.getForEntity(requestUri, Image.class).getBody();
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
      return restTemplate
          .exchange(requestUri, HttpMethod.GET, null, IMAGE_LIST_RESPONSE_TYPE)
          .getBody()
          .getRows()
          .stream()
          .map(CmdbRow::getDoc)
          .collect(Collectors.toList());
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error loading images list for service <" + serviceId + "> from CMDB.", ex);
    }
  }

  @Override
  public List<CloudService> getServicesByProvider(String providerId) {

    URI requestUri = UriBuilder
        .fromUri(cmdbProperties.getUrl() + cmdbProperties.getServicesByProviderIdPath())
        .build(providerId)
        .normalize();

    try {
      return restTemplate
          .exchange(requestUri, HttpMethod.GET, null, CLOUD_SERVICE_LIST_RESPONSE_TYPE)
          .getBody()
          .getRows()
          .stream()
          .map(CmdbRow::getDoc)
          .collect(Collectors.toList());
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error loading services list for provider <" + providerId + "> from CMDB.", ex);
    }
  }

  @Override
  public CloudProvider fillCloudProviderInfo(CloudProvider cp) {
    // Get provider's data
    cp.setCmdbProviderData(getProviderById(cp.getId()));

    Map<String, CloudService> allServices = getServicesByProvider(cp.getId())
        .stream()
        .collect(Collectors.toMap(CloudService::getId, Function.identity()));
    // Get provider's services' data
    cp
        .getCmdbProviderServices()
        .replaceAll((serviceId, service) -> Optional
            .ofNullable(allServices.get(serviceId))
            .orElseGet(() -> getServiceById(serviceId)));

    // put the oneData provider services, even if they were not present (because no SLA was
    // associated)
    allServices
        .values()
        .stream()
        .filter(CloudService::isOneProviderStorageService)
        .forEach(oneDataService -> cp
            .getCmdbProviderServices()
            .put(oneDataService.getId(), oneDataService));

    // Get images for compute services
    cp
        .getCmbdProviderServicesByType(Type.COMPUTE)
        .stream()
        .filter(Objects::nonNull)
        .forEach(computeService -> {
          List<ImageData> imageList = getImagesByService(computeService.getId())
              .stream()
              .map(e -> e.getData())
              .collect(Collectors.toList());
          LOG.debug("Image list for service <{}> of provider <{}>: <{}>",
              Arrays.toString(imageList.toArray()), computeService.getId(), cp.getId());
          cp.addCmdbCloudServiceImages(computeService.getId(), imageList);
        });

    return cp;
  }

}
