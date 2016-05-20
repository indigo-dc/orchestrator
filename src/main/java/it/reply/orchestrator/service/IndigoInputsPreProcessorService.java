package it.reply.orchestrator.service;

import alien4cloud.deployment.InputsPreProcessorService;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.normative.ToscaFunctionConstants;

import it.reply.orchestrator.exception.service.ToscaException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Map.Entry;

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
public class IndigoInputsPreProcessorService {

  private static final Logger LOG = LogManager.getLogger(IndigoInputsPreProcessorService.class);

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
  public void processGetInput(ArchiveRoot archiveRoot, Map<String, String> inputs)
      throws ToscaException {

    Map<String, NodeTemplate> nodes = archiveRoot.getTopology().getNodeTemplates();
    Map<String, PropertyDefinition> templateInputs = archiveRoot.getTopology().getInputs();

    // Iterate on each property that is a FunctionPropertyValue

    // For each template's node
    if (nodes != null) {
      for (Entry<String, NodeTemplate> entry : nodes.entrySet()) {
        NodeTemplate nodeTemplate = entry.getValue();
        // process node's properties
        processGetInput(templateInputs, inputs, nodeTemplate.getProperties(), entry.getKey());

        // process node's relationships
        if (nodeTemplate.getRelationships() != null) {
          for (Entry<String, RelationshipTemplate> relEntry : nodeTemplate.getRelationships()
              .entrySet()) {
            RelationshipTemplate relationshipTemplate = relEntry.getValue();
            processGetInput(templateInputs, inputs, relationshipTemplate.getProperties(),
                relEntry.getKey());
          }
        }
        // process node's capabilities
        if (nodeTemplate.getCapabilities() != null) {
          for (Entry<String, Capability> capaEntry : nodeTemplate.getCapabilities().entrySet()) {
            Capability capability = capaEntry.getValue();
            processGetInput(templateInputs, inputs, capability.getProperties(), capaEntry.getKey());
          }
        }
      }
    }
  }

  protected void processGetInput(Map<String, PropertyDefinition> templateInputs,
      Map<String, String> inputs, Map<String, AbstractPropertyValue> properties,
      String objectName) {

    if (properties != null) {
      // For each property
      for (Map.Entry<String, AbstractPropertyValue> propEntry : properties.entrySet()) {
        // Only FunctionPropertyValue are interesting
        if (propEntry.getValue() instanceof FunctionPropertyValue) {
          FunctionPropertyValue function = (FunctionPropertyValue) propEntry.getValue();
          // Only INPUT functions are interesting
          if (ToscaFunctionConstants.GET_INPUT.equals(function.getFunction())) {

            try {
              String inputName = function.getParameters().get(0);
              // Alien4Cloud already validates existing input name
              PropertyDefinition templateInput = templateInputs.get(inputName);
              // Look for user's given input
              String inputValue = inputs.get(inputName);

              // If not null, replace the input value. Otherwise, use default value.
              if (inputValue == null) {
                inputValue = templateInput.getDefault();
              }

              // Replace property value (was Function, now Scalar)
              propEntry.setValue(new ScalarPropertyValue(inputValue));

              LOG.debug(
                  "TOSCA input function replacement: object <{}>, property <{}>, input name <{}>, input value <{}>",
                  objectName, propEntry.getKey(), inputName, inputValue);
            } catch (Exception ex) {
              throw new ToscaException(String.format(
                  "Failed to replace input function on object <%s>, property <%s>, parameters <%s>: %s",
                  objectName, propEntry.getKey(), function.getParameters(), ex.getMessage()), ex);
            }
          } else {
            String msg = String.format(
                "Function <%s> detected for property <%s> while only <get_input> should be authorized.",
                function.getFunction(), propEntry.getKey());
            LOG.error(msg);
            throw new ToscaException(msg);
          }
        }
      }
    }
  }
}
