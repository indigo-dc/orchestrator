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
import com.google.common.primitives.Ints;

import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.custom.Quantity.Format;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.auth.ApiKeyAuth;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ContainerBuilder;
import io.kubernetes.client.openapi.models.V1ContainerPort;
import io.kubernetes.client.openapi.models.V1ContainerPortBuilder;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentBuilder;
import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1ResourceRequirements;
import io.kubernetes.client.openapi.models.V1ResourceRequirementsBuilder;
import io.kubernetes.client.openapi.models.V1Status;
import io.kubernetes.client.util.Config;

import it.reply.orchestrator.annotation.DeploymentProviderQualifier;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.kubernetes.KubernetesContainer;
import it.reply.orchestrator.dto.kubernetes.KubernetesPortMapping;
import it.reply.orchestrator.dto.kubernetes.KubernetesPortMapping.Protocol;
import it.reply.orchestrator.dto.kubernetes.KubernetesTask;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.exception.service.BusinessWorkflowException;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.exception.service.KubernetesException;
import it.reply.orchestrator.exception.service.ToscaException;
import it.reply.orchestrator.function.ThrowingConsumer;
import it.reply.orchestrator.function.ThrowingFunction;
import it.reply.orchestrator.service.IndigoInputsPreProcessorService;
import it.reply.orchestrator.service.IndigoInputsPreProcessorService.RuntimeProperties;
import it.reply.orchestrator.service.ToscaService;
import it.reply.orchestrator.service.deployment.providers.factory.KubernetesClientFactory;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.orchestrator.utils.CommonUtils;
import it.reply.orchestrator.utils.EnumUtils;
import it.reply.orchestrator.utils.OneDataUtils;
import it.reply.orchestrator.utils.ToscaConstants;
import it.reply.orchestrator.utils.ToscaUtils;
import it.reply.orchestrator.utils.WorkflowConstants.ErrorCode;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.normative.types.FloatType;
import org.alien4cloud.tosca.normative.types.IntegerType;
import org.alien4cloud.tosca.normative.types.SizeType;
import org.alien4cloud.tosca.normative.types.StringType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@DeploymentProviderQualifier(DeploymentProvider.KUBERNETES)
@Slf4j
public class KubernetesServiceImpl extends AbstractDeploymentProviderService {

  @Autowired
  private IndigoInputsPreProcessorService indigoInputsPreProcessorService;

  @Autowired
  private ToscaService toscaService;

  @Autowired
  private OAuth2TokenService oauth2TokenService;

  @Autowired
  private ResourceRepository resourceRepository;
  
  @Autowired
  private KubernetesClientFactory kubernetesClientFactory;

  private static CoreV1Api COREV1_API;

  private static final String HOST_CAPABILITY_NAME = "host";

  protected AppsV1Api getClient(CloudProviderEndpoint cloudProviderEndpoint,
      @Nullable OidcTokenId requestedWithToken) {
    String accessToken = oauth2TokenService.getAccessToken(requestedWithToken);
    AppsV1Api outClient = null;
    try {
      outClient = kubernetesClientFactory.build(cloudProviderEndpoint, accessToken);
    } catch (IOException e) {
      LOG.error("Error in doDeploy:" + e.getCause() + " - " + e.getMessage());
      throw new KubernetesException("Error in doDeploy:"
          + e.getCause() + " - " + e.getMessage(), e);
    }
    return outClient;
  }

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

  protected V1Deployment createV1Deployment(DeploymentMessage deploymentMessage) {
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

    //    Map<String, Resource> resources = deployment.getResources().stream()
    //        .filter(resource -> toscaService.isOfToscaType(resource,
    //            ToscaConstants.Nodes.Types.KUBERNETES))
    //        .collect(Collectors.toMap(Resource::getToscaNodeName, res -> res));

    LinkedHashMap<String, KubernetesTask> containersByKuberNode =
        new LinkedHashMap<String, KubernetesTask>();

    List<DeepKubernetesDeployment> deploymentList = new ArrayList<>();

    V1Deployment kubernetesDeployment = new V1Deployment();

    for (NodeTemplate kuberNode : orderedKubernetesApps) {

      KubernetesTask kuberTask = buildTask(graph, kuberNode, deployment.getId());

      List<Resource> resources =  resourceRepository
              .findByToscaNodeNameAndDeployment_id(kuberNode.getName(), deployment.getId());

      resources.forEach(resource -> resource.setIaasId(kuberTask.getId()));
      kuberTask.setInstances(resources.size());

      // Resource kuberResource = resources.get(resources.indexOf(kuberNode.getName()));
      Resource kuberResource = resources.stream()
          .filter(resource -> kuberNode.getName().equals(resource.getToscaNodeName()))
          .findAny()
          .orElse(null);

      String id = Optional.ofNullable(kuberResource.getIaasId()).orElseGet(() -> {
        kuberResource.setIaasId(kuberResource.getId());
        return kuberResource.getIaasId();
      });

      //TODO Check what id it is
      containersByKuberNode.put(kuberNode.getName(), kuberTask);

      kubernetesDeployment = generateExternalTaskRepresentation(kuberTask, deployment.getId());

      DeepKubernetesDeployment deepV1Deployment =
          new DeepKubernetesDeployment(kubernetesDeployment, kuberNode.getName());
      deploymentList.add(deepV1Deployment);

    }

    return kubernetesDeployment;
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
      .extractList(containerCapability.getProperties(),
          "container",
          KubernetesContainer.class::cast)
      .ifPresent(kubernetesTask::setContainers);

    DeploymentArtifact image = toscaService
        .getNodeArtifactByName(taskNode, "image")
        .orElseThrow(() -> new IllegalArgumentException(
            String.format("<image> artifact not found in node <%s> of type <%s>",
                taskNode.getName(), taskNode.getType())));
    // <image> artifact type check
    List<String> supportedTypes = EnumUtils
        .toList(KubernetesContainer.Type.class, KubernetesContainer.Type::getToscaName);

    KubernetesContainer.Type containerType = EnumUtils
        .fromPredicate(KubernetesContainer.Type.class,
            enumItem -> enumItem.getToscaName().equals(image.getArtifactType()))
        .orElseThrow(() -> new IllegalArgumentException(String.format(
            "Unsupported artifact type for <image> artifact in node <%s> of type <%s>."
                + " Given <%s>, supported <%s>",
            taskNode, taskNode.getType(), image.getArtifactType(), supportedTypes)));

    KubernetesContainer container = new KubernetesContainer(containerType);

    String imageName = Optional
        .ofNullable(image.getArtifactRef())
        .orElseThrow(() ->
            new IllegalArgumentException(
                "<file> field for <image> artifact in node <" + taskNode.getName()
                    + "> must be provided")
        );

    container.setImage(imageName);
    
    
    
    
    kubernetesTask.setContainer(container);

    /* TODO
     * cpu and memory in Kubernetes Container are rappresented as Quantity.class,
     *
     * The expression 0.1 is equivalent to the expression 100m,
     * which can be read as “one hundred millicpu”. */

    ToscaUtils
      .extractScalar(containerCapability.getProperties(), "num_cpus", FloatType.class)
      .ifPresent(kubernetesTask::setCpu);

    /* Limits and requests for memory are measured in bytes.
     * You can express memory as a plain integer or as a fixed-point integer
     * using one of these suffixes: E, P, T, G, M, K.
     * You can also use the power-of-two equivalents: Ei, Pi, Ti, Gi, Mi, Ki.*/

    ToscaUtils
      .extractScalar(containerCapability.getProperties(), "mem_size", SizeType.class)
      .map(memSize -> memSize.convert("MB"))
      .ifPresent(kubernetesTask::setMemory);

    
    ToscaUtils
      .extractList(containerCapability.getProperties(), "volumes", String.class::cast)
      .ifPresent(kubernetesTask::setVolumes);

    kubernetesTask.setReplicas(
        ToscaUtils
          .extractScalar(containerCapability.getProperties(), "replicas", FloatType.class)
          .orElse(1.0));//ifPresent(kubernetesTask::setReplicas);
    
    ToscaUtils
      .extractList(containerCapability.getProperties(), "publish_ports", l ->
          this.generatePortMapping((Map<String, Object>) l)
      )
      .ifPresent(portMappings -> kubernetesTask
          .getContainer()
          .orElseThrow(
              () -> new RuntimeException(
                  "there are ports to publish but no container is present"))
          .setPortMappings(portMappings));

    return kubernetesTask;
  }
  
  protected KubernetesPortMapping generatePortMapping(Map<String, Object> portMappingProperties) {

    int sourcePortValue = CommonUtils
        .getFromOptionalMap(portMappingProperties, "source")
        .map(value -> ToscaUtils.parseScalar((String) value, IntegerType.class))
        .map(Ints::checkedCast)
        .orElseThrow(() -> new ToscaException(
            "source port in 'publish_ports' property must be provided"));

    KubernetesPortMapping portMapping = new KubernetesPortMapping(sourcePortValue);

    CommonUtils.getFromOptionalMap(portMappingProperties, "target")
        .map(value -> ToscaUtils.parseScalar((String) value, IntegerType.class))
        .map(Ints::checkedCast)
        .ifPresent(portMapping::setServicePort);

    CommonUtils.getFromOptionalMap(portMappingProperties, "protocol")
        .map(value -> EnumUtils.fromNameOrThrow(Protocol.class, (String) value))
        .ifPresent(portMapping::setProtocol);

    return portMapping;
  }

  protected V1Deployment generateExternalTaskRepresentation(KubernetesTask kubernetesTask,
      String deploymentId) {

    // V1Deployment v1Deployment = new V1Deployment();

    //    v1Deployment.setApiVersion("apps/v1");
    //    v1Deployment.setKind("Deployment");

    Map<String, Quantity> requestsRes = new HashMap<String, Quantity>();

    String mem = kubernetesTask.getMemory().toString();//.replace("Mb", "M").replaceAll(" ", "");

    //TODO cpu and ram to string and not quantity maybe
    requestsRes.put("cpu", new Quantity(new BigDecimal(kubernetesTask.getCpu()), Format.DECIMAL_SI));
    requestsRes.put("memory", new Quantity(new BigDecimal(kubernetesTask.getMemory()), Format.DECIMAL_SI));

    List<V1Container> v1Containers = new ArrayList<V1Container>();
    if (kubernetesTask.getContainers() == null || kubernetesTask.getContainers().isEmpty()) {
      V1Container contV1 = new V1ContainerBuilder()
          .withName(kubernetesTask.getToscaNodeName()+"_"+kubernetesTask.getId())
          .withNewResources()
            .withRequests(requestsRes)
          .endResources()
          .build();
      setContainerPorts(contV1, kubernetesTask.getContainer().get().getPortMappings());
      v1Containers.add(contV1);
    } else {
      for (KubernetesContainer cont : kubernetesTask.getContainers()) {
        V1Container contV1 = new V1ContainerBuilder()
            .withName(cont.getType().getName())
            .withImage(cont.getImage())
            .withNewResources()
              .withRequests(requestsRes)
            .endResources()
            //.addNewPort()
            //  .withHostPort(1)//targhet port optional
            //  .withContainerPort(cont.getPort().intValue())//source port 
            //.endPort()
            .build();
        setContainerPorts(contV1, cont.getPortMappings());
        v1Containers.add(contV1);
      }
    }

    V1Deployment v1Deployment = new V1DeploymentBuilder()
        .withApiVersion("apps/v1")
        .withKind("Deployment")
        .withNewMetadata()
            .withName(kubernetesTask.getId())
        .endMetadata()
        .withNewSpec()
            .withReplicas(kubernetesTask.getReplicas().intValue()) /*TODO check if good value*/
            .withNewSelector()
                .addToMatchLabels("app", kubernetesTask.getToscaNodeName())
            .endSelector()
            .withNewTemplate()
                .withNewMetadata()
                    .addToLabels("app", kubernetesTask.getToscaNodeName())
                .endMetadata()
                .withNewSpec()
                  .addAllToContainers(v1Containers)
                .endSpec()
            .endTemplate()
        .endSpec()
        .build();

    return v1Deployment;
  }
  
  private void setContainerPorts(V1Container contV1, List<KubernetesPortMapping> portMappings) {
    for(KubernetesPortMapping kubPort : portMappings) {
      V1ContainerPort v1ContainerPort = new V1ContainerPortBuilder()
          .withProtocol(kubPort.getProtocol().getName())
          .withContainerPort(kubPort.getContainerPort()) //not nullable port
          .withHostPort(kubPort.getServicePort()) //nullable port
          .build();
      contV1.addPortsItem(v1ContainerPort);
    }
  }

  /**
   * Connecting Kubernetes Api config.
   * @param deploymentMessage DeploymentMessage as parameter
   * @return
   */


  @Override
  public boolean doDeploy(DeploymentMessage deploymentMessage) {
    Deployment deployment = getDeployment(deploymentMessage);
    V1Deployment v1Deployment = createV1Deployment(deploymentMessage);
    //V1Deployment v1Deployment = createV1DeploymentForTest(deploymentMessage);

    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
    
//    try {
      AppsV1Api apiClient = getClient(deployment.getCloudProviderEndpoint(), requestedWithToken);

//      V1Deployment depCreated = apiClient.createNamespacedDeployment(
//          "default",
//          v1Deployment,
//          "true",
//          null,
//          null);
//      
//      LOG.debug(depCreated.getStatus().toString());
//
//      //TODO handle exception in out
//    } catch (ApiException e) {
//      LOG.error("Error in doDeploy:" + e.getCode() + " - " + e.getMessage() + e.getResponseBody());
//      throw new DeploymentException("Error in doDeploy:"
//          + e.getCode() + " - " + e.getMessage(), e);
//    }
    LOG.info("Creating Kubernetes V1Deployment for deployment {} with definition:\n{}",
        deploymentMessage.getDeploymentId(), v1Deployment.getMetadata().getName());
    return true;
  }

  @Override
  public boolean isDeployed(DeploymentMessage deploymentMessage) {
    Deployment deployment = getDeployment(deploymentMessage);

    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
    
    AppsV1Api apiClient;
    V1Deployment v1Deployment = new V1Deployment();
    try {
      apiClient = getClient(deployment.getCloudProviderEndpoint(), requestedWithToken);

      v1Deployment = apiClient.readNamespacedDeploymentStatus(deployment.getId(), "default", "true");

      printPodList();
    } catch (ApiException e) {
      LOG.error("Error in isDeployed:" + e.getCode() + " - " + e.getMessage());
      throw new DeploymentException(e.getMessage(), e);
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
    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
    V1Deployment v1Deployment = new V1DeploymentBuilder()
        .withNewMetadata()
          .withName(deployment.getId())
        .endMetadata()
        .build();
    AppsV1Api apiClient;
    try {
      apiClient = getClient(deployment.getCloudProviderEndpoint(), requestedWithToken);
      apiClient.patchNamespacedDeployment(
          deployment.getId(),
          "default",
          v1Deployment,
          "true",
          null,
          null,
          null);
      // apiClient.replaceNamespacedDeployment(name, namespace, body, pretty, dryRun, fieldManager);

    } catch (ApiException e) {
      LOG.error("Error in doUpdate:" + e.getCode() + " - " + e.getMessage());
      throw new DeploymentException(e.getMessage(), e);
    } 
    return true;
  }

  @Override
  public void cleanFailedUpdate(DeploymentMessage deploymentMessage) {
    doUndeploy(deploymentMessage);
  }

  @Override
  public boolean doUndeploy(DeploymentMessage deploymentMessage) {

    Deployment deployment = getDeployment(deploymentMessage);
    CloudProviderEndpoint cloudProviderEndpoint = deployment.getCloudProviderEndpoint();
    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
    if (cloudProviderEndpoint != null) {
      AppsV1Api apiClient;
      try {
        apiClient = getClient(deployment.getCloudProviderEndpoint(), requestedWithToken);
        
        V1Status status = apiClient.deleteNamespacedDeployment(
            deployment.getId(),
            "default",
            "true",
            null,
            null,
            null,
            null,
            null);

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
        throw new DeploymentException("Error in doUndeploy:"
            + e.getCode() + " - " + e.getMessage(), e);
      } 
    }
    return true;
  }

  @Override
  public boolean isUndeployed(DeploymentMessage deploymentMessage) {
    boolean isUndeployed = false;
    Deployment deployment = getDeployment(deploymentMessage);
    final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();

    AppsV1Api apiClient;
    try {
      apiClient = getClient(deployment.getCloudProviderEndpoint(), requestedWithToken);

      V1Deployment deplSimleToCheck = apiClient.readNamespacedDeploymentStatus(
          deployment.getId(),
          "default",
          "true");

      V1Deployment depl = apiClient.readNamespacedDeployment(
          deployment.getId(),
          "default",
          "true",
          null,
          null);

      if (depl == null) {
        isUndeployed = true;
      }

    } catch (ApiException e) {
      LOG.error("Error in doUndeploy:" + e.getCode() + " - " + e.getMessage());
      if (e.getCode() == 404) {
        isUndeployed = true;
      }
      throw new DeploymentException(e.getMessage(), e);
    }

    return isUndeployed;
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
  public static class DeepKubernetesDeployment {

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

  private V1Deployment createV1DeploymentForTest(DeploymentMessage deploymentMessage) {
    /* note , cpu and ram are shared between all containers in the pod
     * so it is enought difine it once*/

    //TODO manage needed field for deployment and get them from DeploymentMessage

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

}



