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

import feign.RequestInterceptor;
import feign.auth.BasicAuthRequestInterceptor;

import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.cmdb.MesosFrameworkService;
import it.reply.orchestrator.dto.security.GenericServiceCredential;
import it.reply.orchestrator.service.deployment.providers.CredentialProviderService;

import java.util.Objects;

import lombok.AllArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public abstract class MesosFrameworkClientFactory<V extends MesosFrameworkService, T> {

  @Autowired
  private CredentialProviderService credProvServ;

  /**
   * Generate a new Mesos framework client.
   *
   * @param cloudProviderEndpoint the framework endpoint
   * @param accessToken the access token
   * @return the new client
   */
  public T build(CloudProviderEndpoint cloudProviderEndpoint, String accessToken) {
    final RequestInterceptor requestInterceptor;
    Objects.requireNonNull(accessToken, "Access Token must not be null");
    if (cloudProviderEndpoint.isIamEnabled()) {
      requestInterceptor = requestTemplate -> {
        requestTemplate.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
      };
    } else {
      GenericServiceCredential imCred = credProvServ.credentialProvider(
          cloudProviderEndpoint.getCpComputeServiceId(),
          accessToken,
          GenericServiceCredential.class
          );
      requestInterceptor =
          new BasicAuthRequestInterceptor(imCred.getUsername(), imCred.getPassword());
    }
    return build(cloudProviderEndpoint.getCpEndpoint(), requestInterceptor);
  }

  public abstract T build(String endpoint, RequestInterceptor authInterceptor);

}
