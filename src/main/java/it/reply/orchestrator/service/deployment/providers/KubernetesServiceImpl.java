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

import com.google.common.collect.MoreCollectors;

import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ContainerBuilder;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentBuilder;
import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1ResourceRequirements;
import io.kubernetes.client.openapi.models.V1ResourceRequirementsBuilder;
import io.kubernetes.client.openapi.models.V1Status;
import io.kubernetes.client.util.Config;

import it.infn.ba.deep.qcg.client.model.Job;
import it.infn.ba.deep.qcg.client.model.JobDescription;
import it.infn.ba.deep.qcg.client.model.JobDescriptionExecution;
import it.infn.ba.deep.qcg.client.model.JobWorkingDirectoryPolicy;
import it.infn.ba.deep.qcg.client.model.RemoveConditionCreateMode;
import it.infn.ba.deep.qcg.client.model.RemoveConditionWhen;
import it.reply.orchestrator.annotation.DeploymentProviderQualifier;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.kubernetes.KubernetesContainer;
import it.reply.orchestrator.dto.kubernetes.KubernetesTask;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.exception.service.BusinessWorkflowException;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.exception.service.ToscaException;
import it.reply.orchestrator.service.IndigoInputsPreProcessorService;
import it.reply.orchestrator.service.IndigoInputsPreProcessorService.RuntimeProperties;
import it.reply.orchestrator.service.deployment.providers.QcgServiceImpl.DeepJob;
import it.reply.orchestrator.service.ToscaService;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.orchestrator.utils.CommonUtils;
import it.reply.orchestrator.utils.OneDataUtils;
import it.reply.orchestrator.utils.ToscaConstants;
import it.reply.orchestrator.utils.ToscaUtils;
import it.reply.orchestrator.utils.WorkflowConstants.ErrorCode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.normative.types.BooleanType;
import org.alien4cloud.tosca.normative.types.FloatType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@DeploymentProviderQualifier(DeploymentProvider.KUBERNETES)
@Slf4j
public class KubernetesServiceImpl extends AbstractDeploymentProviderService {

  //  @Autowired
  //  private KubernetesClientFactory kubernetesClientFactory;

  @Autowired
  private IndigoInputsPreProcessorService indigoInputsPreProcessorService;

  @Autowired
  private ToscaService toscaService;

  @Autowired
  private OAuth2TokenService oauth2TokenService;

  @Autowired
  private ResourceRepository resourceRepository;

  private static CoreV1Api COREV1_API;
  
  private static final String HOST_CAPABILITY_NAME = "host";

  //  protected <R> R executeWithClientForResult(CloudProviderEndpoint cloudProviderEndpoint,
  //      @Nullable OidcTokenId requestedWithToken,
  //      ThrowingFunction<ApiClient, R, ApiException> function) throws ApiException {
  //    return oauth2TokenService.executeWithClientForResult(requestedWithToken,
  //        token -> function.apply(kubernetesClientFactory.build(cloudProviderEndpoint, token)),
  //        ex -> ex instanceof ApiException && ((ApiException) ex).getCode() == 401);
  //  }
  //
  //  protected void executeWithClient(CloudProviderEndpoint cloudProviderEndpoint,
  //      @Nullable OidcTokenId requestedWithToken,
  //      ThrowingConsumer<ApiClient, ApiException> consumer) throws ApiException {
  //    executeWithClientForResult(cloudProviderEndpoint, requestedWithToken,
  //        client -> consumer.asFunction().apply(client));
  //  }

  //TODO if needed
  protected ArchiveRoot prepareTemplate(Deployment deployment,
      DeploymentMessage deploymentMessage) {
    RuntimeProperties runtimeProperties =
        OneDataUtils.getOneDataRuntimeProperties(deploymentMessage);
    Map<String, Object> inputs = deployment.getParameters();
    ArchiveRoot ar = toscaService.parseAndValidateTemplate(deployment.getTemplate(), inputs);
    if (runtimeProperties.getVaules().size() > 0) {
      indigoInputsPreProcessorService.processGetInputAttributes(ar, inputs, runtimeProperties);
    } else {
      indigoInputsPreProcessorService.processGetInput(ar, inputs);
    }
    return ar;
  }

  protected List<DeepKubernetesContainer> getContanersList(DeploymentMessage deploymentMessage,
      OidcTokenId requestedWithToken) {
    Deployment deployment = getDeployment(deploymentMessage);
    ArchiveRoot ar = prepareTemplate(deployment, deploymentMessage);

    Map<String, NodeTemplate> nodes = Optional
        .ofNullable(ar.getTopology())
        .map(Topology::getNodeTemplates)
        .orElseGet(HashMap::new);

    DirectedMultigraph<NodeTemplate, RelationshipTemplate> graph =
        toscaService.buildNodeGraph(nodes, false);

    TopologicalOrderIterator<NodeTemplate, RelationshipTemplate> orderIterator =
        new TopologicalOrderIterator<>(graph);

    List<NodeTemplate> orderedKubernetesApps = CommonUtils
        .iteratorToStream(orderIterator)
        .filter(node -> toscaService.isOfToscaType(node, ToscaConstants.Nodes.Types.KUBERNETES))
        .collect(Collectors.toList());

    Map<String, Resource> resources = deployment.getResources().stream()
        .filter(resource -> toscaService.isOfToscaType(resource,
            ToscaConstants.Nodes.Types.KUBERNETES))
        .collect(Collectors.toMap(Resource::getToscaNodeName, res -> res));

    LinkedHashMap<String, KubernetesTask> containersByKuberNode = new LinkedHashMap<String, KubernetesTask>();

    List<DeepKubernetesContainer> containersList = new ArrayList<>();
    
    for (NodeTemplate kuberNode : orderedKubernetesApps) {

      Resource kuberResource = resources.get(kuberNode.getName());
      String id = Optional.ofNullable(kuberResource.getIaasId()).orElseGet(() -> {
        kuberResource.setIaasId(kuberResource.getId());
        return kuberResource.getIaasId();
      });
      
      
      KubernetesTask kuberTask = buildTask(graph, kuberNode, id);
      containersByKuberNode.put(kuberNode.getName(), kuberTask);

      V1Deployment kubernetesContainer = generateExternalTaskRepresentation(kuberTask);
      DeepKubernetesContainer deepV1Container = new DeepKubernetesContainer(kubernetesContainer, kuberNode.getName());
      containersList.add(deepV1Container);
      
      List<Resource> requestsRes = resourceRepository
          .findByToscaNodeNameAndDeployment_id(kuberNode.getName(), deployment.getId());

    }

    return containersList;
  }
  
  /**
   * Build a Kubernetes task object.
   * @param graph the input nodegraph.
   * @param taskNode the input tasknode.
   * @param taskId the input taskid.
   * @return the KubernetesTask.
   */
  public KubernetesTask buildTask(DirectedMultigraph<NodeTemplate, RelationshipTemplate> graph,
      NodeTemplate taskNode, String taskId) {

    KubernetesTask kubernetesTask = new KubernetesTask();

    // orchestrator internal
    kubernetesTask.setId(taskId);

    // TODO MAP ALL PROPETIES FROM TOSCA

    Capability containerCapability = getHostCapability(graph, taskNode);
    
    ToscaUtils
      .extractScalar(containerCapability.getProperties(), "num_cpus", FloatType.class)
      .ifPresent(kubernetesTask::setCpus);


    return kubernetesTask;
  }
  
  protected V1Deployment generateExternalTaskRepresentation(KubernetesTask kubernetesTask) {

    V1Deployment v1Deployment = new V1Deployment();

    v1Deployment.setApiVersion("apps/v1");
    //(kubernetesTask.getId());
//    if (qcgjob.getAttributes() != null) {
//      job.setAttributes((HashMap<String, String>) ((HashMap<String, String>)
//          qcgjob.getAttributes()).clone());
//    }
//    job.setUser(qcgjob.getUser());
//    job.setState(qcgjob.getState());
//    job.setOperation(qcgjob.getOperation());
//    job.setNote(qcgjob.getNote());

    return v1Deployment;
  }

  private AppsV1Api connectApi(DeploymentMessage deploymentMessage) throws IOException {
    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
    String accessToken = oauth2TokenService
        .getAccessToken(CommonUtils.checkNotNull(requestedWithToken));

    ApiClient client = Config.fromToken(
        deploymentMessage.getChosenCloudProviderEndpoint().getCpEndpoint(), accessToken);
    if (client == null) {
      client = Config.defaultClient();
    }
    return new AppsV1Api(client);
    //TODO manage ApiException or throw some exception
  }

  private V1Deployment createV1Deployment(DeploymentMessage deploymentMessage) {
    /* note , cpu and ram are shared between all containers in the pod
     * so it is enought difine it once*/

    //TODO manage needed field for deployment and get them from DeploymentMessage
    List<V1Container> containers = new ArrayList<>();

    Map<String, Quantity> requestsRes = new HashMap<String, Quantity>();
    /*The expression 0.1 is equivalent to the expression 100m,
     *which can be read as “one hundred millicpu”.*/
    requestsRes.put("cpu", new Quantity("32Mi"));
    requestsRes.put("memory", new Quantity("100m"));

    Map<String, Quantity> limitRes = new HashMap<String, Quantity>();
    limitRes.put("cpu", new Quantity("64Mi"));
    limitRes.put("memory", new Quantity("200m"));

    V1ResourceRequirements resources = new V1ResourceRequirementsBuilder()
        .withRequests(requestsRes)
        .withLimits(limitRes)
        .build();

    V1DeploymentSpec spec = new V1DeploymentSpec();
    V1Container cont = new V1ContainerBuilder()
        .withName("nginx")
        .withImage("nginx:1.7.9")
        .withResources(resources)
        .addNewPort()
        .withContainerPort(80)
        .endPort()
        .build();

    Deployment deployment = getDeployment(deploymentMessage);

    V1Deployment v1Deployment = new V1DeploymentBuilder()
        .withApiVersion("apps/v1")
        .withKind("Deployment")
        .withNewMetadata()
            .withName(deployment.getId())
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
                    .withContainers(cont)
                .endSpec()
            .endTemplate()
        .endSpec()
        .build();

    return v1Deployment;
  }

  @Override
  public boolean doDeploy(DeploymentMessage deploymentMessage) {

    V1Deployment v1Deployment = createV1Deployment(deploymentMessage);

    try {
      AppsV1Api app = connectApi(deploymentMessage);
      app.createNamespacedDeployment(
          "default",
          v1Deployment,
          "true",
          null,
          null);

    } catch (ApiException e) {
      LOG.error("Error in doDeploy:" + e.getCode() + " - " + e.getMessage());
    } catch (IOException e) {
      LOG.error("Error in doDeploy:" + e.getCause() + " - " + e.getMessage());
    }
    LOG.info("Creating Kubernetes V1Deployment for deployment {} with definition:\n{}",
        deploymentMessage.getDeploymentId(), v1Deployment.getMetadata().getName());
    return true;
  }

  @Override
  public boolean isDeployed(DeploymentMessage deploymentMessage) {
    Deployment deployment = getDeployment(deploymentMessage);

    AppsV1Api app;
    V1Deployment v1Deployment = new V1Deployment();
    try {
      app = new AppsV1Api(Config.defaultClient());

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
    if (!isDeployed) {
      LOG.warn(v1Deployment.getStatus().getConditions().get(0).getMessage());
    }
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

      //      ApiResponse<V1Status> response = app.deleteNamespacedDeploymentWithHttpInfo(
      //          deployment.getId(),
      //          "default",
      //          "true",
      //          null,
      //          null,
      //          null,
      //          null,
      //          null);
      V1Status status = app.deleteNamespacedDeployment(
          deployment.getId(),
          "default",
          "true",
          null,
          null,
          null,
          null,
          null);

      //response.getStatusCode();
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
    COREV1_API = new CoreV1Api();
    V1PodList list =  COREV1_API.listPodForAllNamespaces(
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
  
  @Data
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  @RequiredArgsConstructor
  public static class DeepKubernetesContainer {

    @NonNull
    @NotNull
    private V1Deployment v1Deployment;

    @NonNull
    @NotNull
    private String toscaNodeName;

  }
  
  protected Capability getHostCapability(
      DirectedMultigraph<NodeTemplate, RelationshipTemplate> graph, NodeTemplate taskNode) {

    NodeTemplate hostNode = getHostNode(graph, taskNode);

    // at this point we're sure that it exists
    return hostNode.getCapabilities().get(HOST_CAPABILITY_NAME);
  }
  
  protected NodeTemplate getHostNode(DirectedMultigraph<NodeTemplate, RelationshipTemplate> graph,
      NodeTemplate taskNode) {
    return graph
        .incomingEdgesOf(taskNode)
        .stream()
        .filter(
            relationship -> HOST_CAPABILITY_NAME.equals(relationship.getTargetedCapabilityName()))
        .map(graph::getEdgeSource)
        // if more than 1 node is present -> IllegalArgumentException
        .collect(MoreCollectors.toOptional())
        .orElseThrow(() -> new IllegalArgumentException(
            String.format("No hosting node provided for node <%s>", taskNode.getName())));
  }

}
