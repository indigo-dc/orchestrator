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

package it.reply.orchestrator.service.deployment.providers;

import alien4cloud.tosca.model.ArchiveRoot;
import io.kubernetes.client.common.KubernetesType;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.kubernetes.client.util.generic.KubernetesApiResponse;
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
import it.reply.orchestrator.dto.kubernetes.fluxcd.V1HelmReleaseSpecRollback;
import it.reply.orchestrator.dto.kubernetes.fluxcd.V1HelmReleaseStatus;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.exception.service.BusinessWorkflowException;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.function.ThrowingFunction;
import it.reply.orchestrator.service.IndigoInputsPreProcessorService;
import it.reply.orchestrator.service.ToscaService;
import it.reply.orchestrator.service.deployment.providers.factory.KubernetesClientFactory;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.orchestrator.utils.ToscaConstants;
import it.reply.orchestrator.utils.ToscaUtils;
import it.reply.orchestrator.utils.WorkflowConstants;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@DeploymentProviderQualifier(DeploymentProvider.KUBERNETES)
@Slf4j
public class KubernetesServiceImpl extends AbstractDeploymentProviderService {

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
    return new GenericKubernetesApi<>(
            V1HelmRelease.class,
            V1HelmReleaseList.class,
            "helm.fluxcd.io",
            "v1",
            "helmReleases",
            apiClient);
  }

  @Override
  protected Optional<String> getAdditionalErrorInfoInternal(DeploymentMessage deploymentMessage) {
    CloudProviderEndpoint chosenCloudProviderEndpoint = deploymentMessage
            .getChosenCloudProviderEndpoint();
    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
    Deployment deployment = getDeployment(deploymentMessage);
    String name = deployment.getEndpoint();
    try {
      return Optional.of(executeWithClientForResult(chosenCloudProviderEndpoint,
              requestedWithToken, client -> client.get(name)))
              .map(KubernetesApiResponse::getObject)
              .map(V1HelmRelease::getStatus)
              .map(V1HelmReleaseStatus::getReleaseStatus);
    } catch (ApiException e) {
      LOG.warn("Error retrieving status information for helm release {}", name);
      return Optional.empty();
    }
  }

  @Override
  protected Optional<String> getDeploymentLogInternal(DeploymentMessage deploymentMessage) {
    // TODO
    return Optional.empty();
  }

  @Override
  protected Optional<String> getDeploymentExtendedInfoInternal(
          DeploymentMessage deploymentMessage) {
    // TODO
    return Optional.empty();
  }

  protected <R extends KubernetesType> KubernetesApiResponse<R> executeWithClientForResult(
          CloudProviderEndpoint cloudProviderEndpoint,
          OidcTokenId requestedWithToken,
          ThrowingFunction<GenericKubernetesApi<V1HelmRelease, V1HelmReleaseList>,
                  KubernetesApiResponse<R>, ApiException> function)
          throws ApiException {
    return oauth2TokenService
             .executeWithClientForResult(requestedWithToken, token -> function
                            .apply(this.getHelmClient(cloudProviderEndpoint, token))
                            .throwsApiException(),
                 ex -> ex instanceof ApiException && ((ApiException) ex).getCode() == 401);
  }

  private V1HelmRelease helmReleaseFromTosca(String name, String namespace, Deployment deployment) {

    Map<String, Object> inputs = deployment.getParameters();
    ArchiveRoot ar = toscaService.parseAndValidateTemplate(deployment.getTemplate(), inputs);
    indigoInputsPreProcessorService.processGetInput(ar, inputs);

    NodeTemplate chartNode = Optional
        .ofNullable(ar.getTopology())
        .map(Topology::getNodeTemplates)
        .flatMap(nodes -> nodes
            .values()
            .stream()
            .filter(node -> toscaService
                .isOfToscaType(node, ToscaConstants.Nodes.Types.KUBERNETES_HELM_CHART))
                .findFirst())
        .orElseThrow(() -> new OrchestratorException("Unable to find Kubernetes TOSCA node"));

    String repository = ToscaUtils.extractScalar(chartNode.getProperties(), "repository")
        .orElseThrow(() -> new OrchestratorException("Helm Repository not specified"));
    String chartName = ToscaUtils.extractScalar(chartNode.getProperties(), "name")
        .orElseThrow(() -> new OrchestratorException("Chart Name not specified"));
    String version = ToscaUtils.extractScalar(chartNode.getProperties(), "version")
        .orElseThrow(() -> new OrchestratorException("Chart Version not specified"));
    Map<String, Object> values = ToscaUtils.extractMap(chartNode.getProperties(), "values")
        .orElseGet(HashMap::new);

    return new V1HelmRelease()
            .apiVersion("helm.fluxcd.io/v1")
            .kind("HelmRelease")
            .metadata(new V1ObjectMeta()
                    .name(name)
                    .namespace(namespace)
            ).spec(new V1HelmReleaseSpec()
                    .chart(new V1HelmReleaseSpecChart()
                            .repository(repository)
                            .name(chartName)
                            .version(version)
                    ).wait(true)
                    .values(values)
            );
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
    String namespace = oauth2TokenService.getOrganization(requestedWithToken);

    V1HelmRelease helmRelease = helmReleaseFromTosca(name, namespace, deployment);
    try {
      this.executeWithClientForResult(chosenCloudProviderEndpoint, requestedWithToken,
          client -> client.create(helmRelease));
    } catch (ApiException e) {
      throw new OrchestratorException("Error creating Helm Release", e);
    }
    return true;
  }

  @Override
  public boolean isDeployed(DeploymentMessage deploymentMessage) {
    CloudProviderEndpoint chosenCloudProviderEndpoint = deploymentMessage
            .getChosenCloudProviderEndpoint();
    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
    Deployment deployment = getDeployment(deploymentMessage);
    String name = deployment.getEndpoint();
    V1HelmRelease helmRelease = null;
    try {
      helmRelease = this.executeWithClientForResult(chosenCloudProviderEndpoint,
              requestedWithToken, client -> client.get(name)).getObject();
    } catch (ApiException e) {
      throw new OrchestratorException("Error retrieving helm release status", e);
    }
    if (helmRelease.getStatus() == null || helmRelease.getStatus().getPhase() == null) {
      return false;
    }
    switch (helmRelease.getStatus().getPhase()) {
      case SUCCEEDED:
        return "deployed".equals(helmRelease.getStatus().getReleaseStatus());
      case DEPLOYED:
      case CHARTFETCHED:
      case INSTALLING:
      case UPGRADING:
      case TESTING:
      case TESTED:
      case ROLLINGBACK:
        return false;
      case ROLLEDBACK:
      case FAILED:
      case DEPLOYFAILED:
      case TESTFAILED:
      case CHARTFETCHFAILED:
      case ROLLBACKFAILED:
        throw new BusinessWorkflowException(WorkflowConstants.ErrorCode.CLOUD_PROVIDER_ERROR,
                "Helm release creation or update failed");
      default:
        throw new OrchestratorException("Unknown Chart phase: "
                + helmRelease.getStatus().getPhase());
    }
  }

  @Override
  public void cleanFailedDeploy(DeploymentMessage deploymentMessage) {
    doUndeploy(deploymentMessage);
  }

  @Override
  public boolean doUpdate(DeploymentMessage deploymentMessage, String template) {
    Deployment deployment = getDeployment(deploymentMessage);
    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
    deployment.setTask(Task.DEPLOYER);
    String name = deployment.getId();
    deployment.setEndpoint(name);
    CloudProviderEndpoint chosenCloudProviderEndpoint = deploymentMessage
            .getChosenCloudProviderEndpoint();
    String namespace = oauth2TokenService.getOrganization(requestedWithToken);

    V1HelmRelease helmRelease = helmReleaseFromTosca(name, namespace, deployment);
    helmRelease
            .getSpec()
            // automatically rollback if upgrade fails
            .setRollback(new V1HelmReleaseSpecRollback().enable(true));
    try {
      this.executeWithClientForResult(chosenCloudProviderEndpoint, requestedWithToken,
          client -> client.update(helmRelease));
    } catch (ApiException e) {
      throw new OrchestratorException("Error updating Helm release", e);
    }
    return true;
  }

  @Override
  public void cleanFailedUpdate(DeploymentMessage deploymentMessage) {
    // nothing to do, charts are automatically rolled back
  }

  @Override
  public boolean doUndeploy(DeploymentMessage deploymentMessage) {
    Deployment deployment = getDeployment(deploymentMessage);
    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
    String name = deployment.getId();
    CloudProviderEndpoint chosenCloudProviderEndpoint = deploymentMessage
            .getChosenCloudProviderEndpoint();
    try {
      this.executeWithClientForResult(chosenCloudProviderEndpoint, requestedWithToken,
          client -> client.delete(name));
    } catch (ApiException e) {
      throw new OrchestratorException("Error deleting Helm release", e);
    }
    return true;
  }

  @Override
  public boolean isUndeployed(DeploymentMessage deploymentMessage) {
    Deployment deployment = getDeployment(deploymentMessage);
    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
    String name = deployment.getId();
    CloudProviderEndpoint chosenCloudProviderEndpoint = deploymentMessage
            .getChosenCloudProviderEndpoint();
    try {
      this.executeWithClientForResult(chosenCloudProviderEndpoint, requestedWithToken,
          client -> client.get(name));
    } catch (ApiException e) {
      if (404 == e.getCode()) {
        return true;
      }
      throw new OrchestratorException("Error while checking for Helm release deletion ", e);
    }
    return false;
  }

  @Override
  public void doProviderTimeout(DeploymentMessage deploymentMessage) {
    throw new BusinessWorkflowException(WorkflowConstants.ErrorCode.CLOUD_PROVIDER_ERROR,
            "Error executing request to Kubernetes service",
            new DeploymentException("Provider Timeout"));
  }
}
