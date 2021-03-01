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

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.Config;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.security.GenericServiceCredential;
import it.reply.orchestrator.service.deployment.providers.CredentialProviderService;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class KubernetesClientFactory {

  private final CredentialProviderService credentialProvider;

  /**
   * Generate a new Kubernetes client.
   *
   * @param cloudProviderEndpoint the Kubernetes endpoint
   * @param accessToken the access token
   * @return the new client
   */
  public ApiClient build(CloudProviderEndpoint cloudProviderEndpoint, String accessToken) {
    Objects.requireNonNull(accessToken, "Access Token must not be null");
    if (cloudProviderEndpoint.isIamEnabled()) {
      return Config.fromToken(cloudProviderEndpoint.getCpEndpoint(), accessToken, false);
    } else {
      GenericServiceCredential credential = credentialProvider.credentialProvider(
          cloudProviderEndpoint.getCpComputeServiceId(),
          accessToken,
          GenericServiceCredential.class
          );
      return Config.fromUserPassword(cloudProviderEndpoint.getCpEndpoint(),
              credential.getUsername(),
              credential.getPassword(),
              false
      );
    }
  }

}
