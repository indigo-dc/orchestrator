/*
 * Copyright © 2015-2019 Santer Reply S.p.A.
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

package it.reply.orchestrator.service.deployment.providers;

import it.reply.orchestrator.dto.security.GenericCredentialInterface;
import it.reply.orchestrator.dto.vault.TokenAuthenticationExtended;
import it.reply.orchestrator.service.VaultService;

import java.net.URI;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CredentialProviderService implements CredentialProviderServiceInterface {

  @Autowired
  private VaultService vaultService;

  /**
   * Get credentials stored in vault service.
   *
   * @param  serviceId  is CpComputeServiceId of cloud provider
   * @param  accessToken with audience
   * @param  clazz type of return class
   * @return GenericCredential or GenericCredentialWithTenant
   */
  public <T extends GenericCredentialInterface> T credentialProvider(String serviceId,
      String accessToken, Class<T> clazz) {

    if (serviceId == null || serviceId.isEmpty()) {
      LOG.error("SeriviceId is empty");
    }

    TokenAuthenticationExtended vaultToken =
        (TokenAuthenticationExtended) vaultService.retrieveToken(
            vaultService.getServiceUri().get(),
            accessToken
            );

    String pathVaultComplete = vaultService.getServiceUri().get()
        + "/v1/secret/data/"
        + vaultToken.getEntityId()
        + vaultService.getServicePath()
        + serviceId;

    return vaultService.readSecret(vaultToken, pathVaultComplete, clazz);
  }

}
