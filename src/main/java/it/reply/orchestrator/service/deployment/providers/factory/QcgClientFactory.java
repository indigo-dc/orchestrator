/*
 * Copyright © 2019 I.N.F.N.
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
      // TODO remove test token ad resume standard implementation
      requestTemplate.header(HttpHeaders.AUTHORIZATION,
          "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6ImRlZmF1bHQta2lkIn0.e"
              + "yJzdWIiOiJkZWZhdWx0LXVzZSIsImlzcyI6ImRlZmF1bHQtaXNzdWVyIiwiaWF0Ij"
              + "oxNTcwNTIzMTIzLCJleHAiOjE1NzkxNjMxMjMsImF0dHJzIjp7ImxvY2FsX3VzZXI"
              + "iOiJ1bmtub3duIn0sInBlcm1zIjp7Im93bmVyIjpbInN0YXR1cyIsImNhbmNlbCJd"
              + "LCJzeXN0ZW0iOlsic3VibWl0IiwicmVzb3VyY2VzIiwiYWRtaW4iLCJzY2hlbWEiX"
              + "SwiYWRtaW4iOlsidXBkYXRlIiwic3RhdHVzIiwiY2FuY2VsIl19fQ.e1zvno3Z99S"
              + "9TCESOq40V3BG5bhgpuntQS5S2dIRFFpMnMfJlvGXaNe60JbmrH3Ps7muzAIYQ2Cr"
              + "hjurblklFBfKIYqO5fT3nWrbHCqwwm8ArHxcUbZAzWSoDIFmPdIpJRG69oE0B6KcS"
              + "Vs9I96tD9gl6H605NLyR8LC8UgkhVceVEKQbufj5A6QcR2XiQx3oHGEDLk2KlV4XN"
              + "Jlt28-SJcd71FObtnXGwcCKBVblCZ75AUYLZeDU0gObRT0objw2y7kxpu2YSF9wVi"
              + "Q2AzAIkfFCJP5HEqRjTVq5SsxNyK0sqEmXcjgbwoMFrfpr8WBj3ynO_f18gEBnwSO3H8vYQ");
      // .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    };
    return build(cloudProviderEndpoint.getCpEndpoint(), requestInterceptor);
  }

}
