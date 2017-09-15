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

import com.google.common.collect.MoreCollectors;
import com.google.common.primitives.Ints;

import alien4cloud.model.components.ComplexPropertyValue;
import alien4cloud.model.components.DeploymentArtifact;
import alien4cloud.model.components.ListPropertyValue;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.tosca.normative.BooleanType;
import alien4cloud.tosca.normative.FloatType;
import alien4cloud.tosca.normative.IntegerType;
import alien4cloud.tosca.normative.InvalidPropertyValueException;
import alien4cloud.tosca.normative.Size;
import alien4cloud.tosca.normative.SizeType;
import alien4cloud.tosca.normative.StringType;

import it.reply.orchestrator.dto.mesos.MesosContainer;
import it.reply.orchestrator.dto.mesos.MesosPortMapping;
import it.reply.orchestrator.dto.mesos.MesosPortMapping.Protocol;
import it.reply.orchestrator.dto.mesos.MesosTask;
import it.reply.orchestrator.exception.service.ToscaException;
import it.reply.orchestrator.service.ToscaService;
import it.reply.orchestrator.utils.CommonUtils;
import it.reply.orchestrator.utils.EnumUtils;

import org.jgrapht.graph.DirectedMultigraph;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class AbstractMesosDeploymentService<T extends MesosTask<T>, S extends Object>
    extends AbstractDeploymentProviderService {

  private static final String HOST_CAPABILITY_NAME = "host";

  @Autowired
  private ToscaService toscaService;

  protected abstract T createInternalTaskRepresentation();

  protected abstract S generateExternalTaskRepresentation(T internalTaskRepresentation);

  /**
   * Builds a MesosTask.
   * 
   * @param graph
   *          the graph of with all nodes
   * @param taskNode
   *          the node representing the task
   * @param taskId
   *          the id of the task
   * @return the Mesos task
   * @throws InvalidPropertyValueException
   *           if some TOSCA properties are not of the right type
   */
  public T buildTask(DirectedMultigraph<NodeTemplate, RelationshipTemplate> graph,
      NodeTemplate taskNode, String taskId) {

    T task = createInternalTaskRepresentation();
    task.setId(taskId);

    toscaService
        .<ScalarPropertyValue>getTypedNodePropertyByName(taskNode, "command")
        .map(ScalarPropertyValue::getValue)
        .map(String::trim)
        .ifPresent(task::setCmd);

    if ("".equals(task.getCmd())) { // it must be either null or not empty
      throw new ToscaException(String.format(
          "<command> property of node <%s> must not be an empty string", taskNode.getName()));
    }

    toscaService
        .<ListPropertyValue>getTypedNodePropertyByName(taskNode, "uris")
        .ifPresent(property -> {
          // Convert List<Object> to List<String>
          List<String> uris = toscaService.parseListPropertyValue(property,
              item -> ((ScalarPropertyValue) item).getValue());
          task.setUris(uris);
        });

    toscaService
        .<ComplexPropertyValue>getTypedNodePropertyByName(taskNode, "environment_variables")
        .ifPresent(property -> {
          // Convert Map<String, Object> to Map<String, String>
          Map<String, String> env = toscaService.parseComplexPropertyValue(property,
              value -> ((ScalarPropertyValue) value).getValue());
          task.setEnv(env);
        });

    toscaService
        .<ListPropertyValue>getTypedNodePropertyByName(taskNode, "constraints")
        .ifPresent(property -> {
          List<List<String>> constraints = toscaService
              .parseListPropertyValue(property, item -> (ListPropertyValue) item)
              .stream()
              .map(constraint -> toscaService.parseListPropertyValue(constraint,
                  item -> ((ScalarPropertyValue) item).getValue()))
              .collect(Collectors.toList());
          task.setConstraints(constraints);
        });

    toscaService
        .<ComplexPropertyValue>getTypedNodePropertyByName(taskNode, "labels")
        .ifPresent(property -> {
          // Convert Map<String, Object> to Map<String, String>
          Map<String, String> labels = toscaService.parseComplexPropertyValue(property,
              value -> ((ScalarPropertyValue) value).getValue());
          task.setLabels(labels);
        });

    DeploymentArtifact image = toscaService
        .getNodeArtifactByName(taskNode, "image")
        .orElseThrow(() -> new IllegalArgumentException(
            String.format("<image> artifact not found in node <%s> of type <%s>",
                taskNode.getName(), taskNode.getType())));

    // <image> artifact type check
    List<String> supportedTypes =
        EnumUtils.toList(MesosContainer.Type.class, MesosContainer.Type::getToscaName);

    MesosContainer.Type containerType = EnumUtils
        .fromPredicate(MesosContainer.Type.class,
            enumItem -> enumItem.getToscaName().equals(image.getArtifactType()))
        .orElseThrow(() -> new IllegalArgumentException(String.format(
            "Unsupported artifact type for <image> artifact in node <%s> of type <%s>."
                + " Given <%s>, supported <%s>",
            taskNode, taskNode.getType(), image.getArtifactType(), supportedTypes)));

    MesosContainer container = new MesosContainer(containerType);

    task.setContainer(container);

    String imageName = CommonUtils
        .<ScalarPropertyValue>optionalCast(image.getFile())
        .orElseThrow(() -> new IllegalArgumentException(String.format(
            "<file> field for <image> artifact in node <%s> must be provided", taskNode.getName())))
        .getValue();

    container.setImage(imageName);

    toscaService
        .<ScalarPropertyValue>getTypedNodePropertyByName(taskNode, "priviliged")
        .ifPresent(value -> {
          Boolean priviliged = toscaService.parseScalarPropertyValue(value, BooleanType.class);
          container.setPriviliged(priviliged);
        });

    toscaService
        .<ScalarPropertyValue>getTypedNodePropertyByName(taskNode, "force_pull_image")
        .ifPresent(value -> {
          Boolean forcePullImage = toscaService.parseScalarPropertyValue(value, BooleanType.class);
          container.setForcePullImage(forcePullImage);
        });

    Capability dockerCapability = getHostCapability(graph, taskNode);

    toscaService
        .<ScalarPropertyValue>getTypedCapabilityPropertyByName(dockerCapability, "num_cpus")
        .ifPresent(value -> {
          Double numCpus = toscaService.parseScalarPropertyValue(value, FloatType.class);
          task.setCpus(numCpus);
        });

    toscaService
        .<ScalarPropertyValue>getTypedCapabilityPropertyByName(dockerCapability, "mem_size")
        .ifPresent(value -> {
          Size memSize = toscaService.parseScalarPropertyValue(value, SizeType.class);
          task.setMemSize(memSize.convert("MB")); // Mesos wants MB
        });

    Optional<ListPropertyValue> volumesProperty =
        toscaService.getTypedCapabilityPropertyByName(dockerCapability, "volumes");

    List<String> containerVolumes = volumesProperty
        .map(property -> toscaService
            .parseListPropertyValue(property, item -> (ScalarPropertyValue) item))
        .orElseGet(Collections::emptyList)
        .stream()
        .map(ScalarPropertyValue::getValue)
        .collect(Collectors.toList());
    container.setVolumes(containerVolumes);

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
      task
          .getContainer()
          .orElseThrow(() -> new RuntimeException(
              "there are ports to publish but no container is present"))
          .setPortMappings(portMapping);
    }

    return task;
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

  protected List<NodeTemplate> getParentNodes(String parentCapabilityName,
      DirectedMultigraph<NodeTemplate, RelationshipTemplate> graph,
      NodeTemplate taskNode) {
    return graph
        .incomingEdgesOf(taskNode)
        .stream()
        .filter(
            relationship -> parentCapabilityName.equals(relationship.getTargetedCapabilityName()))
        .map(graph::getEdgeSource)
        .collect(Collectors.toList());
  }

  protected Capability getHostCapability(
      DirectedMultigraph<NodeTemplate, RelationshipTemplate> graph, NodeTemplate taskNode) {

    NodeTemplate hostNode = getHostNode(graph, taskNode);

    // at this point we're sure that it exists
    return hostNode.getCapabilities().get(HOST_CAPABILITY_NAME);
  }

  protected MesosPortMapping generatePortMapping(ComplexPropertyValue portMappingProperties) {
    Map<String, ScalarPropertyValue> values = toscaService
        .parseComplexPropertyValue(portMappingProperties, value -> (ScalarPropertyValue) value);

    ScalarPropertyValue sourcePortProperty =
        CommonUtils
            .getFromOptionalMap(values, "source")
            .orElseThrow(() -> new ToscaException(
                "source port in 'publish_ports' property must be provided"));

    Long sourcePortVaule =
        toscaService.parseScalarPropertyValue(sourcePortProperty, IntegerType.class);
    MesosPortMapping portMapping = new MesosPortMapping(Ints.checkedCast(sourcePortVaule));

    CommonUtils.getFromOptionalMap(values, "target").ifPresent(value -> {
      Long targetPortVaule = toscaService.parseScalarPropertyValue(value, IntegerType.class);
      portMapping.setServicePort(Ints.checkedCast(targetPortVaule));
    });

    CommonUtils.getFromOptionalMap(values, "protocol").ifPresent(value -> {
      String protocolVaule = toscaService.parseScalarPropertyValue(value, StringType.class);
      Protocol protocol = EnumUtils.fromNameOrThrow(Protocol.class, protocolVaule);
      portMapping.setProtocol(protocol);
    });
    return portMapping;
  }
}
