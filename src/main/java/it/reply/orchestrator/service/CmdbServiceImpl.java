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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@PropertySource("classpath:cmdb/cmdb.properties")
public class CmdbServiceImpl implements CmdbService {

  @Autowired
  private RestTemplate restTemplate;

  @Value("${cmdb.url}")
  private String url;

  @Value("${service.id}")
  private String serviceIdUrlPath;

  @Value("${provider.id}")
  private String providerIdUrlPath;

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public CloudService getServiceById(String id) {

    ResponseEntity<CloudService> response =
        restTemplate.getForEntity(url.concat(serviceIdUrlPath).concat(id), CloudService.class);
    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody();
    }
    throw new DeploymentException("Unable to find service <" + id + "> in the CMDB."
        + response.getStatusCode().toString() + " " + response.getStatusCode().getReasonPhrase());
  }

  @Override
  public Provider getProviderById(String id) {
    ResponseEntity<Provider> response =
        restTemplate.getForEntity(url.concat(providerIdUrlPath).concat(id), Provider.class);
    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody();
    }
    throw new DeploymentException("Unable to find provider <" + id + "> in the CMDB."
        + response.getStatusCode().toString() + " " + response.getStatusCode().getReasonPhrase());
  }

  @Override
  public Image getImageById(String imageId) {
    ResponseEntity<Image> response =
        restTemplate.getForEntity(url.concat("image/id").concat(imageId), Image.class);
    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody();
    }
    throw new DeploymentException("Unable to find image <" + imageId + "> in the CMDB."
        + response.getStatusCode().toString() + " " + response.getStatusCode().getReasonPhrase());
  }

  @Override
  public List<Image> getImagesByService(String serviceId) {
    ResponseEntity<CmdbHasManyList<CmdbRow<Image>>> response = restTemplate.exchange(
        url.concat(serviceIdUrlPath).concat(serviceId).concat("/has_many/images?include_docs=true"),
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
    ResponseEntity<CmdbHasManyList<CmdbRow<CloudService>>> response = restTemplate.exchange(
        url.concat(providerIdUrlPath)
            .concat(providerId)
            .concat("/has_many/services?include_docs=true"),
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
    cp.setName(cp.getCmdbProviderData().getId());

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
