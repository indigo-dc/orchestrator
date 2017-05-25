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

import com.google.common.collect.Lists;

import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.onedata.OneData.OneDataProviderInfo;
import it.reply.orchestrator.dto.onedata.ProviderDetails;
import it.reply.orchestrator.dto.onedata.SpaceDetails;
import it.reply.orchestrator.dto.onedata.UserSpaces;
import it.reply.orchestrator.exception.service.DeploymentException;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;

import javax.annotation.PostConstruct;

@Service
@PropertySource(value = { "classpath:application.properties", "${conf-file-path.onedata}" })
public class OneDataServiceImpl implements OneDataService {

  @Autowired
  private RestTemplate restTemplate;

  @Value("${onezone.default.url}")
  private String defaultOneZoneEndpoint;

  @Value("${onezone.base.rest.path}")
  private String oneZoneBaseRestPath;

  @Value("${oneprovider.base.rest.path}")
  private String oneProviderBaseRestPath;

  @Value("${service.space.token}")
  private String serviceSpaceToken;

  @Value("${service.space.name}")
  private String serviceSpaceName;

  @Value("${service.space.provider}")
  private String serviceSpaceProvider;

  @Value("${service.space.path:''}")
  private String serviceSpacePath;

  @PostConstruct
  private void init() {
    defaultOneZoneEndpoint = addTrailingSlash(defaultOneZoneEndpoint);

    if (oneZoneBaseRestPath.startsWith("/")) {
      oneZoneBaseRestPath = oneZoneBaseRestPath.substring(1);
    }
    oneZoneBaseRestPath = addTrailingSlash(oneZoneBaseRestPath);

    if (oneProviderBaseRestPath.startsWith("/")) {
      oneProviderBaseRestPath = oneProviderBaseRestPath.substring(1);
    }
    oneProviderBaseRestPath = addTrailingSlash(oneProviderBaseRestPath);

  }

  private String addTrailingSlash(String endpoint) {
    if (!endpoint.endsWith("/")) {
      endpoint += "/";
    }
    return endpoint;
  }

  @Override
  public String getServiceSpaceToken() {
    return serviceSpaceToken;
  }

  @Override
  public String getServiceSpaceName() {
    return serviceSpaceName;
  }

  @Override
  public String getServiceSpaceProvider() {
    return serviceSpaceProvider;
  }

  @Override
  public String getServiceSpacePath() {
    return serviceSpacePath;
  }

  @Override
  public UserSpaces getUserSpacesId(String oneZoneEndpoint, String oneDataToken) {
    if (oneZoneEndpoint == null) {
      oneZoneEndpoint = defaultOneZoneEndpoint;
    }
    oneZoneEndpoint = addTrailingSlash(oneZoneEndpoint) + oneZoneBaseRestPath + "user/spaces";

    ResponseEntity<UserSpaces> response =
        getForEntity(restTemplate, oneZoneEndpoint, oneDataToken, UserSpaces.class);
    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody();
    }
    throw new DeploymentException("Unable to get OneData spaces. "
        + response.getStatusCode().toString() + " " + response.getStatusCode().getReasonPhrase());
  }

  @Override
  public UserSpaces getUserSpacesId(String onedataToken) {
    return getUserSpacesId(null, onedataToken);
  }

  @Override
  public SpaceDetails getSpaceDetailsFromId(String oneZoneEndpoint, String oneDataToken,
      String oneSpaceId) {
    if (oneZoneEndpoint == null) {
      oneZoneEndpoint = defaultOneZoneEndpoint;
    }
    oneZoneEndpoint =
        addTrailingSlash(oneZoneEndpoint) + oneZoneBaseRestPath + "user/spaces/" + oneSpaceId;

    ResponseEntity<SpaceDetails> response =
        getForEntity(restTemplate, oneZoneEndpoint, oneDataToken, SpaceDetails.class);
    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody();
    }
    throw new DeploymentException("Unable to OneData space details. "
        + response.getStatusCode().toString() + " " + response.getStatusCode().getReasonPhrase());
  }

  @Override
  public SpaceDetails getSpaceDetailsFromId(String oneDataToken, String oneSpaceId) {
    return getSpaceDetailsFromId(null, oneDataToken, oneSpaceId);
  }

  @Override
  public String getUserSpaceNameById(String oneZoneEndpoint, String oneDataToken,
      String oneSpaceId) {
    SpaceDetails details = getSpaceDetailsFromId(oneZoneEndpoint, oneDataToken, oneSpaceId);
    return details.getName();
  }

  @Override
  public String getUserSpaceNameById(String onedataToken, String oneSpaceId) {
    return getUserSpaceNameById(null, onedataToken, oneSpaceId);
  }

  @Override
  public List<String> getProvidersIdBySpaceId(String oneZoneEndpoint, String oneDataToken,
      String oneSpaceId) {
    SpaceDetails details = getSpaceDetailsFromId(oneZoneEndpoint, oneDataToken, oneSpaceId);
    return Lists.newArrayList(details.getProvidersSupports().keySet());
  }

  @Override
  public List<String> getProvidersIdBySpaceId(String oneDataToken, String oneSpaceId) {
    return getProvidersIdBySpaceId(null, oneDataToken, oneSpaceId);
  }

  @Override
  public ProviderDetails getProviderDetailsFromId(String oneZoneEndpoint, String oneDataToken,
      String oneSpaceId, String oneProviderId) {
    if (oneZoneEndpoint == null) {
      oneZoneEndpoint = defaultOneZoneEndpoint;
    }
    oneZoneEndpoint = String.format("%s%sspaces/%s/providers/%s", addTrailingSlash(oneZoneEndpoint),
        oneZoneBaseRestPath, oneSpaceId, oneProviderId);

    ResponseEntity<ProviderDetails> response =
        getForEntity(restTemplate, oneZoneEndpoint, oneDataToken, ProviderDetails.class);
    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody();
    }
    throw new DeploymentException("Unable to OneData provider details. "
        + response.getStatusCode().toString() + " " + response.getStatusCode().getReasonPhrase());
  }

  @Override
  public ProviderDetails getProviderDetailsFromId(String oneDataToken, String oneSpaceId,
      String oneProviderId) {
    return getProviderDetailsFromId(null, oneDataToken, oneSpaceId, oneProviderId);
  }

  private <T> ResponseEntity<T> getForEntity(RestTemplate restTemplate, String endpoint,
      String oneDataToken, Class<T> entityClass) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("macaroon", oneDataToken);

    HttpEntity<?> entity = new HttpEntity<>(headers);
    return restTemplate.exchange(endpoint, HttpMethod.GET, entity, entityClass);
  }

  // private boolean sameUrl(String lhs, String rhs) {
  // if (lhs == null && rhs == null) {
  // return true;
  // } else if (lhs != null && rhs != null) {
  // URI lhsUri = URI.create(addTrailingSlash(lhs));
  // URI rhsUri = URI.create(addTrailingSlash(rhs));
  // if (!Objects.equal(lhsUri.getHost(), rhsUri.getHost())) {
  // return false;
  // } else if (!Objects.equal(lhsUri.getPath(), rhsUri.getPath())) {
  // return false;
  // } else {
  // if (lhsUri.getScheme().equals("http") && rhsUri.getScheme().equals("http")) {
  // if ((lhsUri.getPort() == -1 || lhsUri.getPort() == 80) && (rhsUri.getPort() == -1 ||
  // rhsUri.getPort() == 80)) {
  // return true;
  // }
  // } else if (lhsUri.getScheme().equals("https") && rhsUri.getScheme().equals("https")) {
  // if ((lhsUri.getPort() == -1 || lhsUri.getPort() == 443) && (rhsUri.getPort() == -1 ||
  // rhsUri.getPort() == 443)) {
  // return true;
  // }
  // }
  // return Objects.equal(lhsUri.getScheme(), rhsUri.getScheme()) && lhsUri.getPort() ==
  // rhsUri.getPort();
  // }
  // } else {
  // return false;
  // }
  // }

  @Override
  public OneData populateProviderInfo(OneData onedataParameter) {
    boolean addAllProvidersinfo = false;
    if (CollectionUtils.isEmpty(onedataParameter.getProviders())) {
      addAllProvidersinfo = true;
    } else {
      // FIXME remove once all the logic has been implemented
      return onedataParameter;
    }

    UserSpaces spaces = getUserSpacesId(onedataParameter.getZone(), onedataParameter.getToken());
    List<String> providersId = Lists.newArrayList();
    SpaceDetails spaceDetail = null;
    for (String spaceId : spaces.getSpaces()) {
      spaceDetail =
          getSpaceDetailsFromId(onedataParameter.getZone(), onedataParameter.getToken(), spaceId);
      if (Objects.equals(onedataParameter.getSpace(), spaceDetail.getCanonicalName())) {
        providersId.addAll(spaceDetail.getProvidersSupports().keySet());
        break;
      }
    }
    if (spaceDetail == null) {
      throw new DeploymentException(String.format("Could not found space %s in onezone %s",
          onedataParameter.getSpace(), onedataParameter.getZone() != null
              ? onedataParameter.getZone() : defaultOneZoneEndpoint));
    }

    for (String providerId : providersId) {
      ProviderDetails providerDetails = getProviderDetailsFromId(onedataParameter.getZone(),
          onedataParameter.getToken(), spaceDetail.getSpaceId(), providerId);
      if (addAllProvidersinfo) {
        OneDataProviderInfo providerInfo = OneDataProviderInfo.builder()
            .id(providerId)
            .endpoint(providerDetails.getRedirectionPoint())
            .build();
        onedataParameter.getProviders().add(providerInfo);
      } else {
        // TODO implement the logic
      }
    }
    return onedataParameter;
  }

}
