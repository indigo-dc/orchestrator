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
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Primitives;

import alien4cloud.deployment.InputsPreProcessorService;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.ComplexPropertyValue;
import alien4cloud.model.components.ConcatPropertyValue;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
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

  private static final Set<Class<?>> PRIMITIVE_CLASSES = ImmutableSet.of(
      String.class,
      Boolean.class,
      Byte.class,
      Character.class,
      Double.class,
      Float.class,
      Integer.class,
      Long.class,
      Short.class);

  @FunctionalInterface
  public static interface ToscaLambda
      extends BiFunction<FunctionPropertyValue, String, Optional<AbstractPropertyValue>> {
  }

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
  public void processGetInput(ArchiveRoot archiveRoot, Map<String, Object> inputs) {
    Optional.ofNullable(archiveRoot).map(ArchiveRoot::getTopology).ifPresent(topology -> {
      Map<String, PropertyDefinition> templateInputs =
          Optional
              .ofNullable(topology.getInputs())
              .orElseGet(Collections::emptyMap);

      Map<String, ToscaLambda> functions = new HashMap<>();
      functions.put(ToscaFunctionConstants.GET_INPUT, (FunctionPropertyValue function,
          String propertyName) -> processGetInputFuction(function, templateInputs, inputs,
              propertyName));

      processFunctions(topology, functions);
    });
  }

  protected void processFunctions(Topology topology, Map<String, ToscaLambda> functions) {

    Map<String, NodeTemplate> nodes =
        Optional
            .ofNullable(topology.getNodeTemplates())
            .orElseGet(Collections::emptyMap);

    // Process policies
    Optional
        .ofNullable(topology.getPolicies())
        .orElseGet(Collections::emptyList)
        .forEach(policy -> processFunctions(functions, policy.getProperties(),
            String.format("policies[%s][properties]", policy.getName())));

    // Iterate on each element that could have nested FunctionPropertyValue

    nodes.forEach((nodeName, nodeTemplate) -> {
      // process node's properties
      processFunctions(functions, nodeTemplate.getProperties(),
          String.format("node_templates[%s][properties]", nodeName));

      // process node's relationships
      Optional
          .ofNullable(nodeTemplate.getRelationships())
          .orElseGet(Collections::emptyMap)
          .forEach(
              (relationshipName, relationship) -> processFunctions(functions,
                  relationship.getProperties(),
                  String.format(
                      "node_templates[%s][requirements][%s][relationship][%s][properties]",
                      nodeName, relationship.getRequirementName(), relationshipName)));

      // process node's capabilities
      Optional
          .ofNullable(nodeTemplate.getCapabilities())
          .orElseGet(Collections::emptyMap)
          .forEach((capablilityName, capablility) -> processFunctions(functions,
              capablility.getProperties(), String.format(
                  "node_templates[%s][capabilities][%s][properties]", nodeName, capablilityName)));

      // process node's artifacts
      Optional
          .ofNullable(nodeTemplate.getArtifacts())
          .orElseGet(Collections::emptyMap)
          .forEach(
              (artifactName,
                  artifact) -> processFunctions(functions, artifact.getFile(), String
                      .format("node_templates[%s][artifacts][%s][file]", nodeName, artifactName))
                          .ifPresent(artifact::setFile));

      // process node's interfaces
      Optional
          .ofNullable(nodeTemplate.getInterfaces())
          .orElseGet(Collections::emptyMap)
          .forEach((interfaceName, toscaInterface) -> processInterfaceOperationsInputs(functions,
              toscaInterface.getOperations(),
              String.format("node_templates[%s][interfaces][%s]", nodeName, interfaceName)));

    });
  }

  protected Optional<AbstractPropertyValue> processFunctions(
      Map<String, ToscaLambda> functions, AbstractPropertyValue propertyValue,
      String propertyName) {

    if (propertyValue instanceof FunctionPropertyValue) {
      FunctionPropertyValue functionPropertyValue = (FunctionPropertyValue) propertyValue;
      return functions
          .getOrDefault(functionPropertyValue.getFunction(), IndigoInputsPreProcessorService::noOp)
          .apply(functionPropertyValue, propertyName);
    } else if (propertyValue instanceof ConcatPropertyValue) {
      return processConcat(functions, (ConcatPropertyValue) propertyValue, propertyName);
    } else if (propertyValue instanceof ComplexPropertyValue) {
      processFunctions(functions, ((ComplexPropertyValue) propertyValue).getValue(),
          propertyName);
      return Optional.of(propertyValue);
    } else if (propertyValue instanceof ListPropertyValue) {
      processFunctions(functions, ((ListPropertyValue) propertyValue).getValue(),
          propertyName);
      return Optional.of(propertyValue);
    }
    return Optional.ofNullable(propertyValue);
  }

  protected void processFunctions(Map<String, ToscaLambda> functions,
      Map<String, ? super AbstractPropertyValue> optionalProperties, String propertyName) {

    Optional
        .ofNullable(optionalProperties)
        .ifPresent(properties -> properties
            .replaceAll((key, oldValue) -> {
              if (oldValue instanceof AbstractPropertyValue) {
                AbstractPropertyValue oldPropertyValue = (AbstractPropertyValue) oldValue;
                return processFunctions(functions, oldPropertyValue,
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

  protected void processFunctions(Map<String, ToscaLambda> functions,
      List<? super AbstractPropertyValue> optionalProperties,
      String propertyName) {

    Optional
        .ofNullable(optionalProperties)
        .ifPresent(properties -> IntStream
            .range(0, properties.size())
            .forEach(index -> {
              Object oldValue = properties.get(index);
              if (oldValue instanceof AbstractPropertyValue) {
                AbstractPropertyValue oldPropertyValue = (AbstractPropertyValue) oldValue;
                processFunctions(functions, oldPropertyValue,
                    String.format("%s[%s]", propertyName, index))
                        // Replace function value with the replaced value (if non null and changed)
                        .filter(newPropertyValue -> newPropertyValue != oldPropertyValue)
                        .ifPresent(newPropertyValue -> properties.set(index, newPropertyValue));
              } else {
                // DO NOTHING, NOT A SUBSTITUTABLE TYPE
              }
            }));
  }

  private Optional<AbstractPropertyValue> processConcat(Map<String, ToscaLambda> functions,
      ConcatPropertyValue concatPropertyValue, String propertyName) {
    processFunctions(functions, concatPropertyValue.getParameters(),
        String.format("%s[concat]", propertyName));
    boolean allParametersAreScalarValues = concatPropertyValue
        .getParameters()
        .stream()
        .allMatch(ScalarPropertyValue.class::isInstance);
    if (allParametersAreScalarValues) {
      // if all the parameters of the concat function are scalars they can be joined
      // if they are not it means that some functions haven't been evaluated
      // TODO should we raise an error if Complex or ListPropertyValue are encountered?
      ScalarPropertyValue scalarPropertyValue = new ScalarPropertyValue(concatPropertyValue
          .getParameters()
          .stream()
          .map(ScalarPropertyValue.class::cast)
          .map(ScalarPropertyValue::getValue)
          .collect(Collectors.joining()));
      scalarPropertyValue.setPrintable(true);
      return Optional.of(scalarPropertyValue);
    } else {
      return Optional.of(concatPropertyValue);
    }
  }

  private void processInterfaceOperationsInputs(Map<String, ToscaLambda> functions,
      Map<String, Operation> optionalOperations, String propertyName) {
    Optional
        .ofNullable(optionalOperations)
        .orElseGet(Collections::emptyMap)
        .forEach((operationName, operation) -> processFunctions(functions,
            operation.getInputParameters(),
            String.format("%s[inputs][%s]", propertyName, operationName)));
  }

  protected static Optional<AbstractPropertyValue> noOp(FunctionPropertyValue function,
      String propertyName) {
    return Optional.ofNullable(function);
  }

  protected Optional<AbstractPropertyValue> processGetInputFuction(FunctionPropertyValue function,
      Map<String, PropertyDefinition> templateInputs, Map<String, Object> inputs,
      String propertyName) {
    try {
      String inputName = function.getParameters().get(0);
      // Alien4Cloud already validates existing input name
      PropertyDefinition templateInput = CommonUtils
          .getFromOptionalMap(templateInputs, inputName)
          .orElseThrow(() -> new IllegalArgumentException(
              String.format("No input with name <%s> defined in the template", inputName)));
      // Look for user's given input, if not present use the default one
      Object inputValue = Optional
          .ofNullable(inputs.getOrDefault(inputName, templateInput.getDefault()))
          // No given input or default value available -> error
          .orElseThrow(() -> new IllegalArgumentException(
              String.format(
                  "No input provided for <%s> and no default value provided in the definition",
                  inputName)));

      LOG.debug(
          "TOSCA input function replacement: <{}>, input name <{}>, input value <{}>",
          propertyName, inputName, inputValue);

      return Optional.of(toPropertyValue(inputValue));

    } catch (RuntimeException ex) {
      throw new ToscaException(
          String.format("Failed to replace input function on <%s>, caused by: %s",
              propertyName, ex.getMessage()), ex);
    }
  }

  private AbstractPropertyValue toPropertyValue(Object javaValue) {
    Preconditions.checkNotNull(javaValue);
    final AbstractPropertyValue val;
    if (PRIMITIVE_CLASSES.contains(Primitives.wrap(javaValue.getClass()))) {
      val = new ScalarPropertyValue(javaValue.toString());
    } else if (javaValue instanceof List) {
      List<Object> list = ((List<?>) javaValue)
          .stream()
          .map(entry -> toPropertyValue(entry))
          .collect(Collectors.toList());
      val = new ListPropertyValue(list);
    } else if (javaValue instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = ((Map<String, Object>) javaValue)
          .entrySet()
          .stream()
          .collect(Collectors.toMap(
              Entry::getKey, entry -> toPropertyValue(entry.getValue())));
      val = new ComplexPropertyValue(map);
    } else {
      throw new IllegalArgumentException(
          String.format("Error converting %s to PropertyValue: Unsupported value type <%s>",
          javaValue, javaValue.getClass()));
    }

    val.setPrintable(true);
    return val;
  }
}
