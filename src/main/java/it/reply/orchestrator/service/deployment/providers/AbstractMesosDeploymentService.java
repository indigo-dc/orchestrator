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

import com.google.common.collect.MoreCollectors;
import com.google.common.primitives.Ints;

import it.reply.orchestrator.dto.mesos.MesosContainer;
import it.reply.orchestrator.dto.mesos.MesosPortMapping;
import it.reply.orchestrator.dto.mesos.MesosPortMapping.Protocol;
import it.reply.orchestrator.dto.mesos.MesosTask;
import it.reply.orchestrator.exception.service.ToscaException;
import it.reply.orchestrator.service.ToscaService;
import it.reply.orchestrator.utils.CommonUtils;
import it.reply.orchestrator.utils.EnumUtils;
import it.reply.orchestrator.utils.ToscaUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.normative.types.BooleanType;
import org.alien4cloud.tosca.normative.types.FloatType;
import org.alien4cloud.tosca.normative.types.IntegerType;
import org.alien4cloud.tosca.normative.types.SizeType;
import org.jgrapht.graph.DirectedMultigraph;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractMesosDeploymentService<T extends MesosTask<T>, S>
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
   */
  @SuppressWarnings("unchecked")
  public T buildTask(DirectedMultigraph<NodeTemplate, RelationshipTemplate> graph,
      NodeTemplate taskNode, String taskId) {

    T task = createInternalTaskRepresentation();
    task.setId(taskId);

    ToscaUtils
        .extractScalar(taskNode.getProperties(), "command")
        .ifPresent(task::setCmd);

    ToscaUtils
        .extractList(taskNode.getProperties(), "uris", String.class::cast)
        .ifPresent(task::setUris);

    ToscaUtils
        .extractMap(taskNode.getProperties(), "environment_variables", String.class::cast)
        .ifPresent(task::setEnv);

    ToscaUtils
        .extractList(taskNode.getProperties(), "constraints",
            l -> ((List<Object>) l)
                .stream()
                .map(String.class::cast)
                .collect(Collectors.toList()))
        .ifPresent(task::setConstraints);

    ToscaUtils
        .extractMap(taskNode.getProperties(), "labels", String.class::cast)
        .ifPresent(task::setLabels);

    DeploymentArtifact image = toscaService
        .getNodeArtifactByName(taskNode, "image")
        .orElseThrow(() -> new IllegalArgumentException(
            String.format("<image> artifact not found in node <%s> of type <%s>",
                taskNode.getName(), taskNode.getType())));

    // <image> artifact type check
    List<String> supportedTypes = EnumUtils
        .toList(MesosContainer.Type.class, MesosContainer.Type::getToscaName);

    MesosContainer.Type containerType = EnumUtils
        .fromPredicate(MesosContainer.Type.class,
            enumItem -> enumItem.getToscaName().equals(image.getArtifactType()))
        .orElseThrow(() -> new IllegalArgumentException(String.format(
            "Unsupported artifact type for <image> artifact in node <%s> of type <%s>."
                + " Given <%s>, supported <%s>",
            taskNode, taskNode.getType(), image.getArtifactType(), supportedTypes)));

    MesosContainer container = new MesosContainer(containerType);

    task.setContainer(container);

    String imageName = Optional
        .ofNullable(image.getArtifactRef())
        .orElseThrow(() ->
            new IllegalArgumentException(
                "<file> field for <image> artifact in node <" + taskNode.getName()
                    + "> must be provided")
        );

    container.setImage(imageName);

    ToscaUtils
        .extractScalar(taskNode.getProperties(), "privileged", BooleanType.class)
        .ifPresent(container::setPrivileged);

    ToscaUtils
        .extractScalar(taskNode.getProperties(), "force_pull_image", BooleanType.class)
        .ifPresent(container::setForcePullImage);

    Capability containerCapability = getHostCapability(graph, taskNode);

    ToscaUtils
        .extractScalar(containerCapability.getProperties(), "num_cpus", FloatType.class)
        .ifPresent(task::setCpus);

    ToscaUtils
        .extractScalar(containerCapability.getProperties(), "num_gpus", IntegerType.class)
        .ifPresent(task::setGpus);

    ToscaUtils
        .extractScalar(containerCapability.getProperties(), "mem_size", SizeType.class)
        .map(memSize -> memSize.convert("MB"))
        .ifPresent(task::setMemSize);

    ToscaUtils
        .extractList(containerCapability.getProperties(), "volumes", String.class::cast)
        .ifPresent(container::setVolumes);

    ToscaUtils
        .extractList(containerCapability.getProperties(), "publish_ports", l ->
            this.generatePortMapping((Map<String, Object>) l)
        )
        .ifPresent(portMappings -> task
            .getContainer()
            .orElseThrow(
                () -> new RuntimeException(
                    "there are ports to publish but no container is present"))
            .setPortMappings(portMappings));

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

  protected MesosPortMapping generatePortMapping(Map<String, Object> portMappingProperties) {

    int sourcePortValue = CommonUtils
        .getFromOptionalMap(portMappingProperties, "source")
        .map(value -> ToscaUtils.parseScalar((String) value, IntegerType.class))
        .map(Ints::checkedCast)
        .orElseThrow(() -> new ToscaException(
            "source port in 'publish_ports' property must be provided"));

    MesosPortMapping portMapping = new MesosPortMapping(sourcePortValue);

    CommonUtils.getFromOptionalMap(portMappingProperties, "target")
        .map(value -> ToscaUtils.parseScalar((String) value, IntegerType.class))
        .map(Ints::checkedCast)
        .ifPresent(portMapping::setServicePort);

    CommonUtils.getFromOptionalMap(portMappingProperties, "protocol")
        .map(value -> EnumUtils.fromNameOrThrow(Protocol.class, (String) value))
        .ifPresent(portMapping::setProtocol);

    return portMapping;
  }
}
