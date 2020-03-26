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
import feign.auth.BasicAuthRequestInterceptor;
import feign.RequestInterceptor;
import feign.slf4j.Slf4jLogger;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.auth.ApiKeyAuth;
import io.kubernetes.client.util.Config;
import it.infn.ba.deep.qcg.client.utils.QcgException;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.security.GenericServiceCredential;
import it.reply.orchestrator.utils.CommonUtils;

import java.io.IOException;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KubernetesClientFactory extends CloudService{

//  /**
//   * Build a Kubernetes Api client object.
//   * @param cloudProviderEndpoint the service endpoint.
//   * @param accessToken the input accesstoken.
//   * @return the Kubernetes Api client object.
//   */
//  public void build(CloudProviderEndpoint cloudProviderEndpoint, OidcTokenId accessToken) {
//    final RequestInterceptor requestInterceptor;
//    Objects.requireNonNull(accessToken, "Access Token must not be null");
//    requestInterceptor = requestTemplate ->
//        requestTemplate.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
//        
//    //TODO if needed
//  }

  /**
   * Build a Kubernetes client object.
   * @param cloudProviderEndpoint the input Kubernetes service endpoint.
   * @param accessToken.
   * @return AppsV1Api the Kubernetes client object.
   * @throws IOException 
   */
  public AppsV1Api build(CloudProviderEndpoint cloudProviderEndpoint, String accessToken) throws IOException {
    LOG.info("Generating Kubernetes client with endpoint {}", cloudProviderEndpoint.getCpEndpoint());

    ApiClient x = new ApiClient();

    ApiKeyAuth bearerToken = (ApiKeyAuth) x.getAuthentication("BearerToken");
    bearerToken.setApiKey(accessToken);
    
    String cpEndpoint = cloudProviderEndpoint.getCpEndpoint();

    //x.setAccessToken(accessToken);
    if (cpEndpoint != null && !cpEndpoint.isEmpty()) {
      x.setBasePath(cpEndpoint);
    }

    ApiClient client = Config.fromToken(cpEndpoint, accessToken);
    if (client == null) {
      client = Config.defaultClient();
    }
    return new AppsV1Api(client);
  }

}
