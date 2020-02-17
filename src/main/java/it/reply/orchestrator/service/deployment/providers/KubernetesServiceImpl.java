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

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.ApiResponse;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentBuilder;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1Status;
import io.kubernetes.client.util.Config;

import it.reply.orchestrator.annotation.DeploymentProviderQualifier;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.exception.service.BusinessWorkflowException;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.ToscaService;
import it.reply.orchestrator.service.VaultService;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.orchestrator.utils.WorkflowConstants.ErrorCode;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@DeploymentProviderQualifier(DeploymentProvider.KUBERNETES)
@Slf4j
public class KubernetesServiceImpl extends AbstractDeploymentProviderService {

  @Autowired
  private ToscaService toscaService;

  @Autowired
  private OAuth2TokenService oauth2TokenService;

  @Autowired
  private VaultService vaultService;

  @Override
  public boolean doDeploy(DeploymentMessage deploymentMessage) {
    Deployment deployment = getDeployment(deploymentMessage);
    V1Deployment outDepl = new V1Deployment();
    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();

    // Update status of the deployment - if not already done (remember the Iterative
    // mode)
    if (deployment.getTask() != Task.DEPLOYER) {
      deployment.setTask(Task.DEPLOYER);
    }
    if (deployment.getEndpoint() == null) {
      deployment.setEndpoint("<NO_ENDPOINT>");
    }

    try {
      // access configuration TODO
      ApiClient client = Config.defaultClient();
      Configuration.setDefaultApiClient(client);
      AppsV1Api app = new AppsV1Api(client);

      String name = deployment.getId();

      //TODO manage needed field for deployment and get them from DeploymentMessage
      V1Deployment v1Deployment = new V1DeploymentBuilder()
          .withApiVersion("apps/v1")
          .withKind("Deployment")
          .withNewMetadata()
              .withName(name)
          .endMetadata()
          .withNewSpec()
              .withReplicas((Integer) deployment.getParameters().get("replicas"))
              .withNewSelector()
                  .addToMatchLabels("app", "nginx")
              .endSelector()
              .withNewTemplate()
                  .withNewMetadata()
                      .addToLabels("app", "nginx")
                  .endMetadata()
                  .withNewSpec()
                      .addNewContainer()
                          .withImage("nginx:1.7.9")
                          .withName("nginx")
                          .addNewPort()
                              .withContainerPort(80)
                          .endPort()
                      .endContainer()
                  .endSpec()
              .endTemplate()
          .endSpec()
          .build();

      outDepl = app.createNamespacedDeployment(
          "default",
          v1Deployment,
          "true",
          null,
          null);

      String ref = outDepl.getMetadata().getName();

    } catch (ApiException e) {
      LOG.error("Error in doUndeploy:" + e.getCode() + " - " + e.getMessage());
    } catch (IOException e) {
      LOG.error("Error in doUndeploy:" + e.getCause() + " - " + e.getMessage());
    }

    LOG.info("Creating Kubernetes App Group for deployment {} with definition:\n{}",
        deployment.getId(), outDepl.getMetadata().getName());
    //  deployment.getId(), group);
    CloudProviderEndpoint cloudProviderEndpoint = deployment.getCloudProviderEndpoint();
    vaultService.getServiceUri()
        .map(URI::toString)
        .ifPresent(cloudProviderEndpoint::setVaultEndpoint);
    //    executeWithClient(cloudProviderEndpoint, requestedWithToken,
    //        client -> client.createGroup(group));
    return true;
  }

  @Override
  public boolean isDeployed(DeploymentMessage deploymentMessage) {
    Deployment deployment = getDeployment(deploymentMessage);

    AppsV1Api app;
    V1Deployment v1Deployment = new V1Deployment();
    CoreV1Api api;
    try {
      app = new AppsV1Api(Config.defaultClient());
      api = new CoreV1Api();

      V1Namespace namespace = api.readNamespaceStatus("default", null);
      v1Deployment = app.readNamespacedDeploymentStatus(deployment.getId(), "default", "true");

      printPodList();
    } catch (ApiException e) {
      LOG.error("Error in doUndeploy:" + e.getCode() + " - " + e.getMessage());
    } catch (IOException e) {
      LOG.error("Error in doUndeploy:" + e.getCause() + " - " + e.getMessage());
    }

    boolean isDeployed =
        v1Deployment.getStatus().getReplicas() == v1Deployment.getStatus().getReadyReplicas();
    LOG.debug("Kubernetes App Group for deployment {} is deployed? {}", deployment.getId(),
        isDeployed);
    return isDeployed;
  }

  @Override
  public void cleanFailedDeploy(DeploymentMessage deploymentMessage) {
    doUndeploy(deploymentMessage);
  }

  @Override
  public boolean doUpdate(DeploymentMessage deploymentMessage, String template) {
    Deployment deployment = getDeployment(deploymentMessage);

    V1Deployment v1Deployment = new V1DeploymentBuilder()
        .withNewMetadata()
          .withName(deployment.getId())
        .endMetadata()
        .build();
    AppsV1Api app;
    CoreV1Api api;
    try {
      app = new AppsV1Api(Config.defaultClient());
      api = new CoreV1Api();

      app.patchNamespacedDeployment(
          deployment.getId(),
          "default",
          v1Deployment,
          "true",
          null,
          null,
          null);
      // app.replaceNamespacedDeployment(name, namespace, body, pretty, dryRun, fieldManager);

    } catch (ApiException e) {
      LOG.error("Error in doUndeploy:" + e.getCode() + " - " + e.getMessage());
    } catch (IOException e) {
      LOG.error("Error in doUndeploy:" + e.getCause() + " - " + e.getMessage());
    }
    throw new UnsupportedOperationException("Marathon app deployments do not support update.");
  }

  @Override
  public void cleanFailedUpdate(DeploymentMessage deploymentMessage) {
    doUndeploy(deploymentMessage);
  }

  @Override
  public boolean doUndeploy(DeploymentMessage deploymentMessage) {

    Deployment deployment = getDeployment(deploymentMessage);
    AppsV1Api app;
    try {
      app = new AppsV1Api(Config.defaultClient());

      ApiResponse<V1Status> response = app.deleteNamespacedDeploymentWithHttpInfo(
          deployment.getId(),
          "default",
          "true",
          null,
          null,
          null,
          null,
          null);
      V1Status status = app.deleteNamespacedDeployment(
          deployment.getId(),
          "default",
          "true",
          null,
          null,
          null,
          null,
          null);

      response.getStatusCode();
      LOG.debug("Deleting deployment exited with :"
          + status.getCode()
          + " - "
          + status.getMessage()
          + " - "
          + status.getStatus());

    } catch (ApiException e) {
      LOG.error("Error in doUndeploy:" + e.getCode() + " - " + e.getMessage());
      // TODO manage throwing errorCode exception
      //      if(e.getCode()!=404) {
      //        throw new HttpResponseException(e.getCode(), "KubernetesApiException");
      //      }
    } catch (IOException e) {
      LOG.error("Error in doUndeploy:" + e.getCause() + " - " + e.getMessage());
    }
    return true;
  }

  @Override
  public boolean isUndeployed(DeploymentMessage deploymentMessage) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void doProviderTimeout(DeploymentMessage deploymentMessage) {
    throw new BusinessWorkflowException(ErrorCode.CLOUD_PROVIDER_ERROR,
        "Error executing request to Kubernetes",
        new DeploymentException("Kubernetes provider timeout during deployment"));
  }

  @Override
  protected Optional<String> getAdditionalErrorInfoInternal(DeploymentMessage deploymentMessage) {
    Deployment deployment = getDeployment(deploymentMessage);

    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();

    List<CloudProviderEndpoint> cloudProviderEndpoints =
        deployment.getCloudProviderEndpoint().getAllCloudProviderEndpoint();
    //TODO
    return Optional.empty();
  }

  private void printPodList() throws ApiException {
    CoreV1Api api = new CoreV1Api();
    V1PodList list = api.listPodForAllNamespaces(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
    for (V1Pod item : list.getItems()) {
      System.out.println(item.getMetadata().getName());
    }
  }

}
