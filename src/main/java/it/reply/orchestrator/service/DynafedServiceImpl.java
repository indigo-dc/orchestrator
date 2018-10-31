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

import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.Type;
import it.reply.orchestrator.dto.dynafed.Dynafed;
import it.reply.orchestrator.dto.dynafed.Dynafed.Resource;
import it.reply.orchestrator.dto.dynafed.Metalink;
import it.reply.orchestrator.dto.dynafed.Metalink.File;
import it.reply.orchestrator.dto.dynafed.Metalink.Url;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.security.OAuth2TokenService;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.core.UriBuilder;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.RequestEntity.HeadersBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class DynafedServiceImpl {

  private RestTemplate restTemplate;

  private OAuth2TokenService oauth2TokenService;

  /**
   * Generate a new {@link DynafedServiceImpl}.
   *
   * @param restTemplateBuilder
   *     the restTemplateBuilder
   * @param oauth2TokenService
   *     the oauth2TokenService
   */
  public DynafedServiceImpl(OAuth2TokenService oauth2TokenService,
      RestTemplateBuilder restTemplateBuilder) {
    this.oauth2TokenService = oauth2TokenService;
    this.restTemplate = restTemplateBuilder.build();
  }

  /**
   * Populate a {@link Dynafed} requirement.
   *
   * @param dynafed
   *     the requirement to populate
   * @param cloudProviders
   *     the available cloud providers
   * @param requestedWithToken
   *     the token ID
   * @return the requirement populated
   */
  public Dynafed populateDyanfedData(Dynafed dynafed,
      Map<String, CloudProvider> cloudProviders,
      OidcTokenId requestedWithToken) {

    Map<String, CloudService> storageServices = cloudProviders
        .values()
        .stream()
        .flatMap(cloudProvider -> cloudProvider
            .getCmdbProviderServices()
            .values()
            .stream()
            .filter(
                cloudService -> Type.STORAGE == cloudService.getData().getType() && !cloudService
                    .isOneProviderStorageService()))
        .collect(Collectors
            .toMap(cs -> cs.getData().getHostname(), cs -> cs));

    dynafed
        .getFiles()
        .forEach(file -> {
          URI requestUri = UriBuilder
              .fromUri(file.getEndpoint() + "?metalink")
              .build()
              .normalize();

          Metalink metalink;
          try {
            metalink = oauth2TokenService.executeWithClientForResult(requestedWithToken,
                accessToken -> {
                  HeadersBuilder<?> requestBuilder = RequestEntity.get(requestUri);
                  if (accessToken != null) {
                    requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
                  }
                  return restTemplate.exchange(requestBuilder.build(), Metalink.class);
                }, OAuth2TokenService.restTemplateTokenRefreshEvaluator).getBody();
          } catch (RestClientException ex) {
            throw new DeploymentException("Error retrieving metalink of file " + file.getEndpoint(),
                ex);
          }
          File metalinkFile = metalink.getFiles().get(0);
          file.setSize(metalinkFile.getSize());

          List<Resource> resources = metalinkFile
              .getUrls()
              .stream()
              .map(Url::getValue)
              .filter(url -> storageServices.containsKey(url.getHost()))
              .map(url -> {
                CloudService storageService = storageServices.get(url.getHost());
                return Dynafed.Resource
                    .builder()
                    .endpoint(url.toString())
                    .cloudProviderId(storageService.getData().getProviderId())
                    .cloudServiceId(storageService.getId())
                    .build();
              })
              .collect(Collectors.toList());

          if (resources.isEmpty()) {
            throw new DeploymentException(
                "No registered storage service available for file " + file.getEndpoint());
          }
          file.setResources(resources);
        });
    return dynafed;
  }
}
