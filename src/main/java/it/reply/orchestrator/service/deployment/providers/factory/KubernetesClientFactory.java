/*
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

package it.reply.orchestrator.service.deployment.providers.factory;

import feign.Feign;
import feign.Logger.Level;
import feign.RequestInterceptor;
import feign.slf4j.Slf4jLogger;

import io.kubernetes.client.openapi.ApiClient;

import it.infn.ba.deep.qcg.client.utils.QcgException;
import it.reply.orchestrator.dto.CloudProviderEndpoint;

import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KubernetesClientFactory {

  /**
   * Build a Kubernetes Api client object.
   * @param cloudProviderEndpoint the service endpoint.
   * @param accessToken the input accesstoken.
   * @return the Kubernetes Api client object.
   */
  public ApiClient build(CloudProviderEndpoint cloudProviderEndpoint, String accessToken) {
    final RequestInterceptor requestInterceptor;
    Objects.requireNonNull(accessToken, "Access Token must not be null");
    requestInterceptor = requestTemplate ->
        requestTemplate.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

    return build(cloudProviderEndpoint.getCpEndpoint(), requestInterceptor);
  }

  /**
   * Build a Kubernetes client object.
   * @param apiClientEndpoint the input Kubernetes service endpoint.
   * @param authInterceptor the input request interceptor.
   * @return the Kubernetes client object.
   */
  public ApiClient build(String apiClientEndpoint, RequestInterceptor authInterceptor) {
    LOG.info("Generating Qcg client with endpoint {}", apiClientEndpoint);

    //TODO * new ApiEncoder()).decoder(new QcgDecoder()
    return Feign.builder().encoder(null/* * */)
        .logger(new Slf4jLogger(ApiClient.class))
        .logLevel(Level.FULL)
        .errorDecoder((methodKey, response) -> new QcgException(response.status(),
            response.reason()))
        .requestInterceptor(authInterceptor).requestInterceptor(template -> {
          template.header(HttpHeaders.ACCEPT, "application/json");
          template.header(HttpHeaders.CONTENT_TYPE, "application/json");
        }).target(ApiClient.class, apiClientEndpoint);
  }

}
