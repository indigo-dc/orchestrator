/*
 * Copyright © 2015-2017 Santer Reply S.p.A.
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

import com.google.common.primitives.Ints;

import alien4cloud.model.components.ComplexPropertyValue;
import alien4cloud.model.components.ListPropertyValue;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.normative.IntegerType;
import alien4cloud.tosca.normative.InvalidPropertyValueException;
import alien4cloud.tosca.normative.StringType;
import alien4cloud.tosca.parser.ParsingException;

import it.reply.orchestrator.annotation.DeploymentProviderQualifier;
import it.reply.orchestrator.config.properties.MarathonProperties;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.mesos.MesosContainer;
import it.reply.orchestrator.dto.mesos.MesosPortMapping;
import it.reply.orchestrator.dto.mesos.MesosPortMapping.Protocol;
import it.reply.orchestrator.dto.mesos.marathon.MarathonApp;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.exception.service.ToscaException;
import it.reply.orchestrator.service.ToscaService;
import it.reply.orchestrator.utils.CommonUtils;
import it.reply.orchestrator.utils.EnumUtils;

import lombok.extern.slf4j.Slf4j;

import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.MarathonClient;
import mesosphere.marathon.client.MarathonException;
import mesosphere.marathon.client.model.v2.App;
import mesosphere.marathon.client.model.v2.Container;
import mesosphere.marathon.client.model.v2.Docker;
import mesosphere.marathon.client.model.v2.ExternalVolume;
import mesosphere.marathon.client.model.v2.Group;
import mesosphere.marathon.client.model.v2.HealthCheck;
import mesosphere.marathon.client.model.v2.LocalVolume;
import mesosphere.marathon.client.model.v2.Port;
import mesosphere.marathon.client.model.v2.Volume;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@DeploymentProviderQualifier(DeploymentProvider.MARATHON)
@EnableConfigurationProperties(MarathonProperties.class)
@Slf4j
public class MarathonServiceImpl extends AbstractMesosDeploymentService<MarathonApp, App> {

  private ToscaService toscaService;

  private MarathonProperties marathonProperties;

  @Autowired
  public MarathonServiceImpl(ToscaService toscaService, MarathonProperties marathonProperties) {
    this.toscaService = toscaService;
    this.marathonProperties = marathonProperties;
  }

  protected Marathon getMarathonClient() {
    LOG.info("Generating Marathon client with parameters: {}", marathonProperties);
    return MarathonClient.getInstanceWithBasicAuth(marathonProperties.getUrl(),
        marathonProperties.getUsername(), marathonProperties.getPassword());
  }

  protected Group createGroup(Deployment deployment)
      throws ToscaException, ParsingException, IOException, InvalidPropertyValueException {
    ArchiveRoot ar =
        toscaService.prepareTemplate(deployment.getTemplate(), deployment.getParameters());
    Map<String, NodeTemplate> nodes = ar.getTopology().getNodeTemplates();

    // don't check for cycles, already validated at web-service time
    DirectedMultigraph<NodeTemplate, RelationshipTemplate> graph =
        toscaService.buildNodeGraph(nodes, false);

    TopologicalOrderIterator<NodeTemplate, RelationshipTemplate> orderIterator =
        new TopologicalOrderIterator<>(graph);

    List<NodeTemplate> orderedMarathonApps = CommonUtils.iteratorToStream(orderIterator)
        .filter(node -> MarathonApp.TOSCA_NODE_NAME.equals(node.getType()))
        .collect(Collectors.toList());

    Map<String, Resource> resources = deployment.getResources()
        .stream()
        .filter(resource -> MarathonApp.TOSCA_NODE_NAME.equals(resource.getToscaNodeType()))
        .collect(Collectors.toMap(Resource::getToscaNodeName, Function.identity()));

    Group group = new Group();
    List<App> apps = new ArrayList<>();
    for (NodeTemplate marathonNode : orderedMarathonApps) {
      Resource appResource = resources.get(marathonNode.getName());
      String id = appResource.getIaasId();
      if (id == null) {
        id = UUID.randomUUID().toString();
        appResource.setIaasId(id);
      }
      MarathonApp marathonTask = buildTask(graph, marathonNode, id);
      App marathonApp = generateExternalTaskRepresentation(marathonTask);
      apps.add(marathonApp);
    }
    group.setApps(apps);
    return group;
  }

  @Override
  public boolean doDeploy(DeploymentMessage deploymentMessage) {
    Deployment deployment = deploymentMessage.getDeployment();

    try {

      Group group = createGroup(deployment);

      group.setId(UUID.randomUUID().toString());

      LOG.info("Creating Marathon App Group for deployment {} with definition:\n{}",
          deployment.getId(), group);
      getMarathonClient().createGroup(group);
      deployment.setEndpoint(group.getId());
      return true;

    } catch (ToscaException | ParsingException | IOException | InvalidPropertyValueException
        | MarathonException ex) {
      LOG.error("Failed to deploy Marathon apps <{}>", deployment.getId(), ex);
      updateOnError(deployment.getId(), ex);
      return false;
    }
  }

  @Override
  public boolean isDeployed(DeploymentMessage deploymentMessage) throws DeploymentException {
    Deployment deployment = deploymentMessage.getDeployment();
    try {
      String groupId = deployment.getEndpoint();
      // final Marathon client = getMarathonClient();
      // Group group = client.getGroup(groupId);
      // TMP TODO remove it and use an use an update marathon version
      Group group = this.getPolulatedGroup(groupId);
      ///////////////////////////////////////////////////////////////
      Collection<App> apps = Optional.ofNullable(group.getApps()).orElse(new ArrayList<>());
      LOG.debug("Marathon App Group for deployment {} current status:\n{}", deployment.getId(),
          group);

      // if no Mesos deployments are left -> deploy is done
      boolean isDeployed = apps.stream().allMatch(app -> isAppDeployed(app));
      LOG.debug("Marathon App Group for deployment {} is deployed? {}", deployment.getId(),
          isDeployed);
      return isDeployed;
    } catch (Exception ex) {
      LOG.error("Error polling Marathon for deployment <{}>", deployment.getId(), ex);
      updateOnError(deployment.getId(), ex);
      return false;
    }
  }

  @Deprecated
  private Group getPolulatedGroup(String groupId) throws MarathonException {
    final Marathon client = getMarathonClient();
    Group group = client.getGroup(groupId);
    // LOG.debug("Marathon App Group for deployment {} current status:\n{}", deployment.getId(),
    // group);
    Collection<App> apps = Optional.ofNullable(group.getApps()).orElse(Collections.emptyList());

    List<App> completeInfoApps = new ArrayList<>();
    for (App app : apps) {
      completeInfoApps.add(client.getApp(app.getId()).getApp());
    }
    apps = completeInfoApps;

    group.setApps(apps);
    return group;
  }

  private boolean isAppDeployed(App app) {
    boolean noDeploymentsLeft = CollectionUtils.isEmpty(app.getDeployments());
    boolean allTasksReady;
    List<HealthCheck> healtchecks =
        Optional.ofNullable(app.getHealthChecks()).orElse(Collections.emptyList());
    if (healtchecks.isEmpty()) {
      // if no health-checks are defined -> we're good if they are running
      allTasksReady = ObjectUtils.compare(app.getInstances(), app.getTasksRunning(), false) <= 0;
    } else {
      allTasksReady = ObjectUtils.compare(app.getInstances(), app.getTasksHealthy(), false) <= 0;
    }

    return noDeploymentsLeft && allTasksReady;
  }

  @Override
  public void finalizeDeploy(DeploymentMessage deploymentMessage, boolean deployed) {
    if (deployed) {
      try {

        // Update deployment status
        updateOnSuccess(deploymentMessage.getDeploymentId());
      } catch (Exception ex) {
        LOG.error("Error finalizing deployment", ex);
        // Update deployment status
        updateOnError(deploymentMessage.getDeploymentId(), ex);
      }
    } else {
      try {
        Deployment deployment = deploymentMessage.getDeployment();
        String prefix = String.format("/%s/", deployment.getEndpoint());
        Map<String, Resource> resources = deployment.getResources()
            .stream()
            .filter(resource -> MarathonApp.TOSCA_NODE_NAME.equals(resource.getToscaNodeType()))
            .collect(
                Collectors.toMap(resource -> prefix + resource.getIaasId(), Function.identity()));

        String groupId = deployment.getEndpoint();
        Group group = this.getPolulatedGroup(groupId);

        List<App> failedApps = Optional.ofNullable(group.getApps())
            .orElse(Collections.emptyList())
            .stream()
            .filter(app -> !this.isAppDeployed(app))
            .collect(Collectors.toList());

        List<String> failedAppsMessage = new ArrayList<>();
        for (App app : failedApps) {
          Optional.ofNullable(app.getLastTaskFailure())
              .map(failure -> failure.getMessage())
              .ifPresent(appMessage -> {
                Resource resource = resources.get(app.getId());
                failedAppsMessage.add(String.format(
                    "%n - App <%s> with id <%s> (Marathon id %s): %s", resource.getToscaNodeName(),
                    resource.getId(), resource.getIaasId(), appMessage));
              });
        }

        if (failedAppsMessage.isEmpty()) {
          updateOnError(deploymentMessage.getDeploymentId());
        } else {
          StringBuilder message = new StringBuilder("Some Application failed:");
          failedAppsMessage.forEach(message::append);
          updateOnError(deploymentMessage.getDeploymentId(), message.toString());
        }
      } catch (Exception ex) {
        LOG.error("Error finalizing deployment", ex);
        updateOnError(deploymentMessage.getDeploymentId());
      }
    }

    // TODO (?) Update resources attributes on DB?
  }

  @Override
  public boolean doUpdate(DeploymentMessage deploymentMessage, String template) {
    throw new UnsupportedOperationException("Marathon app deployments do not support update.");
  }

  @Override
  public boolean doUndeploy(DeploymentMessage deploymentMessage) {
    Deployment deployment = deploymentMessage.getDeployment();
    try {
      String groupId = deployment.getEndpoint();
      getMarathonClient().deleteGroup(groupId, true);
      return true;
    } catch (Exception ex) {
      if (ex instanceof MarathonException
          && HttpStatus.NOT_FOUND.value() == ((MarathonException) ex).getStatus()) {
        LOG.info("Marathon Group <{}> of deployment <{}> was already undeployed",
            deployment.getEndpoint(), deployment.getId());
        return true;
      }
      LOG.error("Failed to delete Marathon deployment <{}>", deployment.getId(), ex);
      updateOnError(deployment.getId(), ex);
      return false;
    }
  }

  @Override
  public boolean isUndeployed(DeploymentMessage deploymentMessage) throws DeploymentException {
    boolean isUndeployed = false;
    Deployment deployment = deploymentMessage.getDeployment();
    String groupId = deployment.getEndpoint();
    try {
      getMarathonClient().getGroup(groupId);
    } catch (MarathonException ex) {
      if (HttpStatus.NOT_FOUND.value() == ex.getStatus()) {
        isUndeployed = true;
      } else {
        LOG.error("Error polling Marathon for deployment <{}>", deployment.getId(), ex);
        updateOnError(deployment.getId(), ex);
      }
    }
    LOG.debug("Marathon App Group for deployment {} is undeployed? {}", deployment.getId(),
        isUndeployed);
    return isUndeployed;

  }

  @Override
  public void finalizeUndeploy(DeploymentMessage deploymentMessage, boolean undeployed) {
    if (undeployed) {
      updateOnSuccess(deploymentMessage.getDeploymentId());
    } else {
      updateOnError(deploymentMessage.getDeploymentId());
    }

    // TODO (?) Update resources attributes on DB?

    return;

  }

  @Override
  protected MarathonApp createInternalTaskRepresentation() {
    return new MarathonApp();
  }

  @Override
  public MarathonApp buildTask(DirectedMultigraph<NodeTemplate, RelationshipTemplate> graph,
      NodeTemplate taskNode, String taskId) throws InvalidPropertyValueException {
    MarathonApp app = super.buildTask(graph, taskNode, taskId);

    Optional<ComplexPropertyValue> envVars =
        toscaService.getTypedNodePropertyByName(taskNode, "labels");

    if (envVars.isPresent()) {
      // Convert Map<String, Object> to Map<String, String>
      app.setLabels(toscaService.parseComplexPropertyValue(envVars.get(),
          value -> ((ScalarPropertyValue) value).getValue()));
    }

    Capability dockerCapability = getHostCapability(graph, taskNode);

    Optional<ListPropertyValue> publishPortsProperty =
        toscaService.getTypedCapabilityPropertyByName(dockerCapability, "publish_ports");
    if (publishPortsProperty.isPresent()) {
      List<MesosPortMapping> portMapping =
          toscaService
              .parseListPropertyValue(publishPortsProperty.get(),
                  item -> (ComplexPropertyValue) item)
              .stream()
              .map(this::generatePortMapping)
              .collect(Collectors.toList());
      app.getContainer()
          .orElseThrow(() -> new RuntimeException(
              "there are ports to publish but no container is present in Marathon DTO"))
          .setPortMappings(portMapping);
    }
    return app;
  }

  @Override
  protected App generateExternalTaskRepresentation(MarathonApp marathonTask) {
    App app = new App();
    app.setId(marathonTask.getId());
    app.setCmd(marathonTask.getCmd());
    app.setConstraints(marathonTask.getConstraints());
    app.setCpus(marathonTask.getCpus());
    app.setMem(marathonTask.getMemSize());
    app.setUris(marathonTask.getUris());
    app.setLabels(marathonTask.getLabels());
    marathonTask.getContainer()
        .ifPresent(mesosContainer -> app.setContainer(generateContainer(mesosContainer)));
    //// HARDCODED BITS //////
    app.setInstances(1);
    //////////////////////////
    return app;
  }

  private Container generateContainer(MesosContainer mesosContainer) {
    Container container = new Container();
    if (mesosContainer.getType() == MesosContainer.Type.DOCKER) {
      container.setType(MesosContainer.Type.DOCKER.getName());
      container.setVolumes(mesosContainer.getVolumes()
          .stream()
          .map(this::generateVolume)
          .collect(Collectors.toList()));
      Docker docker = new Docker();
      container.setDocker(docker);
      docker.setImage(mesosContainer.getImage());
      List<Port> ports = mesosContainer.getPortMappings()
          .stream()
          .map(this::generatePort)
          .collect(Collectors.toList());
      docker.setPortMappings(ports);

      //// HARDCODED BITS //////
      docker.setNetwork("BRIDGE");
      docker.setForcePullImage(true);
      //////////////////////////
    } else {
      throw new DeploymentException(
          "Unknown Marathon container type: " + mesosContainer.getType().toString());
    }
    return container;
  }

  private MesosPortMapping generatePortMapping(ComplexPropertyValue portMappingProperties) {
    Map<String, ScalarPropertyValue> values = toscaService
        .parseComplexPropertyValue(portMappingProperties, value -> (ScalarPropertyValue) value);

    try {
      ScalarPropertyValue sourcePortProperty =
          CommonUtils.getFromOptionalMap(values, "source").orElseThrow(
              () -> new ToscaException("source port in 'publish_ports' property must be provided"));

      Long sourcePortVaule =
          toscaService.parseScalarPropertyValue(sourcePortProperty, IntegerType.class);
      MesosPortMapping portMapping = new MesosPortMapping(Ints.checkedCast(sourcePortVaule));

      Optional<ScalarPropertyValue> targetPortProperty =
          CommonUtils.getFromOptionalMap(values, "target");
      if (targetPortProperty.isPresent()) {
        Long targetPortVaule =
            toscaService.parseScalarPropertyValue(targetPortProperty.get(), IntegerType.class);
        portMapping.setServicePort(Ints.checkedCast(targetPortVaule));
      }

      Optional<ScalarPropertyValue> protocolProperty =
          CommonUtils.getFromOptionalMap(values, "protocol");
      if (protocolProperty.isPresent()) {
        String protocolVaule =
            toscaService.parseScalarPropertyValue(protocolProperty.get(), StringType.class);
        Protocol protocol = EnumUtils.fromNameOrThrow(Protocol.class, protocolVaule);
        portMapping.setProtocol(protocol);
      }
      return portMapping;
    } catch (InvalidPropertyValueException ex) {
      throw new ToscaException("Error parsing port mapping", ex);
    }
  }

  private Port generatePort(MesosPortMapping portMapping) {
    Port port = new Port();
    port.setContainerPort(portMapping.getContainerPort());
    port.setProtocol(portMapping.getProtocol().getName());
    Optional.ofNullable(portMapping.getServicePort()).ifPresent(port::setServicePort);
    return port;
  }

  Volume generateVolume(String containerVolumeMount) {

    Supplier<DeploymentException> validationExceptionSupplier = () -> new DeploymentException(String
        .format("Volume mount <%s> not supported for marathon containers", containerVolumeMount));

    if (!containerVolumeMount.matches("([^:]+):([^:]+):([^:]+)(?::([^:]+):([^:]+))?")) {
      throw validationExceptionSupplier.get();
    }

    // split the volumeMount string and extract only the non blank strings
    List<String> volumeMountSegments = Arrays.asList(containerVolumeMount.split(":"))
        .stream()
        .sequential()
        .filter(StringUtils::isNotBlank)
        .collect(Collectors.toList());

    final Volume volume;
    switch (volumeMountSegments.size()) {
      case 3:
        LocalVolume localVolume = new LocalVolume();
        localVolume.setHostPath(volumeMountSegments.get(0));
        volume = localVolume;
        break;
      case 5:
        ExternalVolume externalVolume = new ExternalVolume();
        externalVolume.setName(volumeMountSegments.get(0));
        externalVolume.setProvider(volumeMountSegments.get(3));
        externalVolume.setDriver(volumeMountSegments.get(4));
        volume = externalVolume;
        break;
      default:
        throw validationExceptionSupplier.get();
    }

    volume.setContainerPath(volumeMountSegments.get(1));
    volume.setMode(volumeMountSegments.get(2).toUpperCase(Locale.US));
    return volume;
  }
}
