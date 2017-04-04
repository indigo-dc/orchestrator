package it.reply.orchestrator.service.deployment.providers;

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

import com.google.common.collect.MoreCollectors;

import alien4cloud.model.components.ComplexPropertyValue;
import alien4cloud.model.components.DeploymentArtifact;
import alien4cloud.model.components.ListPropertyValue;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.tosca.normative.FloatType;
import alien4cloud.tosca.normative.InvalidPropertyValueException;
import alien4cloud.tosca.normative.Size;
import alien4cloud.tosca.normative.SizeType;

import it.reply.orchestrator.dto.mesos.MesosContainer;
import it.reply.orchestrator.dto.mesos.MesosTask;
import it.reply.orchestrator.service.ToscaService;
import it.reply.orchestrator.utils.CommonUtils;
import it.reply.orchestrator.utils.EnumUtils;

import lombok.extern.slf4j.Slf4j;

import org.jgrapht.graph.DirectedMultigraph;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
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
      NodeTemplate taskNode, String taskId) throws InvalidPropertyValueException {

    T task = createInternalTaskRepresentation();
    task.setId(taskId);

    Optional<ScalarPropertyValue> command =
        toscaService.getTypedNodePropertyByName(taskNode, "command");
    command.ifPresent(property -> task.setCmd(property.getValue()));

    Optional<ListPropertyValue> uris = toscaService.getTypedNodePropertyByName(taskNode, "uris");
    if (uris.isPresent()) {
      // Convert List<Object> to List<String>
      task.setUris(toscaService.parseListPropertyValue(uris.get(),
          item -> ((ScalarPropertyValue) item).getValue()));
    }

    Optional<ComplexPropertyValue> envVars =
        toscaService.getTypedNodePropertyByName(taskNode, "environment_variables");

    if (envVars.isPresent()) {
      // Convert Map<String, Object> to Map<String, String>
      task.setEnv(toscaService.parseComplexPropertyValue(envVars.get(),
          value -> ((ScalarPropertyValue) value).getValue()));
    }

    Optional<ListPropertyValue> constraintsProperty =
        toscaService.getTypedNodePropertyByName(taskNode, "constraints");
    if (constraintsProperty.isPresent()) {
      List<List<String>> constraints = toscaService
          .parseListPropertyValue(constraintsProperty.get(), item -> (ListPropertyValue) item)
          .stream()
          .map(constraint -> toscaService.parseListPropertyValue(constraint,
              item -> ((ScalarPropertyValue) item).getValue()))
          .collect(Collectors.toList());
      task.setConstraints(constraints);
    }

    DeploymentArtifact image = toscaService.getNodeArtifactByName(taskNode, "image")
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

    String imageName = CommonUtils.<ScalarPropertyValue>optionalCast(image.getFile())
        .orElseThrow(() -> new IllegalArgumentException(String.format(
            "<file> field for <image> artifact in node <%s> must be provided", taskNode.getName())))
        .getValue();

    container.setImage(imageName);

    Capability dockerCapability = getHostCapability(graph, taskNode);

    Optional<ScalarPropertyValue> cpusProperty =
        toscaService.getTypedCapabilityPropertyByName(dockerCapability, "num_cpus");
    if (cpusProperty.isPresent()) {
      task.setCpus(toscaService.parseScalarPropertyValue(cpusProperty.get(), FloatType.class));
    }

    Optional<ScalarPropertyValue> memProperty =
        toscaService.getTypedCapabilityPropertyByName(dockerCapability, "mem_size");
    if (memProperty.isPresent()) {
      Size memSize = toscaService.parseScalarPropertyValue(memProperty.get(), SizeType.class);
      task.setMemSize(memSize.convert("MB")); // Mesos wants MB
    }

    Optional<ListPropertyValue> volumesProperty =
        toscaService.getTypedCapabilityPropertyByName(dockerCapability, "volumes");

    List<String> containerVolumes = volumesProperty
        .map(property -> toscaService.parseListPropertyValue(property,
            item -> (ScalarPropertyValue) item))
        .orElse(Collections.emptyList())
        .stream()
        .map(ScalarPropertyValue::getValue)
        .collect(Collectors.toList());
    container.setVolumes(containerVolumes);

    return task;
  }

  protected NodeTemplate getHostNode(DirectedMultigraph<NodeTemplate, RelationshipTemplate> graph,
      NodeTemplate taskNode) {
    return graph.incomingEdgesOf(taskNode)
        .stream()
        .filter(
            relationship -> HOST_CAPABILITY_NAME.equals(relationship.getTargetedCapabilityName()))
        .map(graph::getEdgeSource)
        // if more than 1 node is present -> IllegalArgumentException
        .collect(MoreCollectors.toOptional())
        .orElseThrow(() -> new IllegalArgumentException(
            String.format("No hosting node provided for node <%s>", taskNode.getName())));
  }

  protected Capability getHostCapability(
      DirectedMultigraph<NodeTemplate, RelationshipTemplate> graph, NodeTemplate taskNode) {

    NodeTemplate hostNode = getHostNode(graph, taskNode);

    // at this point we're sure that it exists
    return hostNode.getCapabilities().get(HOST_CAPABILITY_NAME);
  }
}
