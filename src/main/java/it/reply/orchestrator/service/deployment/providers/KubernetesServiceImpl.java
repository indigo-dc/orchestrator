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

package it.reply.orchestrator.service.deployment.providers;

import alien4cloud.tosca.model.ArchiveRoot;
import io.kubernetes.client.common.KubernetesType;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1ServiceList;
import io.kubernetes.client.openapi.models.V1ServiceSpec;
import io.kubernetes.client.openapi.models.V1Status;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.kubernetes.client.util.generic.KubernetesApiResponse;
import it.reply.orchestrator.annotation.DeploymentProviderQualifier;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.cmdb.KubernetesService;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.kubernetes.fluxcd.V1HelmRelease;
import it.reply.orchestrator.dto.kubernetes.fluxcd.V1HelmReleaseList;
import it.reply.orchestrator.dto.kubernetes.fluxcd.V1HelmReleaseSpec;
import it.reply.orchestrator.dto.kubernetes.fluxcd.V1HelmReleaseSpecChart;
import it.reply.orchestrator.dto.kubernetes.fluxcd.V1HelmReleaseSpecRollback;
import it.reply.orchestrator.dto.kubernetes.fluxcd.V1HelmReleaseStatusConditions;
import it.reply.orchestrator.dto.workflow.CloudServicesOrderedIterator;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.exception.service.BusinessWorkflowException;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.function.ThrowingFunction;
import it.reply.orchestrator.service.IndigoInputsPreProcessorService;
import it.reply.orchestrator.service.IndigoInputsPreProcessorService.RuntimeProperties;
import it.reply.orchestrator.service.ToscaService;
import it.reply.orchestrator.service.deployment.providers.factory.KubernetesClientFactory;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.orchestrator.utils.CommonUtils;
import it.reply.orchestrator.utils.ToscaConstants;
import it.reply.orchestrator.utils.ToscaUtils;
import it.reply.orchestrator.utils.WorkflowConstants;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.model.definitions.OutputDefinition;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

@Service
@DeploymentProviderQualifier(DeploymentProvider.KUBERNETES)
@Slf4j
public class KubernetesServiceImpl extends AbstractDeploymentProviderService {

  @Autowired
  private ToscaService toscaService;

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
            "helmreleases",
            apiClient);
  }

  private CoreV1Api getCoreClient(
      CloudProviderEndpoint cloudProviderEndpoint, String accessToken) {
    ApiClient apiClient = clientFactory.build(cloudProviderEndpoint, accessToken);
    return new CoreV1Api(apiClient);
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
  protected Optional<String> getDeploymentExtendedInfoInternal(
          DeploymentMessage deploymentMessage) {
    return Optional.empty();
  }

  protected <R extends KubernetesType> KubernetesApiResponse<R> executeWithHelmClientForResult(
          CloudProviderEndpoint cloudProviderEndpoint,
          OidcTokenId requestedWithToken,
          ThrowingFunction<GenericKubernetesApi<V1HelmRelease, V1HelmReleaseList>,
                  KubernetesApiResponse<R>, ApiException> function)
          throws ApiException {
    return oauth2TokenService
             .executeWithClientForResult(requestedWithToken, token -> function
                            .apply(this.getHelmClient(cloudProviderEndpoint, token))
                            .onFailure(this::throwApiException),
                 ex -> ex instanceof ApiException && ((ApiException) ex).getCode() == 401);
  }

  protected <R> R executeWithCoreClientForResult(
      CloudProviderEndpoint cloudProviderEndpoint,
      OidcTokenId requestedWithToken,
      ThrowingFunction<CoreV1Api, R, ApiException> function)
      throws ApiException {
    return oauth2TokenService
        .executeWithClientForResult(requestedWithToken, token -> function
                .apply(this.getCoreClient(cloudProviderEndpoint, token)),
            ex -> ex instanceof ApiException && ((ApiException) ex).getCode() == 401);
  }

  private void throwApiException(V1Status errorStatus) throws ApiException {
    int errorCode = Optional.ofNullable(errorStatus.getCode()).orElse(0);
    throw new ApiException(errorCode, errorStatus.toString());
  }

  private V1HelmRelease helmReleaseFromTosca(V1HelmRelease helmRelease, Deployment deployment) {
    return helmReleaseFromTosca(helmRelease, deployment, deployment.getTemplate());
  }

  private V1HelmRelease helmReleaseFromTosca(V1HelmRelease helmRelease, Deployment deployment,
      String template) {
    Map<String, Object> inputs = deployment.getParameters();
    ArchiveRoot ar = toscaService.parseAndValidateTemplate(template, inputs);
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
    String values = ToscaUtils.extractScalar(chartNode.getProperties(), "values")
        .orElseGet(String::new);

    return helmRelease
        .spec(new V1HelmReleaseSpec()
                .chart(new V1HelmReleaseSpecChart()
                        .repository(repository)
                        .name(chartName)
                        .version(version)
                ).wait(true)
                .values(new Yaml().load(values))
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

    V1HelmRelease helmRelease = new V1HelmRelease()
        .apiVersion("helm.fluxcd.io/v1")
        .kind("HelmRelease")
        .metadata(new V1ObjectMeta()
            .name(name)
            .namespace(namespace)
        );
    helmReleaseFromTosca(helmRelease, deployment);
    try {
      this.executeWithHelmClientForResult(chosenCloudProviderEndpoint, requestedWithToken,
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
    String namespace = oauth2TokenService.getOrganization(requestedWithToken);
    V1HelmRelease helmRelease = null;
    try {
      helmRelease = this.executeWithHelmClientForResult(chosenCloudProviderEndpoint,
              requestedWithToken, client -> client.get(namespace, name)).getObject();
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
        String cause = Optional
            .ofNullable(helmRelease.getStatus().getConditions())
            .orElseGet(Collections::emptyList)
            .stream()
            .reduce((first, second) -> second)
            .map(V1HelmReleaseStatusConditions::getMessage)
            .map(message -> "\n" + message)
            .orElse("");
        throw new BusinessWorkflowException(WorkflowConstants.ErrorCode.CLOUD_PROVIDER_ERROR,
                "Helm release creation or update failed" + cause);
      default:
        throw new OrchestratorException("Unknown Chart phase: "
                + helmRelease.getStatus().getPhase());
    }
  }

  @Override
  public void cleanFailedDeploy(DeploymentMessage deploymentMessage) {
    CloudServicesOrderedIterator iterator = deploymentMessage.getCloudServicesOrderedIterator();
    boolean isLastProvider = !iterator.hasNext();
    boolean isKeepLastAttempt = deploymentMessage.isKeepLastAttempt();
    LOG.info("isLastProvider: {} and isKeepLastAttempt: {}", isLastProvider, isKeepLastAttempt);

    Deployment deployment = getDeployment(deploymentMessage);
    String deploymentEndpoint = deployment.getEndpoint();

    if (deploymentEndpoint == null) {
      LOG.info("Nothing left to clean up from last deployment attempt");
    } else if (isLastProvider && isKeepLastAttempt) {
      LOG.info("Keeping the last deployment attempt");
    } else {
      LOG.info("Deleting the last deployment attempt");
      doUndeploy(deploymentMessage);
    }
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

    try {
      this.executeWithHelmClientForResult(chosenCloudProviderEndpoint, requestedWithToken,
          client -> {
            V1HelmRelease helmRelease = client.get(namespace, name).getObject();
            helmRelease
                .getSpec()
                // automatically rollback if upgrade fails
                .setRollback(new V1HelmReleaseSpecRollback().enable(true));
            helmReleaseFromTosca(helmRelease, deployment, template);
            return client.update(helmRelease);
          });
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
    CloudProviderEndpoint chosenCloudProviderEndpoint = deployment.getCloudProviderEndpoint();
    String namespace = oauth2TokenService.getOrganization(requestedWithToken);
    try {
      this.executeWithHelmClientForResult(chosenCloudProviderEndpoint, requestedWithToken,
          client -> client.delete(namespace, name));
    } catch (ApiException e) {
      if (404 == e.getCode()) {
        return true;
      }
      throw new OrchestratorException("Error deleting Helm release", e);
    }
    return true;
  }

  @Override
  public boolean isUndeployed(DeploymentMessage deploymentMessage) {
    Deployment deployment = getDeployment(deploymentMessage);
    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
    String name = deployment.getId();
    CloudProviderEndpoint chosenCloudProviderEndpoint = deployment.getCloudProviderEndpoint();
    String namespace = oauth2TokenService.getOrganization(requestedWithToken);
    try {
      this.executeWithHelmClientForResult(chosenCloudProviderEndpoint, requestedWithToken,
          client -> client.get(namespace, name));
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

  @Override
  public void finalizeDeploy(DeploymentMessage deploymentMessage) {
    Deployment deployment = getDeployment(deploymentMessage);
    Map<String, Object> inputs = deployment.getParameters();
    ArchiveRoot ar = toscaService.parseAndValidateTemplate(deployment.getTemplate(), inputs);
    indigoInputsPreProcessorService.processGetInput(ar, inputs);

    Map<String, OutputDefinition> outputs = Optional
        .ofNullable(ar.getTopology())
        .map(Topology::getOutputs)
        .orElseGet(HashMap::new);
    if (!outputs.isEmpty()) {
      CloudProviderEndpoint chosenCloudProviderEndpoint = deploymentMessage
          .getChosenCloudProviderEndpoint();
      final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();

      String name = deployment.getEndpoint();
      String namespace = oauth2TokenService.getOrganization(requestedWithToken);
      String labelSelector = String.format("app.kubernetes.io/instance=%s-%s", namespace, name);
      Map<String, NodeTemplate> nodes = Optional
          .ofNullable(ar.getTopology())
          .map(Topology::getNodeTemplates)
          .orElseGet(HashMap::new);

      DirectedMultigraph<NodeTemplate, RelationshipTemplate> graph =
          toscaService.buildNodeGraph(nodes, false);

      TopologicalOrderIterator<NodeTemplate, RelationshipTemplate> orderIterator =
          new TopologicalOrderIterator<>(graph);

      String chartNodeName = CommonUtils
          .iteratorToStream(orderIterator)
          .filter(node -> toscaService
              .isOfToscaType(node, ToscaConstants.Nodes.Types.KUBERNETES_HELM_CHART))
          .findFirst()
          .orElseThrow(() -> new DeploymentException("Error finalizing deployment"))
          .getName();

      RuntimeProperties runtimeProperties = new RuntimeProperties();
      KubernetesService kubernetesService = deploymentMessage
          .getCloudServicesOrderedIterator()
          .currentService(KubernetesService.class);

      runtimeProperties.put(kubernetesService.getWorkerNodesIp(), chartNodeName,
          "worker_nodes_ips");
      try {
        V1ServiceList services = this.executeWithCoreClientForResult(chosenCloudProviderEndpoint,
            requestedWithToken, client -> client
                .listNamespacedService(namespace, null, null, null, null, labelSelector, null, null,
                    null, null));
        services.getItems().forEach(service -> {
          String serviceName = service
              .getMetadata().getName().split(namespace + "-" + name + "-")[1];
          Optional
              .ofNullable(service.getSpec())
              .map(V1ServiceSpec::getPorts)
              .orElseGet(Collections::emptyList)
              .forEach(portSpec -> {
                String portName = portSpec.getName();
                Optional
                    .ofNullable(portSpec.getNodePort())
                    .ifPresent(target -> runtimeProperties
                        .put(target, chartNodeName, "service_ports", serviceName,
                            portName, "target"));
                Optional
                    .ofNullable(portSpec.getPort())
                    .ifPresent(source -> runtimeProperties
                        .put(source, chartNodeName, "service_ports", serviceName,
                            portName, "source"));
                Optional
                    .ofNullable(portSpec.getProtocol())
                    .map(String::toLowerCase)
                    .ifPresent(protocol -> runtimeProperties
                        .put(protocol, chartNodeName, "service_ports", serviceName,
                            portName, "protocol"));
              });
        });

      } catch (ApiException e) {
        throw new OrchestratorException("Error retrieving helm release status", e);
      }
      deployment.setOutputs(indigoInputsPreProcessorService.processOutputs(ar,
          deployment.getParameters(), runtimeProperties));
    }
    super.finalizeDeploy(deploymentMessage);
  }
}
