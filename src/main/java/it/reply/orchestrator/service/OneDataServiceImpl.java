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

import it.reply.orchestrator.config.properties.OneDataProperties;
import it.reply.orchestrator.config.properties.OneDataProperties.ServiceSpaceProperties;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dal.repository.OidcEntityRepository;
import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.onedata.OneData.OneDataProviderInfo;
import it.reply.orchestrator.dto.onedata.OneData.OneDataProviderInfo.OneDataProviderInfoBuilder;
import it.reply.orchestrator.dto.onedata.ProviderDetails;
import it.reply.orchestrator.dto.onedata.SpaceDetails;
import it.reply.orchestrator.dto.onedata.Token;
import it.reply.orchestrator.dto.onedata.Tokens;
import it.reply.orchestrator.dto.onedata.UserSpaces;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.security.OAuth2TokenService;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.core.UriBuilder;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@EnableConfigurationProperties(OneDataProperties.class)
public class OneDataServiceImpl implements OneDataService {

  private OneDataProperties oneDataProperties;

  private RestTemplate restTemplate;

  private OAuth2TokenService oauth2TokenService;

  private OidcEntityRepository oidcEntityRepository;

  public OneDataServiceImpl(OneDataProperties oneDataProperties,
    RestTemplateBuilder restTemplateBuilder,
    OAuth2TokenService oauth2TokenService,
    OidcEntityRepository oidcEntityRepository) {
    this.oneDataProperties = oneDataProperties;
    this.restTemplate = restTemplateBuilder.build();
    this.oauth2TokenService = oauth2TokenService;
    this.oidcEntityRepository = oidcEntityRepository;
  }

  @Override
  public UserSpaces getUserSpacesId(String oneZoneEndpoint, String oneDataToken) {

    URI requestUri = UriBuilder
      .fromUri(oneZoneEndpoint + oneDataProperties.getOnezoneBasePath() + "/user/spaces")
        .build()
        .normalize();

    try {
      return restTemplate
        .exchange(requestUri, HttpMethod.GET, withOnedataToken(oneDataToken), UserSpaces.class)
          .getBody();
    } catch (RestClientException ex) {
      throw new DeploymentException("Error retrieving OneData spaces", ex);
    }
  }

  @Override
  public SpaceDetails getSpaceDetailsFromId(String oneZoneEndpoint, String oneDataToken,
      String oneSpaceId) {

    URI requestUri = UriBuilder
      .fromUri(
        oneZoneEndpoint + oneDataProperties.getOnezoneBasePath() + "/user/spaces/{oneSpaceId}")
      .build(oneSpaceId)
      .normalize();

    try {
      return restTemplate
        .exchange(requestUri, HttpMethod.GET, withOnedataToken(oneDataToken), SpaceDetails.class)
          .getBody();
    } catch (RestClientException ex) {
      throw new DeploymentException("Error retrieving details for OneData space " + oneSpaceId, ex);
    }
  }

  @Override
  public ProviderDetails getProviderDetailsFromId(String oneZoneEndpoint, String oneDataToken,
    String oneProviderId) {

    URI requestUri = UriBuilder
      .fromUri(
        oneZoneEndpoint + oneDataProperties.getOnezoneBasePath() + "/providers/{oneProviderId}")
        .build(oneProviderId)
        .normalize();

    try {
      return restTemplate
        .exchange(requestUri, HttpMethod.GET, withOnedataToken(oneDataToken), ProviderDetails.class)
          .getBody();
    } catch (RestClientException ex) {
      throw new DeploymentException("Error retrieving details of provider " + oneProviderId, ex);
    }
  }

  @Override
  public Tokens getOneDataTokens(String oneZoneEndpoint, OidcTokenId oidcTokenId) {

    String organization = oidcEntityRepository
      .findByOidcEntityId(oidcTokenId.getOidcEntityId())
      .orElseThrow(
        () -> new DeploymentException("No user associated to deployment token found"))
      .getOrganization();

    URI requestUri = UriBuilder
      .fromUri(oneZoneEndpoint + oneDataProperties.getOnezoneBasePath() + "/user/client_tokens")
      .build()
      .normalize();

    Function<String, Tokens> request = (token) -> restTemplate
      .exchange(requestUri, HttpMethod.GET, withOidcToken(organization, token), Tokens.class)
      .getBody();
    try {
      try {
        String accessToken = this.oauth2TokenService.getAccessToken(oidcTokenId);
        return request.apply(accessToken);
      } catch (HttpClientErrorException ex) {
        if (HttpStatus.UNAUTHORIZED == ex.getStatusCode()) {
          String refreshedAccessToken = oauth2TokenService.getRefreshedAccessToken(oidcTokenId);
          return request.apply(refreshedAccessToken);
        } else {
          throw ex;
        }
      }
    } catch (RestClientException ex) {
      throw new DeploymentException("Error retrieving OneData tokens", ex);
    }
  }

  @Override
  public String generateOneDataToken(String oneZoneEndpoint, OidcTokenId oidcTokenId) {

    String organization = oidcEntityRepository
      .findByOidcEntityId(oidcTokenId.getOidcEntityId())
      .orElseThrow(
        () -> new DeploymentException("No user associated to deployment token found"))
      .getOrganization();

    URI requestUri = UriBuilder
      .fromUri(oneZoneEndpoint + oneDataProperties.getOnezoneBasePath() + "/user/client_tokens")
      .build()
      .normalize();

    Function<String, String> request = (token) -> restTemplate
      .exchange(requestUri, HttpMethod.POST, withOidcToken(organization, token), Token.class)
      .getBody()
      .getToken();

    try {
      try {
        String accessToken = this.oauth2TokenService.getAccessToken(oidcTokenId);
        return request.apply(accessToken);
      } catch (HttpClientErrorException ex) {
        if (HttpStatus.UNAUTHORIZED == ex.getStatusCode()) {
          String refreshedAccessToken = oauth2TokenService.getRefreshedAccessToken(oidcTokenId);
          return request.apply(refreshedAccessToken);
        } else {
          throw ex;
        }
      }
    } catch (RestClientException ex) {
      throw new DeploymentException("Error generating new OneData token", ex);
    }
  }

  @Override
  public String getOneDataToken(String oneZoneEndpoint, OidcTokenId oidcTokenId) {
    return getOneDataTokens(oneZoneEndpoint, oidcTokenId)
      .getTokens()
      .stream()
      .findAny()
      .orElseGet(() -> generateOneDataToken(oneZoneEndpoint, oidcTokenId));
  }

  private HttpEntity<?> withOnedataToken(String oneDataToken) {
    // TODO use a request interceptor (restTemplate must not be singleton)
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Auth-Token", oneDataToken);
    return new HttpEntity<>(headers);
  }

  private HttpEntity<?> withOidcToken(String organization, String oidcToken) {
    // TODO use a request interceptor (restTemplate must not be singleton)
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Auth-Token", organization + ":" + oidcToken);
    return new HttpEntity<>(headers);
  }

  @Override
  public OneData populateProviderInfo(OneData oneDataParameter,
    Map<String, CloudProvider> cloudProviders,
    OidcTokenId requestedWithToken,
    String deploymentId) {
    if (oneDataParameter.isServiceSpace()) {
      ServiceSpaceProperties serviceSpaceProperties = oneDataProperties.getServiceSpace();
      Optional.ofNullable(serviceSpaceProperties.getOnezoneUrl())
        .map(URI::toString)
        .ifPresent(oneDataParameter::setOnezone);
      oneDataParameter.setToken(serviceSpaceProperties.getToken());
      oneDataParameter.setSpace(serviceSpaceProperties.getName());
      oneDataParameter.setPath(serviceSpaceProperties.getBaseFolderPath() + deploymentId + "/");
    }
    String spaceName = oneDataParameter.getSpace();

    final String oneZone;
    if (oneDataParameter.getOnezone() == null) {
      oneZone = oneDataProperties.getOnezoneUrl().toString();
      oneDataParameter.setOnezone(oneZone);
    } else {
      oneZone = oneDataParameter.getOnezone();
    }

    String oneToken;
    if (oneDataParameter.getToken() != null) {
      oneToken = oneDataParameter.getToken();
    } else {
      oneToken = getOneDataToken(oneZone, requestedWithToken);
      oneDataParameter.setToken(oneToken);
    }

    Set<String> requestedProviders = oneDataParameter
      .getOneproviders()
      .stream()
      .map(OneDataProviderInfo::getEndpoint)
      .collect(Collectors.toSet());

    boolean useRequestedProviders = !requestedProviders.isEmpty();

    UserSpaces spaces = getUserSpacesId(oneZone, oneToken);

    SpaceDetails spaceDetail = spaces
      .getSpaces()
      .stream()
      .map(spaceId -> getSpaceDetailsFromId(oneZone, oneToken, spaceId))
      .filter(tmpSpaceDetail -> Objects.equals(spaceName, tmpSpaceDetail.getName()))
      .findAny()
      .orElseThrow(() -> new DeploymentException(
        "No OneData space with name " + spaceName + " could be found in oneZone " + oneZone));

    List<OneDataProviderInfo> oneDataProviderInfos = new ArrayList<>();
    Iterator<String> providerIdIterator = spaceDetail.getProviders().keySet().iterator();

    while (providerIdIterator.hasNext() && (!useRequestedProviders || !requestedProviders
      .isEmpty())) {
      String providerId = providerIdIterator.next();
      OneDataProviderInfoBuilder oneDataProviderInfobuilder = OneDataProviderInfo
        .builder()
        .id(providerId);
      boolean cloudServiceFound = false;
      for (Map.Entry<String, CloudProvider> cloudProviderEntry : cloudProviders.entrySet()) {
        String cloudProviderId = cloudProviderEntry.getKey();
        CloudProvider cloudProvider = cloudProviderEntry.getValue();
        Optional<CloudService> cloudService = cloudProvider
          .getCmdbProviderServices()
          .values()
          .stream()
          .filter(CloudService::isOneProviderStorageService)
          .filter(cs -> providerId.equals(cs.getData().getEndpoint()))
          .findAny();
        if (cloudService.isPresent()) {
          oneDataProviderInfobuilder
            .cloudProviderId(cloudProviderId)
            .cloudServiceId(cloudService.get().getId());
          cloudServiceFound = true;
          break;
        }
      }

      ProviderDetails providerDetails = getProviderDetailsFromId(oneZone, oneToken, providerId);
      String oneProviderEndpoint = providerDetails.getDomain();
      boolean isInRequestedProviders = !requestedProviders.remove(oneProviderEndpoint);

      if (!cloudServiceFound && oneDataParameter.isSmartScheduling()) {
        if (useRequestedProviders && isInRequestedProviders) {
          throw new DeploymentException("Requested OneProvider " + oneProviderEndpoint
            + "not registered in CMDB hence not eligible for smart scheduling");
        } else {
          break;
        }
      }
      oneDataProviderInfos.add(oneDataProviderInfobuilder.endpoint(oneProviderEndpoint).build());
    }
    if (oneDataProviderInfos.isEmpty()) {
      throw new DeploymentException("No OneProviders available for the space " + spaceName);
    }
    if (!requestedProviders.isEmpty()) {
      throw new DeploymentException(
        "Some requested OneProviders are not supporting the space " + spaceName + ":\n" + Arrays
          .toString(requestedProviders.toArray()));
    }
    oneDataParameter.setOneproviders(oneDataProviderInfos);
    return oneDataParameter;
  }

}
