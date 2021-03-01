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

package it.reply.orchestrator.service.deployment.providers.factory;

import feign.Feign;
import feign.Logger.Level;
import feign.RequestInterceptor;
import feign.slf4j.Slf4jLogger;

import it.infn.ba.deep.qcg.client.Qcg;
import it.infn.ba.deep.qcg.client.utils.QcgDecoder;
import it.infn.ba.deep.qcg.client.utils.QcgEncoder;
import it.infn.ba.deep.qcg.client.utils.QcgException;

import it.reply.orchestrator.dto.CloudProviderEndpoint;

import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class QcgClientFactory {

  /**
   * Build a Qcg client object.
   * @param qcgEndpoint the input qcg service endpoint.
   * @param authInterceptor the input request interceptor.
   * @return the Qcg client object.
   */
  public Qcg build(String qcgEndpoint, RequestInterceptor authInterceptor) {
    LOG.info("Generating Qcg client with endpoint {}", qcgEndpoint);

    return Feign.builder().encoder(new QcgEncoder()).decoder(new QcgDecoder())
        .logger(new Slf4jLogger(Qcg.class))
        .logLevel(Level.FULL)
        .errorDecoder((methodKey, response) -> new QcgException(response.status(),
            response.reason()))
        .requestInterceptor(authInterceptor).requestInterceptor(template -> {
          template.header(HttpHeaders.ACCEPT, "application/json");
          template.header(HttpHeaders.CONTENT_TYPE, "application/json");
        }).target(Qcg.class, qcgEndpoint);
  }

  /**
   * Build a Qcg client object.
   * @param cloudProviderEndpoint the service endpoint.
   * @param accessToken the input accesstoken.
   * @return the Qcg client object.
   */
  public Qcg build(CloudProviderEndpoint cloudProviderEndpoint, String accessToken) {
    Objects.requireNonNull(accessToken, "Access Token must not be null");
    RequestInterceptor requestInterceptor = requestTemplate -> {
      requestTemplate.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    };
    return build(cloudProviderEndpoint.getCpEndpoint(), requestInterceptor);
  }

}
