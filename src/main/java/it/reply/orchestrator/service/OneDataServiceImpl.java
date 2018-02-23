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

import com.google.common.collect.Lists;

import it.reply.orchestrator.config.properties.OneDataProperties;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.onedata.OneData.OneDataProviderInfo;
import it.reply.orchestrator.dto.onedata.ProviderDetails;
import it.reply.orchestrator.dto.onedata.SpaceDetails;
import it.reply.orchestrator.dto.onedata.UserSpaces;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.utils.CommonUtils;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.collections4.CollectionUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@EnableConfigurationProperties(OneDataProperties.class)
public class OneDataServiceImpl implements OneDataService {

  private OneDataProperties oneDataProperties;

  private RestTemplate restTemplate;

  public OneDataServiceImpl(OneDataProperties oneDataProperties,
      RestTemplateBuilder restTemplateBuilder) {
    this.oneDataProperties = oneDataProperties;
    this.restTemplate = restTemplateBuilder.build();
  }

  @Override
  public UserSpaces getUserSpacesId(@Nullable String oneZoneEndpoint, String oneDataToken) {

    URI requestUri = UriBuilder
        .fromUri(getOneZoneBaseEndpoint(oneZoneEndpoint) + "/user/spaces")
        .build()
        .normalize();

    try {
      return restTemplate
          .exchange(requestUri, HttpMethod.GET, withToken(oneDataToken), UserSpaces.class)
          .getBody();
    } catch (RestClientException ex) {
      throw new DeploymentException("Error retrieving OneData spaces", ex);
    }
  }

  @Override
  public UserSpaces getUserSpacesId(String onedataToken) {
    return getUserSpacesId(null, onedataToken);
  }

  @Override
  public SpaceDetails getSpaceDetailsFromId(@Nullable String oneZoneEndpoint, String oneDataToken,
      String oneSpaceId) {

    URI requestUri = UriBuilder
        .fromUri(getOneZoneBaseEndpoint(oneZoneEndpoint) + "/user/spaces/{oneSpaceId}")
        .build(oneSpaceId)
        .normalize();

    try {
      return restTemplate
          .exchange(requestUri, HttpMethod.GET, withToken(oneDataToken), SpaceDetails.class)
          .getBody();
    } catch (RestClientException ex) {
      throw new DeploymentException("Error retrieving details for OneData space " + oneSpaceId, ex);
    }
  }

  @Override
  public SpaceDetails getSpaceDetailsFromId(String oneDataToken, String oneSpaceId) {
    return getSpaceDetailsFromId(null, oneDataToken, oneSpaceId);
  }

  @Override
  public ProviderDetails getProviderDetailsFromId(@Nullable String oneZoneEndpoint,
      String oneDataToken, String oneSpaceId, String oneProviderId) {

    URI requestUri = UriBuilder
        .fromUri(getOneZoneBaseEndpoint(oneZoneEndpoint)
            + "/spaces/{oneSpaceId}/providers/{oneProviderId}")
        .build(oneSpaceId, oneProviderId)
        .normalize();

    try {
      return restTemplate
          .exchange(requestUri, HttpMethod.GET, withToken(oneDataToken), ProviderDetails.class)
          .getBody();
    } catch (RestClientException ex) {
      throw new DeploymentException("Error retrieving details of OneData space " + oneSpaceId
          + " on provider " + oneProviderId, ex);
    }
  }

  @Override
  public ProviderDetails getProviderDetailsFromId(String oneDataToken, String oneSpaceId,
      String oneProviderId) {
    return getProviderDetailsFromId(null, oneDataToken, oneSpaceId, oneProviderId);
  }

  private String getOneZoneBaseEndpoint(@Nullable String oneZoneEndpoint) {
    return CommonUtils
        .notNullOrDefaultValue(oneZoneEndpoint, () -> oneDataProperties.getOnezoneUrl().toString())
        .concat(oneDataProperties.getOnezoneBasePath());
  }

  private HttpEntity<?> withToken(String oneDataToken) {
    // TODO use a request interceptor (restTemplate must not be singleton)
    HttpHeaders headers = new HttpHeaders();
    headers.set("macaroon", oneDataToken);
    return new HttpEntity<>(headers);
  }

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
      SpaceDetails tmpSpaceDetail =
          getSpaceDetailsFromId(onedataParameter.getZone(), onedataParameter.getToken(), spaceId);
      if (Objects.equals(onedataParameter.getSpace(), tmpSpaceDetail.getCanonicalName())) {
        spaceDetail = tmpSpaceDetail;
        providersId.addAll(spaceDetail.getProvidersSupports().keySet());
        break;
      }
    }
    if (spaceDetail == null) {
      throw new DeploymentException(
          String.format("No space with name %s could be found in onezone %s",
              onedataParameter.getSpace(), onedataParameter.getZone() != null
                  ? onedataParameter.getZone() : oneDataProperties.getOnezoneBasePath()));
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
