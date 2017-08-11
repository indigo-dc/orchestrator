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

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.core.UriBuilder;

@Service
@Slf4j
@EnableConfigurationProperties(CmdbProperties.class)
public class CmdbServiceImpl implements CmdbService {

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private CmdbProperties cmdbProperties;

  @Override
  public CloudService getServiceById(String serviceId) {

    URI requestUri = UriBuilder
        .fromUri(cmdbProperties.getUrl() + cmdbProperties.getServiceByIdPath())
        .build(serviceId)
        .normalize();

    ResponseEntity<CloudService> response =
        restTemplate.getForEntity(requestUri, CloudService.class);
    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody();
    }
    throw new DeploymentException("Unable to find service <" + serviceId + "> in the CMDB."
        + response.getStatusCode().toString() + " " + response.getStatusCode().getReasonPhrase());
  }

  @Override
  public Provider getProviderById(String providerId) {

    URI requestUri = UriBuilder
        .fromUri(cmdbProperties.getUrl() + cmdbProperties.getProviderByIdPath())
        .build(providerId)
        .normalize();

    ResponseEntity<Provider> response =
        restTemplate.getForEntity(requestUri, Provider.class);
    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody();
    }
    throw new DeploymentException("Unable to find provider <" + providerId + "> in the CMDB."
        + response.getStatusCode().toString() + " " + response.getStatusCode().getReasonPhrase());
  }

  @Override
  public Image getImageById(String imageId) {

    URI requestUri = UriBuilder
        .fromUri(cmdbProperties.getUrl() + cmdbProperties.getImageByIdPath())
        .build(imageId)
        .normalize();

    ResponseEntity<Image> response =
        restTemplate.getForEntity(requestUri, Image.class);
    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody();
    }
    throw new DeploymentException("Unable to find image <" + imageId + "> in the CMDB."
        + response.getStatusCode().toString() + " " + response.getStatusCode().getReasonPhrase());
  }

  @Override
  public List<Image> getImagesByService(String serviceId) {

    URI requestUri = UriBuilder
        .fromUri(cmdbProperties.getUrl() + cmdbProperties.getImagesByServiceIdPath())
        .build(serviceId)
        .normalize();

    ResponseEntity<CmdbHasManyList<CmdbRow<Image>>> response = restTemplate.exchange(
        requestUri,
        HttpMethod.GET, null, new ParameterizedTypeReference<CmdbHasManyList<CmdbRow<Image>>>() {
        });

    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody()
          .getRows()
          .stream()
          .map(e -> e.getDoc())
          .collect(Collectors.toList());
    }
    throw new DeploymentException("Unable to find images for service <" + serviceId
        + "> in the CMDB." + response.getStatusCode().toString() + " "
        + response.getStatusCode().getReasonPhrase());
  }

  @Override
  public List<CloudService> getServicesByProvider(String providerId) {

    URI requestUri = UriBuilder
        .fromUri(cmdbProperties.getUrl() + cmdbProperties.getServicesByProviderIdPath())
        .build(providerId)
        .normalize();

    ResponseEntity<CmdbHasManyList<CmdbRow<CloudService>>> response = restTemplate.exchange(
        requestUri,
        HttpMethod.GET, null,
        new ParameterizedTypeReference<CmdbHasManyList<CmdbRow<CloudService>>>() {
        });

    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody()
          .getRows()
          .stream()
          .map(e -> e.getDoc())
          .collect(Collectors.toList());
    }
    throw new DeploymentException("Unable to find services for provider <" + providerId
        + "> in the CMDB." + response.getStatusCode().toString() + " "
        + response.getStatusCode().getReasonPhrase());
  }

  @Override
  public CloudProvider fillCloudProviderInfo(CloudProvider cp) {
    // Get provider's data
    cp.setCmdbProviderData(getProviderById(cp.getId()));

    Map<String, CloudService> allServices = getServicesByProvider(cp.getId()).stream()
        .collect(Collectors.toMap(CloudService::getId, Function.identity()));
    // Get provider's services' data
    cp.getCmdbProviderServices().replaceAll((serviceId, service) -> Optional
        .ofNullable(allServices.get(serviceId)).orElseGet(() -> getServiceById(serviceId)));

    // put the oneData provider services, even if they were not present (because no SLA was
    // associated)
    allServices.values().stream().filter(CloudService::isOneProviderStorageService).forEach(
        oneDataService -> cp.getCmdbProviderServices().put(oneDataService.getId(), oneDataService));

    // Get images for compute services
    cp.getCmbdProviderServicesByType(Type.COMPUTE)
        .stream()
        .filter(Objects::nonNull)
        .forEach(computeService -> {
          List<ImageData> imageList = getImagesByService(computeService.getId()).stream()
              .map(e -> e.getData())
              .collect(Collectors.toList());
          LOG.debug("Image list for service <{}> of provider <{}>: <{}>",
              Arrays.toString(imageList.toArray()), computeService.getId(), cp.getId());
          cp.addCmdbCloudServiceImages(computeService.getId(), imageList);
        });

    return cp;
  }

}
