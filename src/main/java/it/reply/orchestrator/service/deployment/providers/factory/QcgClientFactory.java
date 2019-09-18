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
import feign.auth.BasicAuthRequestInterceptor;
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

  protected String getFrameworkName() {
    return "Qcg";
  }

  /**
   * Build a Qcg cliet object.
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
   * Build a Qcg cliet object.
   * @param cloudProviderEndpoint the service endpoint.
   * @param accessToken the input accesstoken.
   * @return the Qcg client object.
   */
  public Qcg build(CloudProviderEndpoint cloudProviderEndpoint, String accessToken) {
    final RequestInterceptor requestInterceptor;
    if (cloudProviderEndpoint.getUsername() != null
        || cloudProviderEndpoint.getPassword() != null) {
      Objects.requireNonNull(cloudProviderEndpoint.getUsername(), "Username must be provided");
      Objects.requireNonNull(cloudProviderEndpoint.getPassword(), "Password must be provided");
      requestInterceptor = new BasicAuthRequestInterceptor(cloudProviderEndpoint.getUsername(),
          cloudProviderEndpoint.getPassword());
    } else {
      Objects.requireNonNull(accessToken, "Access Token must not be null");
      requestInterceptor = requestTemplate -> {
        // TODO remove test token ad resume standard implementation
        requestTemplate.header(HttpHeaders.AUTHORIZATION,
            "Bearer " + "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6ImRlZmF1bHQta2lkIn0."
                + "eyJzdWIiOiJkZWZhdWx0LXVzZSIsImlzcyI6ImRlZmF1bHQtaXNzdWVyIiwiaWF0"
                + "IjoxNTYxNTQ0MTA3LCJleHAiOjE1NzAxODQxMDcsImF0dHJzIjp7ImxvY2FsX3Vz"
                + "ZXIiOiJ1bmtub3duIn0sInBlcm1zIjp7Im93bmVyIjpbInN0YXR1cyIsImNhbmNl"
                + "bCJdLCJzeXN0ZW0iOlsic3VibWl0IiwicmVzb3VyY2VzIiwiYWRtaW4iLCJzY2hl"
                + "bWEiXSwiYWRtaW4iOlsidXBkYXRlIiwic3RhdHVzIiwiY2FuY2VsIl19fQ.jW3k5"
                + "jOALPOlxWf7FFjYds0Fs4-bfTdBvBYJxvcgowG-IQbq3KWEX5mrT-8-wc5SzZ9Bl"
                + "7XDr-xx45bxcc0dziGtf05X5MrpxKnQu5q2b9_7w75zM4ijOXnNHLupPSx--rS2Z"
                + "5kctN77oIe3_2S0E5L_ewClKWltO6k7SQIZBmDWPk4TFfbIehYXaOsHsA0oR7hdR"
                + "HKlhMN-hfjZjcmik0jwJR0_oIf7ZS-psVOOCXJ1HK-2UWxj4Hj-P0-FCRAKldMAf"
                + "gChiwcAWJ-Fdp5rWCSg9ZLdI918-gxNvfofrHevKu4XXhlyYsAzm2PNaJSH0TMV3"
                + "T5oSNBWa0iw5r-flA");
        // .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
      };
    }
    return build(cloudProviderEndpoint.getCpEndpoint(), requestInterceptor);
  }

}
