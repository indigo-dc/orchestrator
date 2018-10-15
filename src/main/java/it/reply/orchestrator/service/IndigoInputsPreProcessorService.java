/*
 * Copyright © 2015-2018 Santer Reply S.p.A.
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

import alien4cloud.deployment.InputsPreProcessorService;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.ComplexPropertyValue;
import alien4cloud.model.components.ConcatPropertyValue;
import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.ListPropertyValue;
import alien4cloud.model.components.Operation;
import alien4cloud.model.components.OutputDefinition;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.PropertyValue;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.normative.ToscaFunctionConstants;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Primitives;

import it.reply.orchestrator.exception.service.ToscaException;
import it.reply.orchestrator.function.ToscaFunction;
import it.reply.orchestrator.utils.CommonUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Service;

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

      Map<String, ToscaFunction> functions = new HashMap<>();
      functions.put(ToscaFunctionConstants.GET_INPUT, (FunctionPropertyValue function,
          String propertyName) -> processGetInputFuction(function, templateInputs, inputs,
              propertyName));

      processFunctions(topology, functions);
    });
  }

  /**
   * Process the outputs of a topology.
   *
   * @param archiveRoot
   *          the in-memory TOSCA template.
   * @param inputs
   *          the user's inputs to the template.
   * @param runtimeProperties
   *          deployment runtimeProperties.
   */
  public Map<String, Object> processOutputs(ArchiveRoot archiveRoot, Map<String, Object> inputs,
      RuntimeProperties runtimeProperties) {
    Map<String, Object> processedOutputs = new HashMap<>();
    Optional.ofNullable(archiveRoot).map(ArchiveRoot::getTopology).ifPresent(topology -> {

      Map<String, ToscaFunction> functions = new HashMap<>();

      Map<String, NodeTemplate> nodeTemplates =
        CommonUtils.notNullOrDefaultValue(topology.getNodeTemplates(), Collections::emptyMap);

      functions.put(ToscaFunctionConstants.GET_ATTRIBUTE, (FunctionPropertyValue function,
        String propertyName) -> processGetAttibute(function, runtimeProperties,
        propertyName));

      functions.put(ToscaFunctionConstants.GET_PROPERTY, (FunctionPropertyValue function,
        String propertyName) -> processGetProperty(function, nodeTemplates,
        propertyName));

      Map<String, PropertyDefinition> templateInputs =
          CommonUtils.notNullOrDefaultValue(topology.getInputs(), Collections::emptyMap);

      functions.put(ToscaFunctionConstants.GET_INPUT, (FunctionPropertyValue function,
          String propertyName) -> processGetInputFuction(function, templateInputs, inputs,
              propertyName));

      Map<String, OutputDefinition> outputs =
          CommonUtils.notNullOrDefaultValue(topology.getOutputs(), Collections::emptyMap);

      outputs.forEach((name, output) -> {
        try {
          processFunctions(functions, output.getValue(), String.format("outputs[%s][value]", name))
              .ifPresent(processedOutput -> output.setValue(processedOutput));
        } catch (RuntimeException ex) {
          processedOutputs.put(name, ex.getMessage());
          return;
        }
        if (output.getValue() instanceof PropertyValue) {
          processedOutputs.put(name, ((PropertyValue<?>) output.getValue()).getValue());
        }
      });
    });
    return processedOutputs;
  }

  public void processFunctions(ArchiveRoot archiveRoot, Map<String, Object> inputs,
    RuntimeProperties runtimeProperties) {
    Optional
      .ofNullable(archiveRoot)
      .map(ArchiveRoot::getTopology)
      .ifPresent(topology -> {
        Map<String, ToscaFunction> functions = new HashMap<>();

        Map<String, NodeTemplate> nodeTemplates =
          CommonUtils.notNullOrDefaultValue(topology.getNodeTemplates(), Collections::emptyMap);

        functions.put(ToscaFunctionConstants.GET_ATTRIBUTE, (FunctionPropertyValue function,
          String propertyName) -> processGetAttibute(function, runtimeProperties,
          propertyName));

        functions.put(ToscaFunctionConstants.GET_PROPERTY, (FunctionPropertyValue function,
          String propertyName) -> processGetProperty(function, nodeTemplates,
          propertyName));

        Map<String, PropertyDefinition> templateInputs =
          CommonUtils.notNullOrDefaultValue(topology.getInputs(), Collections::emptyMap);

        functions.put(ToscaFunctionConstants.GET_INPUT, (FunctionPropertyValue function,
          String propertyName) -> processGetInputFuction(function, templateInputs, inputs,
          propertyName));

        processFunctions(topology, functions);
      });
  }


  protected void processFunctions(Topology topology, Map<String, ToscaFunction> functions) {

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

    // // Process outputs
    // Optional
    // .ofNullable(topology.getOutputs())
    // .orElseGet(Collections::emptyMap)
    // .forEach((name, outputDefinition) -> processFunctions(functions,
    // outputDefinition.getValue(), String.format("outputs[%s][value]", name))
    // .ifPresent(processedOutput -> outputDefinition.setValue(processedOutput)));

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
      Map<String, ToscaFunction> functions, AbstractPropertyValue propertyValue,
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

  protected void processFunctions(Map<String, ToscaFunction> functions,
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

  protected void processFunctions(Map<String, ToscaFunction> functions,
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

  private Optional<AbstractPropertyValue> processConcat(Map<String, ToscaFunction> functions,
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

  private void processInterfaceOperationsInputs(Map<String, ToscaFunction> functions,
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

      return Optional.of(toAbstractPropertyValue(inputValue));

    } catch (RuntimeException ex) {
      throw new ToscaException("Failed to replace input function on " + propertyName, ex);
    }
  }

  protected Optional<AbstractPropertyValue> processGetAttibute(FunctionPropertyValue function,
      RuntimeProperties runtimeProperties, String propertyName) {
    AbstractPropertyValue returnValue =
        runtimeProperties.get(RuntimeProperties.escapedPath(function));
    if (returnValue == null) {
      throw new ToscaException(
          String.format("Failed to evaluate function %s[%s][%s]: no value found",
              propertyName, function.getFunction(),
              CommonUtils.nullableCollectionToStream(function.getParameters()).collect(
                  Collectors.joining(", "))));
    } else {
      return Optional.of(returnValue);
    }
  }

  protected Optional<AbstractPropertyValue> processGetProperty(FunctionPropertyValue function,
    @NonNull Map<String, NodeTemplate> nodeTemplates, String propertyName) {
    return CommonUtils
      .getFromOptionalMap(nodeTemplates, function.getParameters().get(0))
      .flatMap(nodeTemplate -> {
        String firstParam = function.getParameters().get(1);
        List<String> additionalParameters = function.getParameters()
          .subList(2, function.getParameters().size());
        Optional<AbstractPropertyValue> returnValue = CommonUtils
          .getFromOptionalMap(nodeTemplate.getProperties(), firstParam)
          .map(IndigoInputsPreProcessorService::toAbstractPropertyValue);
        if (!returnValue.isPresent() && !additionalParameters.isEmpty()) {
          String optionalReqOrCapPropertyName = additionalParameters.get(0);
          additionalParameters = additionalParameters.subList(1, additionalParameters.size());
          returnValue = Optional.ofNullable(nodeTemplate.getCapabilities())
            .map(capabilities -> capabilities.get(firstParam))
            .flatMap(capability -> CommonUtils
              .getFromOptionalMap(capability.getProperties(), optionalReqOrCapPropertyName))
            .map(IndigoInputsPreProcessorService::toAbstractPropertyValue);
          if (!returnValue.isPresent()) {
            returnValue = Optional.ofNullable(nodeTemplate.getRelationships())
              .map(requirements -> requirements.get(firstParam))
              .flatMap(requirement -> CommonUtils
                .getFromOptionalMap(requirement.getProperties(), optionalReqOrCapPropertyName))
              .map(IndigoInputsPreProcessorService::toAbstractPropertyValue);
          }
        }
        if (returnValue.isPresent()) {
          AbstractPropertyValue propertyValue = returnValue.get();
          for (String additionalParameter : additionalParameters) {
            if (propertyValue instanceof ComplexPropertyValue) {
              propertyValue = IndigoInputsPreProcessorService
                .toAbstractPropertyValue(((ComplexPropertyValue) propertyValue)
                  .getValue()
                  .get(additionalParameter));
            } else if (propertyValue instanceof ListPropertyValue) {
              propertyValue = IndigoInputsPreProcessorService
                .toAbstractPropertyValue(((ListPropertyValue) propertyValue)
                  .getValue()
                  .get(Integer.parseInt(additionalParameter)));
            }
          }
          returnValue = Optional.ofNullable(propertyValue);
        }
        if (!returnValue.isPresent()) {
          throw new ToscaException(
            String.format("Failed to evaluate function %s[%s][%s]: no value found",
              propertyName, function.getFunction(),
              CommonUtils.nullableCollectionToStream(function.getParameters()).collect(
                Collectors.joining(", "))));
        }
        return returnValue;
      });

  }

  /**
   * Recursively generates an AbstractPropertyValue from a POJO.
   * 
   * @param javaValue
   *          the POJO
   * @return the AbstractPropertyValue generated
   */
  @NonNull
  public static AbstractPropertyValue toAbstractPropertyValue(Object javaValue) {
    Preconditions.checkNotNull(javaValue);
    final AbstractPropertyValue val;
    if (javaValue instanceof AbstractPropertyValue) {
      val = (AbstractPropertyValue) javaValue;
    } else if (javaValue instanceof PropertyValue) {
      val = toAbstractPropertyValue(((PropertyValue<?>) javaValue).getValue());
    } else if (PRIMITIVE_CLASSES.contains(Primitives.wrap(javaValue.getClass()))) {
      val = new ScalarPropertyValue(javaValue.toString());
    } else if (javaValue instanceof List) {
      List<Object> list = ((List<?>) javaValue)
          .stream()
          .map(entry -> toAbstractPropertyValue(entry))
          .collect(Collectors.toList());
      val = new ListPropertyValue(list);
    } else if (javaValue instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = ((Map<String, Object>) javaValue)
          .entrySet()
          .stream()
          .collect(Collectors.toMap(
              Entry::getKey, entry -> toAbstractPropertyValue(entry.getValue())));
      val = new ComplexPropertyValue(map);
    } else {
      throw new IllegalArgumentException(
          String.format("Error converting %s to PropertyValue: Unsupported value type <%s>",
              javaValue, javaValue.getClass()));
    }

    val.setPrintable(true);
    return val;
  }

  @Data
  public static class RuntimeProperties {
    @NonNull
    Map<String, AbstractPropertyValue> vaules = new HashMap<>();

    /**
     * Put a list of runtime propeties.
     * 
     * @param list
     *          the list
     * @param basekeys
     *          the path chunks associated with the properties
     * @return this object for chaining
     */
    public RuntimeProperties put(List<?> list, String... basekeys) {
      vaules.put(escapedPath(Stream.of(basekeys)),
          IndigoInputsPreProcessorService.toAbstractPropertyValue(list));
      for (int i = 0; i < list.size(); ++i) {
        vaules.put(escapedPath(
            Stream.concat(Stream.of(basekeys), Stream.of(String.valueOf(i)))),
            IndigoInputsPreProcessorService.toAbstractPropertyValue(list.get(i)));
      }
      return this;
    }

    /**
     * Put a runtime property.
     * 
     * @param item
     *          the property
     * @param basekeys
     *          the path chunks associated with the property
     * @return this object for chaining
     */
    public RuntimeProperties put(Object item, String... basekeys) {
      vaules.put(escapedPath(Stream.of(basekeys)),
          IndigoInputsPreProcessorService.toAbstractPropertyValue(item));
      return this;
    }

    public AbstractPropertyValue get(String key) {
      return vaules.get(key);
    }

    private static String escapedPath(Stream<String> stream) {
      return stream.collect(Collectors.joining("."));
    }

    private static String escapedPath(FunctionPropertyValue function) {
      return escapedPath(function.getParameters().stream());
    }
  }

}
