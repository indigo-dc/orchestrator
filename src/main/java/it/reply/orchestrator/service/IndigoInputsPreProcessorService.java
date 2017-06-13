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

package it.reply.orchestrator.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.common.primitives.Primitives;

import alien4cloud.deployment.InputsPreProcessorService;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.ComplexPropertyValue;
import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.ListPropertyValue;
import alien4cloud.model.components.Operation;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.normative.ToscaFunctionConstants;

import it.reply.orchestrator.exception.service.ToscaException;
import it.reply.orchestrator.utils.CommonUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Inputs pre-processor service manages pre-processing of inputs parameters in a Topology.
 * 
 * <br/>
 * <br/>
 * <b>IMPORTANT</b>: please note that this class is a variation of the original
 * {@link InputsPreProcessorService} in Alien4Cloud. It has been edited to allow for an easier input
 * replacement with just the user's input map.
 *
 * @see InputsPreProcessorService
 * @author l.biava
 */
@Service
@Slf4j
public class IndigoInputsPreProcessorService {

  private static final Set<Class<?>> PRIMITIVE_CLASSES =
      Sets.newHashSet(String.class, Boolean.class, Byte.class, Character.class, Double.class,
          Float.class, Integer.class, Long.class, Short.class);

  /**
   * Process the get inputs functions of a topology to inject actual input provided by the user.
   *
   * @param archiveRoot
   *          the in-memory TOSCA template.
   * @param inputs
   *          the user's inputs to the template.
   * @throws ToscaException
   *           if the input replacement fails.
   */
  public void processGetInput(ArchiveRoot archiveRoot, Map<String, Object> inputs)
      throws ToscaException {

    Optional<Topology> topology = Optional.ofNullable(archiveRoot).map(ArchiveRoot::getTopology);

    Map<String, NodeTemplate> nodes =
        topology.map(Topology::getNodeTemplates).orElse(new HashMap<>());

    Map<String, PropertyDefinition> templateInputs =
        topology.map(Topology::getInputs).orElse(new HashMap<>());

    // Process policies
    topology.map(Topology::getPolicies)
        .orElse(new ArrayList<>())
        .forEach(policy -> processGetInput(templateInputs, inputs, policy.getProperties(),
            String.format("policies[%s][properties]", policy.getName())));

    // Iterate on each element that could have nested FunctionPropertyValue

    nodes.forEach((nodeName, nodeTemplate) -> {
      // process node's properties
      processGetInput(templateInputs, inputs, nodeTemplate.getProperties(),
          String.format("node_templates[%s][properties]", nodeName));

      // process node's relationships
      Optional.ofNullable(nodeTemplate.getRelationships())
          .orElse(new HashMap<>())
          .forEach(
              (relationshipName, relationship) -> processGetInput(templateInputs, inputs,
                  relationship.getProperties(),
                  String.format(
                      "node_templates[%s][requirements][%s][relationship][%s][properties]",
                      nodeName, relationship.getRequirementName(), relationshipName)));

      // process node's capabilities
      Optional.ofNullable(nodeTemplate.getCapabilities())
          .orElse(new HashMap<>())
          .forEach((capablilityName, capablility) -> processGetInput(templateInputs, inputs,
              capablility.getProperties(), String.format(
                  "node_templates[%s][capabilities][%s][properties]", nodeName, capablilityName)));

      // process node's artifacts
      Optional.ofNullable(nodeTemplate.getArtifacts())
          .orElse(new HashMap<>())
          .forEach(
              (artifactName,
                  artifact) -> processGetInput(templateInputs,
                      inputs, artifact.getFile(), String.format(
                          "node_templates[%s][artifacts][%s][file]", nodeName, artifactName))
                              .ifPresent(artifact::setFile));

      // process node's interfaces
      Optional.ofNullable(nodeTemplate.getInterfaces())
          .orElse(new HashMap<>())
          .forEach((interfaceName, toscaInterface) -> processInterfaceOperationsInputs(
              templateInputs, inputs, toscaInterface.getOperations(),
              String.format("node_templates[%s][interfaces][%s]", nodeName, interfaceName)));

    });
  }

  protected Optional<AbstractPropertyValue> processGetInput(
      Map<String, PropertyDefinition> optionalTemplateInputs, Map<String, Object> inputs,
      AbstractPropertyValue propertyValue, String propertyName) {
    Map<String, PropertyDefinition> templateInputs =
        CommonUtils.notNullOrDefaultValue(optionalTemplateInputs, new HashMap<>());
    // Only FunctionPropertyValue are interesting
    if (propertyValue instanceof FunctionPropertyValue) {
      FunctionPropertyValue function = (FunctionPropertyValue) propertyValue;
      // Only INPUT functions are interesting
      if (ToscaFunctionConstants.GET_INPUT.equals(function.getFunction())) {

        try {

          String inputName = function.getParameters().get(0);
          // Alien4Cloud already validates existing input name
          PropertyDefinition templateInput = templateInputs.get(inputName);
          // Look for user's given input, if not present use the default one
          Object inputValue =
              Optional.ofNullable(inputs.getOrDefault(inputName, templateInput.getDefault()))
                  // No given input or default value available -> error
                  .orElseThrow(() -> new IllegalArgumentException(String.format(
                      "No input provided for <%s> and no default value is available", inputName)));

          LOG.debug(
              "TOSCA input function replacement: " + "<{}>, input name <{}>, input value <{}>",
              propertyName, inputName, inputValue);

          return Optional.of(handleInputValue(inputValue, inputName));

        } catch (Exception ex) {
          throw new ToscaException(String.format(
              "Failed to replace input function on <%s> with parameters: <%s>, caused by: %s",
              propertyName, function.getParameters(), ex.getMessage()), ex);
        }
      }
    } else if (propertyValue instanceof ComplexPropertyValue) {
      processGetInput(templateInputs, inputs, ((ComplexPropertyValue) propertyValue).getValue(),
          propertyName);
      return Optional.of(propertyValue);

    } else if (propertyValue instanceof ListPropertyValue) {
      processGetInput(templateInputs, inputs, ((ListPropertyValue) propertyValue).getValue(),
          propertyName);
      return Optional.of(propertyValue);
    }
    return Optional.ofNullable(propertyValue);
  }

  protected void processGetInput(Map<String, PropertyDefinition> templateInputs,
      Map<String, Object> inputs, Map<String, ? super AbstractPropertyValue> optionalProperties,
      String propertyName) {

    Optional.ofNullable(optionalProperties)
        .ifPresent(properties -> properties.replaceAll((key, oldValue) -> {
          if (oldValue instanceof AbstractPropertyValue) {
            AbstractPropertyValue oldPropertyValue = (AbstractPropertyValue) oldValue;
            return processGetInput(templateInputs, inputs, oldPropertyValue,
                String.format("%s[%s]", propertyName, key))
                    // Replace function value with the replaced value (if non null and changed)
                    .filter(newPropertyValue -> newPropertyValue != oldPropertyValue)
                    .orElse(oldPropertyValue);
          } else {
            // DO NOTHING, NOT A SUBSTITUTABLE TYPE
            return oldValue;
          }
        }));
  }

  protected void processGetInput(Map<String, PropertyDefinition> templateInputs,
      Map<String, Object> inputs, List<? super AbstractPropertyValue> optionalProperties,
      String propertyName) {

    Optional.ofNullable(optionalProperties)
        .ifPresent(properties -> IntStream.range(0, properties.size()).forEach(i -> {
          Object oldValue = properties.get(i);
          if (oldValue instanceof AbstractPropertyValue) {
            AbstractPropertyValue oldPropertyValue = (AbstractPropertyValue) oldValue;
            processGetInput(templateInputs, inputs, oldPropertyValue,
                String.format("%s[%s]", propertyName, i))
                    // Replace function value with the replaced value (if non null and changed)
                    .filter(newPropertyValue -> newPropertyValue != oldPropertyValue)
                    .ifPresent(newPropertyValue -> properties.set(i, newPropertyValue));
          } else {
            // DO NOTHING, NOT A SUBSTITUTABLE TYPE
          }
        }));
  }

  private void processInterfaceOperationsInputs(Map<String, PropertyDefinition> templateInputs,
      Map<String, Object> inputs, Map<String, Operation> optionalOperations, String propertyName) {
    Optional.ofNullable(optionalOperations)
        .orElse(new HashMap<>())
        .forEach((operationName, operation) -> processGetInput(templateInputs, inputs,
            operation.getInputParameters(),
            String.format("%s[inputs][%s]", propertyName, operationName)));
  }

  private AbstractPropertyValue handleInputValue(Object inputValue, String inputName) {
    Preconditions.checkArgument(inputValue != null, "Invalid input value %s=%s", inputName,
        inputValue);
    final AbstractPropertyValue val;
    if (PRIMITIVE_CLASSES.contains(Primitives.wrap(inputValue.getClass()))) {
      val = new ScalarPropertyValue(inputValue.toString());
    } else if (inputValue instanceof List) {
      List<Object> list = ((List<?>) inputValue).stream()
          .map(entry -> handleInputValue(entry, inputName))
          .collect(Collectors.toList());
      val = new ListPropertyValue(list);
    } else if (inputValue instanceof Map) {
      Map<String, Object> map = ((Map<String, Object>) inputValue).entrySet().stream().collect(
          Collectors.toMap(Entry::getKey, entry -> handleInputValue(entry.getValue(), inputName)));
      val = new ComplexPropertyValue(map);
    } else {
      throw new IllegalArgumentException(
          String.format("Unsupported input type <%s> for value <%s> of input <%s>",
              inputValue.getClass(), inputValue, inputName));
    }

    val.setPrintable(true);
    return val;
  }
}
