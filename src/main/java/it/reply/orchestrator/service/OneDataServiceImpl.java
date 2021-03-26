/*
 * Copyright © 2015-2021 I.N.F.N.
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

import it.reply.orchestrator.config.properties.OneDataProperties;
import it.reply.orchestrator.config.properties.OneDataProperties.ServiceSpaceProperties;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dto.cmdb.CloudProvider;
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

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@EnableConfigurationProperties(OneDataProperties.class)
public class OneDataServiceImpl implements OneDataService {

  private OneDataProperties oneDataProperties;

  private RestTemplate restTemplate;

  private OAuth2TokenService oauth2TokenService;

  /**
   * Generate a new {@link OneDataServiceImpl}.
   *
   * @param oneDataProperties
   *     the oneDataProperties
   * @param restTemplateBuilder
   *     the restTemplateBuilder
   * @param oauth2TokenService
   *     the oauth2TokenService
   */
  public OneDataServiceImpl(OneDataProperties oneDataProperties,
      RestTemplateBuilder restTemplateBuilder,
      OAuth2TokenService oauth2TokenService) {
    this.oneDataProperties = oneDataProperties;
    this.oauth2TokenService = oauth2TokenService;
    this.restTemplate = restTemplateBuilder.build();
  }

  @Override
  public UserSpaces getUserSpacesId(String oneZoneEndpoint, String oneDataToken) {

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(oneZoneEndpoint + oneDataProperties.getOnezoneBasePath() + "/user/spaces")
        .build()
        .normalize()
        .toUri();

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

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(
            oneZoneEndpoint + oneDataProperties.getOnezoneBasePath() + "/user/spaces/{oneSpaceId}")
        .buildAndExpand(oneSpaceId)
        .normalize()
        .toUri();

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

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(
            oneZoneEndpoint + oneDataProperties.getOnezoneBasePath() + "/providers/{oneProviderId}")
        .buildAndExpand(oneProviderId)
        .normalize()
        .toUri();

    try {
      return restTemplate
          .exchange(requestUri, HttpMethod.GET, withOnedataToken(oneDataToken),
              ProviderDetails.class)
          .getBody();
    } catch (RestClientException ex) {
      throw new DeploymentException("Error retrieving details of provider " + oneProviderId, ex);
    }
  }

  @Override
  public Tokens getOneDataTokens(String oneZoneEndpoint, OidcTokenId oidcTokenId) {

    String organization = oauth2TokenService.getOrganization(oidcTokenId);

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(
            oneZoneEndpoint + oneDataProperties.getOnezoneBasePath() + "/user/client_tokens")
        .build()
        .normalize()
        .toUri();

    try {
      return oauth2TokenService.executeWithClientForResult(oidcTokenId,
          token -> restTemplate.exchange(
              requestUri, HttpMethod.GET, withOidcToken(organization, token), Tokens.class)
              .getBody(),
          OAuth2TokenService.restTemplateTokenRefreshEvaluator);
    } catch (RestClientException ex) {
      throw new DeploymentException("Error retrieving OneData tokens", ex);
    }
  }

  @Override
  public Token generateOneDataToken(String oneZoneEndpoint, OidcTokenId oidcTokenId) {

    String organization = oauth2TokenService.getOrganization(oidcTokenId);

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(
            oneZoneEndpoint + oneDataProperties.getOnezoneBasePath() + "/user/client_tokens")
        .build()
        .normalize()
        .toUri();

    try {
      return oauth2TokenService.executeWithClientForResult(oidcTokenId,
          token -> restTemplate.exchange(
              requestUri, HttpMethod.POST, withOidcToken(organization, token), Token.class)
              .getBody(),
          OAuth2TokenService.restTemplateTokenRefreshEvaluator);
    } catch (RestClientException ex) {
      throw new DeploymentException("Error generating new OneData token", ex);
    }
  }

  @Override
  public String getOrGenerateOneDataToken(String oneZoneEndpoint, OidcTokenId oidcTokenId) {
    return getOneDataTokens(oneZoneEndpoint, oidcTokenId)
        .getTokens()
        .stream()
        .findAny()
        .orElseGet(() -> generateOneDataToken(oneZoneEndpoint, oidcTokenId).getToken());
  }

  private HttpEntity<?> withOnedataToken(String oneDataToken) {
    // TODO use a request interceptor (restTemplate must not be singleton)
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Auth-Token", oneDataToken);
    return new HttpEntity<>(headers);
  }

  private HttpEntity<?> withOidcToken(String organization, String oidcToken) {
    Objects.requireNonNull(oidcToken, "OIDC must be enabled");
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
      oneToken = getOrGenerateOneDataToken(oneZone, requestedWithToken);
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

    Map<String, CloudService> oneProviderCloudServices = cloudProviders
        .values()
        .stream()
        .flatMap(provider -> provider.getServices().values().stream())
        .filter(CloudService::isOneProviderStorageService)
        .collect(Collectors.toMap(CloudService::getEndpoint, Function.identity()));

    while (providerIdIterator.hasNext() && (!useRequestedProviders || !requestedProviders
        .isEmpty())) {
      String providerId = providerIdIterator.next();
      OneDataProviderInfoBuilder oneDataProviderInfobuilder = OneDataProviderInfo
          .builder()
          .id(providerId);

      CloudService cloudService = oneProviderCloudServices.get(providerId);
      boolean cloudServiceFound = cloudService != null;
      if (cloudServiceFound) {
        oneDataProviderInfobuilder
            .cloudProviderId(cloudService.getProviderId())
            .cloudServiceId(cloudService.getId());
      }

      ProviderDetails providerDetails = getProviderDetailsFromId(oneZone, oneToken, providerId);
      String oneProviderEndpoint = providerDetails.getDomain();
      boolean isInRequestedProviders = requestedProviders.remove(oneProviderEndpoint);

      if (useRequestedProviders && !isInRequestedProviders) {
        continue;
      }

      if (!cloudServiceFound && oneDataParameter.isSmartScheduling()) {
        if (useRequestedProviders) {
          throw new DeploymentException("Requested OneProvider " + oneProviderEndpoint
              + " not registered in CMDB hence not eligible for smart scheduling");
        } else {
          continue;
        }
      }
      oneDataProviderInfos.add(oneDataProviderInfobuilder.endpoint(oneProviderEndpoint).build());
    }
    if (!requestedProviders.isEmpty()) {
      throw new DeploymentException(
          "These requested OneProviders are not supporting the space " + spaceName + ":\n" + Arrays
              .toString(requestedProviders.toArray()));
    }
    if (oneDataProviderInfos.isEmpty()) {
      throw new DeploymentException("No OneProviders available for the space " + spaceName);
    }
    oneDataParameter.setOneproviders(oneDataProviderInfos);
    return oneDataParameter;
  }

}
