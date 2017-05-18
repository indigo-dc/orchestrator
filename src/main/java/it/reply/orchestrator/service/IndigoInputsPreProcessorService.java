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

import alien4cloud.deployment.InputsPreProcessorService;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.ComplexPropertyValue;
import alien4cloud.model.components.DeploymentArtifact;
import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.IValue;
import alien4cloud.model.components.Interface;
import alien4cloud.model.components.ListPropertyValue;
import alien4cloud.model.components.Operation;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.AbstractPolicy;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.normative.ToscaFunctionConstants;

import it.reply.orchestrator.exception.service.ToscaException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

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

    Map<String, NodeTemplate> nodes = archiveRoot.getTopology().getNodeTemplates();
    Map<String, PropertyDefinition> templateInputs = archiveRoot.getTopology().getInputs();

    // Process policies
    if (archiveRoot.getTopology().getPolicies() != null) {
      for (AbstractPolicy policy : archiveRoot.getTopology().getPolicies()) {
        processGetInputInAbstractPropertyValues(templateInputs, inputs, policy.getProperties(),
            policy.getName());
      }
    }
    // Iterate on each property that is a FunctionPropertyValue

    // For each template's node
    if (nodes != null) {
      for (Entry<String, NodeTemplate> entry : nodes.entrySet()) {
        NodeTemplate nodeTemplate = entry.getValue();
        // process node's properties
        processGetInputInAbstractPropertyValues(templateInputs, inputs,
            nodeTemplate.getProperties(), entry.getKey());

        // process node's relationships
        if (nodeTemplate.getRelationships() != null) {
          for (Entry<String, RelationshipTemplate> relEntry : nodeTemplate.getRelationships()
              .entrySet()) {
            RelationshipTemplate relationshipTemplate = relEntry.getValue();
            processGetInputInAbstractPropertyValues(templateInputs, inputs,
                relationshipTemplate.getProperties(), relEntry.getKey());
          }
        }
        // process node's capabilities
        if (nodeTemplate.getCapabilities() != null) {
          for (Entry<String, Capability> capaEntry : nodeTemplate.getCapabilities().entrySet()) {
            Capability capability = capaEntry.getValue();
            processGetInputInAbstractPropertyValues(templateInputs, inputs,
                capability.getProperties(), capaEntry.getKey());
          }
        }
        // process node's artifacts
        if (nodeTemplate.getArtifacts() != null) {
          for (Entry<String, DeploymentArtifact> artifactEntry : nodeTemplate.getArtifacts()
              .entrySet()) {
            DeploymentArtifact artifact = artifactEntry.getValue();
            artifact.setFile(processGetInput(templateInputs, inputs, artifact.getFile(), "file",
                artifactEntry.getKey()));
          }
        }

        // process node's interfaces
        if (nodeTemplate.getInterfaces() != null) {
          for (Entry<String, Interface> interfacesEntry : nodeTemplate.getInterfaces().entrySet()) {
            Interface toscaInterface = interfacesEntry.getValue();
            if (toscaInterface != null && toscaInterface.getOperations() != null) {
              for (Entry<String, Operation> opEntry : toscaInterface.getOperations().entrySet()) {
                Operation op = opEntry.getValue();
                if (op != null) {
                  processGetInputInIvalues(templateInputs, inputs, op.getInputParameters(),
                      opEntry.getKey());
                }
              }
            }
          }
        }
      }
    }
  }

  protected AbstractPropertyValue processGetInput(Map<String, PropertyDefinition> templateInputs,
      Map<String, Object> inputs, AbstractPropertyValue propertyValue, String propertyName,
      String objectName) {
    // Only FunctionPropertyValue are interesting
    if (propertyValue instanceof FunctionPropertyValue) {
      FunctionPropertyValue function = (FunctionPropertyValue) propertyValue;
      // Only INPUT functions are interesting
      if (ToscaFunctionConstants.GET_INPUT.equals(function.getFunction())) {

        try {
          if (templateInputs == null) {
            throw new IllegalArgumentException("Empty template input list");
          }

          String inputName = function.getParameters().get(0);
          // Alien4Cloud already validates existing input name
          PropertyDefinition templateInput = templateInputs.get(inputName);
          // Look for user's given input
          Object inputValue = inputs.get(inputName);

          // If not null, replace the input value. Otherwise, use default value.
          if (inputValue == null) {
            inputValue = templateInput.getDefault();

            // No given input or default value available -> error
            if (inputValue == null) {
              throw new IllegalArgumentException("No given input or default value available");
            }
          }

          LOG.debug(
              "TOSCA input function replacement: "
                  + "object <{}>, property <{}>, input name <{}>, input value <{}>",
              objectName, propertyName, inputName, inputValue);

          // Replace property value (was Function, now Scalar)
          AbstractPropertyValue val;
          if (inputValue instanceof String || inputValue instanceof Boolean
              || inputValue instanceof Integer || inputValue instanceof Double
              || inputValue instanceof Float) {
            val = new ScalarPropertyValue(inputValue.toString());
          } else if (inputValue instanceof List) {
            List<Object> list = new ArrayList<>();
            for (Object entry : ((List<?>) inputValue)) {
              val = new ScalarPropertyValue(entry.toString());
              val.setPrintable(true);
              list.add(val);
            }
            val = new ListPropertyValue(list);
          } else if (inputValue instanceof Map) {
            val = new ComplexPropertyValue((Map<String, Object>) inputValue);
          } else {
            throw new IllegalArgumentException(
                String.format("Unsupported input type for value <%s> of class <%s>", inputValue,
                    inputValue.getClass()));
          }

          val.setPrintable(true);
          return val;

        } catch (Exception ex) {
          throw new ToscaException(String.format(
              "Failed to replace input function on object <%s>, property <%s>, parameters <%s>: %s",
              objectName, propertyName, function.getParameters(), ex.getMessage()), ex);
        }
      }
    } else if (propertyValue instanceof ComplexPropertyValue) {
      // Complex or List properties might contain other function as their values

      // Look for function in the value
      for (Map.Entry<String, Object> complexEntry : ((ComplexPropertyValue) propertyValue)
          .getValue().entrySet()) {
        // Only AbstractPropertyValue values can have functions in them
        if (complexEntry.getValue() instanceof AbstractPropertyValue) {
          AbstractPropertyValue newValue = processGetInput(templateInputs, inputs,
              (AbstractPropertyValue) complexEntry.getValue(), complexEntry.getKey(), propertyName);
          // Replace function value with the replaced value (if changed)
          if (!Objects.equals(newValue, complexEntry.getValue())) {
            complexEntry.setValue(newValue);
          }
        }
      }

    } else if (propertyValue instanceof ListPropertyValue) {
      List<Object> list = ((ListPropertyValue) propertyValue).getValue();
      for (int i = 0; i < list.size(); i++) {
        if (list.get(i) instanceof AbstractPropertyValue) {
          AbstractPropertyValue complexValue = (AbstractPropertyValue) list.get(i);
          AbstractPropertyValue newValue =
              processGetInput(templateInputs, inputs, (AbstractPropertyValue) complexValue,
                  String.format("%s[%s]", propertyName, i), propertyName);
          // Replace function value with the replaced value (if changed)
          if (!Objects.equals(newValue, complexValue)) {
            list.set(i, newValue);
          }
        }
      }

    }
    return propertyValue;
  }

  protected void processGetInputInIvalues(Map<String, PropertyDefinition> templateInputs,
      Map<String, Object> inputs, Map<String, IValue> properties, String objectName) {

    if (properties != null) {
      // For each property
      for (Entry<String, IValue> propEntry : properties.entrySet()) {
        if (propEntry.getValue() instanceof AbstractPropertyValue) {
          AbstractPropertyValue newValue = processGetInput(templateInputs, inputs,
              (AbstractPropertyValue) propEntry.getValue(), propEntry.getKey(), objectName);
          // Replace function value with the replaced value (if changed)
          if (!Objects.equals(newValue, propEntry.getValue())) {
            propEntry.setValue(newValue);
          }
        } else {
          // DO NOTHING, NOT A SUBSTITUTABLE TYPE
        }
      }
    }
  }

  protected void processGetInputInAbstractPropertyValues(
      Map<String, PropertyDefinition> templateInputs, Map<String, Object> inputs,
      Map<String, AbstractPropertyValue> properties, String objectName) {

    if (properties != null) {
      // For each property
      for (Map.Entry<String, AbstractPropertyValue> propEntry : properties.entrySet()) {
        AbstractPropertyValue newValue = processGetInput(templateInputs, inputs,
            propEntry.getValue(), propEntry.getKey(), objectName);
        // Replace function value with the replaced value (if changed)
        if (!Objects.equals(newValue, propEntry.getValue())) {
          propEntry.setValue(newValue);
        }
      }
    }
  }
}
