/*
 * Copyright © 2019-2020 I.N.F.N.
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

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import it.reply.orchestrator.annotation.DeploymentProviderQualifier;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.kubernetes.fluxcd.V1HelmRelease;
import it.reply.orchestrator.dto.kubernetes.fluxcd.V1HelmReleaseList;
import it.reply.orchestrator.dto.kubernetes.fluxcd.V1HelmReleaseSpec;
import it.reply.orchestrator.dto.kubernetes.fluxcd.V1HelmReleaseSpecChart;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.function.ThrowingConsumer;
import it.reply.orchestrator.function.ThrowingFunction;
import it.reply.orchestrator.service.IndigoInputsPreProcessorService;
import it.reply.orchestrator.service.ToscaService;
import it.reply.orchestrator.service.deployment.providers.factory.KubernetesClientFactory;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@DeploymentProviderQualifier(DeploymentProvider.KUBERNETES)
@Slf4j
public class KubernetesServiceImpl extends AbstractDeploymentProviderService {

  public static final String DEFAULT_NAMESPACE = "default";
  @Autowired
  private ToscaService toscaService;

  @Autowired
  private ResourceRepository resourceRepository;

  @Autowired
  private KubernetesClientFactory clientFactory;

  @Autowired
  private OAuth2TokenService oauth2TokenService;

  @Autowired
  private IndigoInputsPreProcessorService indigoInputsPreProcessorService;

  private GenericKubernetesApi<V1HelmRelease, V1HelmReleaseList> getHelmClient(
          CloudProviderEndpoint cloudProviderEndpoint, String accessToken) {
    ApiClient apiClient = clientFactory.build(cloudProviderEndpoint, accessToken);
    return new GenericKubernetesApi<>(V1HelmRelease.class, V1HelmReleaseList.class,
            "helm.fluxcd.io", "v1","helmReleases", apiClient);
  }

  protected <R> R executeWithClientForResult(CloudProviderEndpoint cloudProviderEndpoint,
                                             OidcTokenId requestedWithToken,
                                             ThrowingFunction<GenericKubernetesApi<V1HelmRelease,
                                                     V1HelmReleaseList>, R, ApiException> function)
          throws ApiException {
    return oauth2TokenService.executeWithClientForResult(requestedWithToken,
            token -> function.apply(this.getHelmClient(cloudProviderEndpoint, token)),
            ex -> ex instanceof ApiException && ((ApiException) ex).getCode() == 401);
  }

  protected void executeWithClient(CloudProviderEndpoint cloudProviderEndpoint,
                                   OidcTokenId requestedWithToken,
                                   ThrowingConsumer<GenericKubernetesApi<V1HelmRelease,
                                           V1HelmReleaseList>, ApiException> consumer)
          throws ApiException {
    executeWithClientForResult(cloudProviderEndpoint, requestedWithToken,
            client -> consumer.asFunction().apply(client));
  }

  @Override
  protected Optional<String> getAdditionalErrorInfoInternal(DeploymentMessage deploymentMessage) {
    return Optional.empty();
  }

  @Override
  protected Optional<String> getDeploymentLogInternal(DeploymentMessage deploymentMessage) {
    return Optional.empty();
  }

  @Override
  protected Optional<String> getDeploymentExtendedInfoInternal(DeploymentMessage deploymentMessage) {
    return Optional.empty();
  }

  private V1HelmRelease helmReleaseFromTosca(String name, Deployment deployment) {
    V1HelmRelease helmRelease = new V1HelmRelease()
            .apiVersion("helm.fluxcd.io/v1")
            .kind("HelmRelease")
            .metadata(new V1ObjectMeta()
                    .name(name)
                    .namespace(DEFAULT_NAMESPACE)
            ).spec(new V1HelmReleaseSpec()
                    .chart(new V1HelmReleaseSpecChart()
                            .repository("")
                            .name(name)
                            .version("")
                    )
            );
    return helmRelease;
  }
  @Override
  public boolean doDeploy(DeploymentMessage deploymentMessage) {
    Deployment deployment = getDeployment(deploymentMessage);
    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
    deployment.setTask(Task.DEPLOYER);
    String name = deployment.getId();
    deployment.setEndpoint(name);
    CloudProviderEndpoint chosenCloudProviderEndpoint = deploymentMessage
            .getChosenCloudProviderEndpoint();

    V1HelmRelease helmRelease = helmReleaseFromTosca(name, deployment);
    try {
      this.executeWithClientForResult(chosenCloudProviderEndpoint, requestedWithToken,
              client -> client.create(helmRelease));
    } catch (ApiException e) {
      throw new OrchestratorException("Error creating Helm Chart " + name, e);
    }
    return false;
  }

  @Override
  public boolean isDeployed(DeploymentMessage deploymentMessage) {
    CloudProviderEndpoint chosenCloudProviderEndpoint = deploymentMessage
            .getChosenCloudProviderEndpoint();
    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
    Deployment deployment = getDeployment(deploymentMessage);
    String name = deployment.getEndpoint();
    V1HelmRelease helmRelease = this.executeWithClientForResult(chosenCloudProviderEndpoint, requestedWithToken,
            client -> client.get(name));
    if (helmRelease.getStatus() == null) {
      return false;
    }
    switch (helmRelease.getStatus().getPhase()) {

      case SUCCEEDED:
      case ROLLEDBACK:
      case DEPLOYED:
        break;
      case CHARTFETCHED:
      case INSTALLING:
      case UPGRADING:
      case TESTING:
      case TESTED:
      case ROLLINGBACK:
        return false;
      case FAILED:
      case DEPLOYFAILED:
      case TESTFAILED:
      case CHARTFETCHFAILED:
      case ROLLBACKFAILED:
        break;
    }
    return false;
  }

  @Override
  public void cleanFailedDeploy(DeploymentMessage deploymentMessage) {

  }

  @Override
  public boolean doUpdate(DeploymentMessage deploymentMessage, String template) {
    return false;
  }

  @Override
  public void cleanFailedUpdate(DeploymentMessage deploymentMessage) {

  }

  @Override
  public boolean doUndeploy(DeploymentMessage deploymentMessage) {
    return false;
  }

  @Override
  public boolean isUndeployed(DeploymentMessage deploymentMessage) {
    return false;
  }

  @Override
  public void doProviderTimeout(DeploymentMessage deploymentMessage) {

  }
}
