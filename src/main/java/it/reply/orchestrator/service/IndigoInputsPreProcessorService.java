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

package it.reply.orchestrator.service;

import alien4cloud.tosca.model.ArchiveRoot;

import com.google.common.collect.Lists;

import it.reply.orchestrator.exception.service.ToscaException;
import it.reply.orchestrator.function.ToscaFunction;
import it.reply.orchestrator.utils.CommonUtils;
import it.reply.orchestrator.utils.ToscaUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.IValue;
import org.alien4cloud.tosca.model.definitions.OutputDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.normative.constants.ToscaFunctionConstants;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IndigoInputsPreProcessorService {

  private static final Pattern GET_INPUT_PATTERN = Pattern
      .compile("\\{\\s*" + ToscaFunctionConstants.GET_INPUT + ":\\s*(?<inputName>\\w+)\\s*}");

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
    Optional
        .ofNullable(archiveRoot).map(ArchiveRoot::getTopology)
        .ifPresent(topology -> {
          Map<String, PropertyDefinition> templateInputs =
              Optional
                .ofNullable(topology.getInputs())
                .orElseGet(Collections::emptyMap);

          Map<String, ToscaFunction> functions = new HashMap<>();
          Set<FunctionPropertyValue> processedFunctions = Collections
              .newSetFromMap(new IdentityHashMap<>());

          functions.put(ToscaFunctionConstants.GET_INPUT, (FunctionPropertyValue function,
              String propertyName) -> processGetInputFunction(function, templateInputs, inputs,
                  propertyName));

          processFunctions(topology, functions, processedFunctions);
        });
  }

  /**
   * Process inputs and attributes of a topology.
   *
   * @param archiveRoot
   *          the in-memory TOSCA template.
   * @param inputs
   *          the user's inputs to the template.
   * @param runtimeProperties
   *          deployment runtimeProperties.
   */
  public void processGetInputAttributes(ArchiveRoot archiveRoot, Map<String, Object> inputs,
      RuntimeProperties runtimeProperties) {
    Optional
        .ofNullable(archiveRoot)
        .map(ArchiveRoot::getTopology)
        .ifPresent(topology -> {
          Map<String, PropertyDefinition> templateInputs =
              Optional
                .ofNullable(topology.getInputs())
                .orElseGet(Collections::emptyMap);

          Map<String, ToscaFunction> functions = new HashMap<>();
          Set<FunctionPropertyValue> processedFunctions = Collections
              .newSetFromMap(new IdentityHashMap<>());

          functions.put(ToscaFunctionConstants.GET_ATTRIBUTE, (FunctionPropertyValue function,
              String propertyName) -> processGetAttribute(function, runtimeProperties,
              propertyName));

          functions.put(ToscaFunctionConstants.GET_INPUT, (FunctionPropertyValue function,
              String propertyName) -> processGetInputFunction(function, templateInputs, inputs,
              propertyName));

          processFunctions(topology, functions, processedFunctions);
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
   *
   * @return the processed outputs
   */
  public Map<String, Object> processOutputs(ArchiveRoot archiveRoot, Map<String, Object> inputs,
      RuntimeProperties runtimeProperties) {
    Map<String, Object> processedOutputs = new HashMap<>();
    Optional.ofNullable(archiveRoot).map(ArchiveRoot::getTopology).ifPresent(topology -> {

      Map<String, ToscaFunction> functions = new HashMap<>();
      Set<FunctionPropertyValue> processedFunctions = Collections
          .newSetFromMap(new IdentityHashMap<>());

      Map<String, NodeTemplate> nodeTemplates =
          CommonUtils.notNullOrDefaultValue(topology.getNodeTemplates(), Collections::emptyMap);

      functions.put(ToscaFunctionConstants.GET_ATTRIBUTE, (FunctionPropertyValue function,
          String propertyName) -> processGetAttribute(function, runtimeProperties,
          propertyName));

      functions.put(ToscaFunctionConstants.GET_PROPERTY, (FunctionPropertyValue function,
          String propertyName) -> processGetProperty(functions, processedFunctions, function,
          nodeTemplates,
          propertyName));

      Map<String, PropertyDefinition> templateInputs =
          CommonUtils.notNullOrDefaultValue(topology.getInputs(), Collections::emptyMap);

      functions.put(ToscaFunctionConstants.GET_INPUT, (FunctionPropertyValue function,
          String propertyName) -> processGetInputFunction(function, templateInputs, inputs,
          propertyName));

      Map<String, OutputDefinition> outputs =
          CommonUtils.notNullOrDefaultValue(topology.getOutputs(), Collections::emptyMap);

      outputs.forEach((name, output) -> {
        try {
          processFunctions(functions, processedFunctions, output.getValue(),
              String.format("outputs[%s][value]", name))
              .ifPresent(processedOutput -> processedOutputs
                  .put(name, ToscaUtils.unwrapPropertyValue(processedOutput)));
        } catch (RuntimeException ex) {
          processedOutputs.put(name, ex.getMessage());
        }
      });
    });
    return processedOutputs;
  }

  /**
   * Process the functions of a topology.
   *
   * @param archiveRoot
   *     the in-memory TOSCA template.
   * @param inputs
   *     the user's inputs to the template.
   * @param runtimeProperties
   *     deployment runtimeProperties.
   */
  public void processFunctions(ArchiveRoot archiveRoot, Map<String, Object> inputs,
      RuntimeProperties runtimeProperties) {
    Optional
        .ofNullable(archiveRoot)
        .map(ArchiveRoot::getTopology)
        .ifPresent(topology -> {
          Map<String, ToscaFunction> functions = new HashMap<>();
          Set<FunctionPropertyValue> processedFunctions = Collections
              .newSetFromMap(new IdentityHashMap<>());

          Map<String, NodeTemplate> nodeTemplates =
              CommonUtils.notNullOrDefaultValue(topology.getNodeTemplates(), Collections::emptyMap);

          functions.put(ToscaFunctionConstants.GET_ATTRIBUTE, (FunctionPropertyValue function,
              String propertyName) -> processGetAttribute(function, runtimeProperties,
              propertyName));

          functions.put(ToscaFunctionConstants.GET_PROPERTY, (FunctionPropertyValue function,
              String propertyName) -> processGetProperty(functions, processedFunctions, function,
              nodeTemplates,
              propertyName));

          Map<String, PropertyDefinition> templateInputs =
              CommonUtils.notNullOrDefaultValue(topology.getInputs(), Collections::emptyMap);

          functions.put(ToscaFunctionConstants.GET_INPUT, (FunctionPropertyValue function,
              String propertyName) -> processGetInputFunction(function, templateInputs, inputs,
              propertyName));

          processFunctions(topology, functions, processedFunctions);
        });
  }

  protected void processFunctions(Topology topology, Map<String, ToscaFunction> functions,
      Set<FunctionPropertyValue> processedFunctions) {

    Map<String, NodeTemplate> nodes =
        Optional
            .ofNullable(topology.getNodeTemplates())
            .orElseGet(Collections::emptyMap);

    // Process policies
    Optional
        .ofNullable(topology.getPolicies())
        .orElseGet(Collections::emptyMap)
        .forEach((policyName, policy) -> processProperties(functions, processedFunctions,
            policy.getProperties(),
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
      processProperties(functions, processedFunctions, nodeTemplate.getProperties(),
          String.format("node_templates[%s][properties]", nodeName));

      // process node's relationships
      Optional
          .ofNullable(nodeTemplate.getRelationships())
          .orElseGet(Collections::emptyMap)
          .forEach(
              (relationshipName, relationship) -> processProperties(functions, processedFunctions,
                  relationship.getProperties(),
                  String.format(
                      "node_templates[%s][requirements][%s][relationship][%s][properties]",
                      nodeName, relationship.getRequirementName(), relationshipName)));

      // process node's capabilities
      Optional
          .ofNullable(nodeTemplate.getCapabilities())
          .orElseGet(Collections::emptyMap)
          .forEach((capabilityName, capability) -> processProperties(functions, processedFunctions,
              capability.getProperties(), String.format(
                  "node_templates[%s][capabilities][%s][properties]", nodeName, capabilityName)));

      // process node's artifacts
      Optional
          .ofNullable(nodeTemplate.getArtifacts())
          .orElseGet(Collections::emptyMap)
          .forEach(
              (artifactName, artifact) -> {
                Matcher m = GET_INPUT_PATTERN.matcher(artifact.getArtifactRef());
                if (m.matches()) {
                  FunctionPropertyValue inputFunction = new FunctionPropertyValue(
                      ToscaFunctionConstants.GET_INPUT, Lists.newArrayList(m.group("inputName")));
                  processFunctions(functions, processedFunctions, inputFunction, String
                      .format("node_templates[%s][artifacts][%s][file]", nodeName, artifactName))
                      .map(ToscaUtils::unwrapPropertyValue)
                      .map(Object::toString)
                      .ifPresent(artifact::setArtifactRef);
                }
              });

      // process node's interfaces
      Optional
          .ofNullable(nodeTemplate.getInterfaces())
          .orElseGet(Collections::emptyMap)
          .forEach((interfaceName, toscaInterface) ->
            Optional
                .ofNullable(toscaInterface.getOperations())
                .orElseGet(Collections::emptyMap)
                .forEach(
                    (operationName, operation) -> processProperties(functions, processedFunctions,
                        operation.getInputParameters(),
                        String.format("node_templates[%s][interfaces][%s][inputs][%s]", nodeName,
                            interfaceName, operationName))));

    });
  }

  @SuppressWarnings("unchecked")
  protected Optional<Object> processFunctions(
      Map<String, ToscaFunction> functions,
      Set<FunctionPropertyValue> processedFunctions,
      Object propertyValue,
      String propertyName) {
    if (propertyValue == null) {
      return Optional.empty();
    } else if (propertyValue instanceof FunctionPropertyValue) {
      FunctionPropertyValue functionPropertyValue = (FunctionPropertyValue) propertyValue;
      boolean circular = !processedFunctions.add(functionPropertyValue);
      if (circular) {
        throw new ToscaException(
            this.functionPropertyValueFailedMessage(functionPropertyValue, propertyName,
                "Circular reference between functions"));
      }
      if (ToscaFunctionConstants.CONCAT.equals(functionPropertyValue.getFunction())) {
        return processConcat(functions, processedFunctions, functionPropertyValue, propertyName);
      } else if (ToscaFunctionConstants.TOKEN.equals(functionPropertyValue.getFunction())) {
        return processToken(functions, processedFunctions, functionPropertyValue, propertyName);
      } else {
        return functions
            .getOrDefault(functionPropertyValue.getFunction(),
                IndigoInputsPreProcessorService::noOp)
            .apply(functionPropertyValue, propertyName);
      }
    } else if (propertyValue instanceof PropertyValue) {
      return processFunctions(functions, processedFunctions,
          ToscaUtils.unwrapPropertyValue(propertyValue), propertyName);
    } else if (propertyValue instanceof List) {
      return processFunctions(functions, processedFunctions, (List<Object>) propertyValue,
          propertyName);
    } else if (propertyValue instanceof Map) {
      return processFunctions(functions, processedFunctions, (Map<String, Object>) propertyValue,
          propertyName);
    } else if (CommonUtils.isPrimitive(propertyValue)) {
      return Optional.of(propertyValue);
    } else {
      throw new IllegalArgumentException("Unexpected property of type " + propertyValue.getClass()
          + " found evaluating functions");
    }
  }

  protected Optional<Object> processFunctions(
      Map<String, ToscaFunction> functions,
      Set<FunctionPropertyValue> processedFunctions,
      Map<String, Object> optionalProperties,
      String propertyName) {

    if (optionalProperties == null) {
      return Optional.empty();
    }
    for (Iterator<Entry<String, Object>> it = optionalProperties.entrySet().iterator();
        it.hasNext(); ) {
      Entry<String, Object> entry = it.next();
      Optional<Object> processedValue = processFunctions(functions, processedFunctions,
          entry.getValue(),
          String.format("%s[%s]", propertyName, entry.getKey()));
      if (processedValue.isPresent()) {
        optionalProperties.put(entry.getKey(), processedValue.get());
      } else {
        it.remove();
      }
    }
    return Optional.of(optionalProperties);
  }

  protected Optional<Object> processFunctions(Map<String, ToscaFunction> functions,
      Set<FunctionPropertyValue> processedFunctions,
      List<Object> optionalProperties,
      String propertyName) {

    if (optionalProperties == null) {
      return Optional.empty();
    }

    for (ListIterator<Object> it = optionalProperties.listIterator(); it.hasNext(); ) {
      int currentIndex = it.nextIndex();
      Object entry = it.next();
      Optional<Object> processedValue = processFunctions(functions, processedFunctions, entry,
          String.format("%s[%s]", propertyName, currentIndex));
      if (processedValue.isPresent()) {
        it.set(processedValue.get());
      } else {
        it.remove();
      }
    }
    return Optional.of(optionalProperties);
  }

  @SuppressWarnings("unchecked")
  private <V extends IValue> Optional<Object> processProperties(
      Map<String, ToscaFunction> functions,
      Set<FunctionPropertyValue> processedFunctions,
      Map<String, V> optionalProperties,
      String basePath) {
    if (optionalProperties == null) {
      return Optional.empty();
    }
    for (Iterator<Entry<String, V>> it = optionalProperties.entrySet()
        .iterator();
        it.hasNext(); ) {
      Entry<String, V> entry = it.next();
      Optional<Object> processedValue = processFunctions(functions, processedFunctions,
          entry.getValue(), String.format("%s[%s]", basePath, entry.getKey()));
      if (processedValue.isPresent()) {
        optionalProperties
            .put(entry.getKey(), (V) ToscaUtils.wrapToPropertyValue(processedValue.get()));
      } else {
        it.remove();
      }
    }
    return Optional.of(optionalProperties);
  }

  private Optional<Object> processConcat(Map<String, ToscaFunction> functions,
      Set<FunctionPropertyValue> processedFunctions,
      FunctionPropertyValue concatPropertyValue, String propertyName) {
    processFunctions(functions, processedFunctions, concatPropertyValue.getParameters(),
        String.format(propertyName + "[concat]", propertyName));
    boolean allParametersAreScalarValues = concatPropertyValue
        .getParameters()
        .stream()
        .allMatch(CommonUtils::isPrimitive);
    if (allParametersAreScalarValues) {
      // if all the parameters of the concat function are scalars they can be joined
      // if they are not it means that some functions haven't been evaluated
      // TODO should we raise an error if Complex or ListPropertyValue are encountered?
      return Optional.of(concatPropertyValue
          .getParameters()
          .stream()
          .map(String::valueOf)
          .collect(Collectors.joining()));
    } else {
      return Optional.of(concatPropertyValue);
    }
  }

  private Optional<Object> processToken(Map<String, ToscaFunction> functions,
      Set<FunctionPropertyValue> processedFunctions,
      FunctionPropertyValue tokenPropertyValue, String propertyName) {
    Object rawStringWithTokens = tokenPropertyValue.getParameters().get(0);
    String stringOfTokenChars = (String) tokenPropertyValue.getParameters().get(1);
    int substringIndex = Integer.valueOf(tokenPropertyValue.getParameters().get(2).toString());

    Optional<Object> processedValue = processFunctions(functions, processedFunctions,
        rawStringWithTokens,
        String.format(propertyName + "[token][0]", propertyName))
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .map(s -> s.split(Pattern.quote(stringOfTokenChars))[substringIndex]);
    if (processedValue.isPresent()) {
      return processedValue;
    } else {
      return Optional.of(tokenPropertyValue);
    }
  }

  protected static Optional<Object> noOp(FunctionPropertyValue function,
      String propertyName) {
    return Optional.ofNullable(function);
  }

  protected Optional<Object> processGetInputFunction(FunctionPropertyValue function,
      Map<String, PropertyDefinition> templateInputs, Map<String, Object> inputs,
      String propertyName) {
    String inputName = function.getTemplateName();
    // Alien4Cloud already validates existing input name
    PropertyDefinition templateInput = CommonUtils
        .getFromOptionalMap(templateInputs, inputName)
        .orElseThrow(() -> new ToscaException(
            functionPropertyValueFailedMessage(function, propertyName,
                "No input with name <" + inputName + "> defined in the template")));
    // Look for user's given input, if not present use the default one
    Object inputValue = Optional
        .ofNullable(inputs.getOrDefault(inputName, templateInput.getDefault()))
        .map(ToscaUtils::unwrapPropertyValue)
        // No given input or default value available -> error
        .orElseThrow(() -> new ToscaException(
            functionPropertyValueFailedMessage(function, propertyName,
                "No input provided for <" + inputName
                    + "> and no default value provided in the definition")));

    LOG.debug(
        "TOSCA input function replacement: <{}>, input name <{}>, input value <{}>",
        propertyName, inputName, inputValue);

    return Optional.of(inputValue);

  }

  protected Optional<Object> processGetAttribute(FunctionPropertyValue function,
      RuntimeProperties runtimeProperties, String propertyName) {
    Object returnValue = runtimeProperties.get(RuntimeProperties.escapedPath(function));
    if (returnValue == null) {
      throw new ToscaException(
          functionPropertyValueFailedMessage(function, propertyName, "Value not found"));
    } else {
      return Optional.of(returnValue);
    }
  }

  private String functionPropertyValueFailedMessage(FunctionPropertyValue function,
      String propertyName, String message) {
    return String.format(
        "Failed to evaluate %s[%s][%s]: %s",
        propertyName,
        function.getFunction(),
        CommonUtils
            .nullableCollectionToStream(function.getParameters())
            .map(Object::toString)
            .collect(Collectors.joining(", ")),
        message
    );
  }

  @SuppressWarnings("rawtypes")
  protected Optional<Object> processGetProperty(
      Map<String, ToscaFunction> functions,
      Set<FunctionPropertyValue> processedFunctions,
      FunctionPropertyValue function,
      @NonNull Map<String, NodeTemplate> nodeTemplates, String propertyName) {
    return CommonUtils
        .getFromOptionalMap(nodeTemplates, function.getTemplateName())
        .flatMap(nodeTemplate -> {
          String firstParam = function.getTemplateName();
          List<String> additionalParameters = function
              .getParameters()
              .stream()
              .skip(1)
              .map(Object::toString)
              .collect(Collectors.toList());
          Optional<AbstractPropertyValue> rootProperty =
              CommonUtils.getFromOptionalMap(nodeTemplate.getProperties(), firstParam);
          if (!rootProperty.isPresent() && !additionalParameters.isEmpty()) {
            String optionalReqOrCapPropertyName = additionalParameters.get(0);
            additionalParameters = additionalParameters.subList(1, additionalParameters.size());
            // Check if is a Capability
            rootProperty = Optional.ofNullable(nodeTemplate.getCapabilities())
                .map(capabilities -> capabilities.get(firstParam))
                .flatMap(capability -> CommonUtils
                    .getFromOptionalMap(capability.getProperties(), optionalReqOrCapPropertyName));
            if (!rootProperty.isPresent()) {
              // Check if is a Requirement
              rootProperty = Optional.ofNullable(nodeTemplate.getRelationships())
                  .map(requirements -> requirements.get(firstParam))
                  .flatMap(requirement -> CommonUtils
                      .getFromOptionalMap(requirement.getProperties(),
                          optionalReqOrCapPropertyName));
            }
          }
          if (rootProperty.isPresent()) {
            Optional<Object> propertyValue = rootProperty.map(ToscaUtils::unwrapPropertyValue);
            for (String additionalParameter : additionalParameters) {
              if (propertyValue.filter(Map.class::isInstance).isPresent()) {
                Object rawProperty = ((Map) propertyValue.get()).get(additionalParameter);
                propertyValue = processFunctions(functions, processedFunctions, rawProperty,
                    additionalParameter);
              } else if (propertyValue.filter(List.class::isInstance).isPresent()) {
                int index = Integer.parseInt(additionalParameter);
                Object rawProperty = ((List) propertyValue.get()).get(index);
                propertyValue = processFunctions(functions, processedFunctions, rawProperty,
                    additionalParameter);
              } else {
                throw new ToscaException(functionPropertyValueFailedMessage(function, propertyName,
                    "Parameter " + additionalParameter
                        + "provided but the pointed property is not complex"));
              }
            }
            return propertyValue;
          } else {
            throw new ToscaException(
                functionPropertyValueFailedMessage(function, propertyName, "Value not found"));
          }
        });

  }

  @Data
  public static class RuntimeProperties {

    @NonNull
    Map<String, Object> vaules = new HashMap<>();

    /**
     * Put a list of runtime propeties.
     *
     * @param list
     *     the list
     * @param basekeys
     *     the path chunks associated with the properties
     * @return this object for chaining
     */
    public RuntimeProperties put(List<?> list, String... basekeys) {
      vaules.put(escapedPath(Stream.of(basekeys)), list);
      for (int i = 0; i < list.size(); ++i) {
        vaules.put(escapedPath(
            Stream.concat(Stream.of(basekeys), Stream.of(String.valueOf(i)))), list.get(i));
      }
      return this;
    }

    /**
     * Put a runtime property.
     *
     * @param item
     *     the property
     * @param basekeys
     *     the path chunks associated with the property
     * @return this object for chaining
     */
    public RuntimeProperties put(Object item, String... basekeys) {
      vaules.put(escapedPath(Stream.of(basekeys)), item);
      return this;
    }

    public Object get(String key) {
      return vaules.get(key);
    }

    private static String escapedPath(Stream<String> stream) {
      return stream.collect(Collectors.joining("."));
    }

    private static String escapedPath(FunctionPropertyValue function) {
      return escapedPath(function.getParameters().stream().map(Object::toString));
    }
  }

}
