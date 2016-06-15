package it.reply.orchestrator.service;

import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.PropertyValue;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;

import com.sun.istack.NotNull;

import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.exception.service.ToscaException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

public interface ToscaService {

  @Nonnull
  public ParsingResult<ArchiveRoot> getArchiveRootFromTemplate(@Nonnull String toscaTemplate)
      throws IOException, ParsingException;

  /**
   * Obtain the string TOSCA template representation from the in-memory representation. <br/>
   * <b>WARNING: Some nodes or properties might be missing!! Use at your own risk!</b>
   * 
   * @param archiveRoot
   * @return
   * @throws IOException
   */
  @Nonnull
  public String getTemplateFromTopology(@Nonnull ArchiveRoot archiveRoot) throws IOException;

  @Nonnull
  public String customizeTemplate(@Nonnull String toscaTemplate, @NotNull String deploymentId)
      throws IOException, ToscaException, ParsingException;

  public void addDeploymentId(ArchiveRoot parsingResult, String deploymentId);

  /**
   * Verifies that all the template's required inputs are present in the user's input list.
   * 
   * @param templateInputs
   *          the templates's defined inputs.
   * @param inputs
   *          the user's input list.
   * @throws ToscaException
   *           in case a required input is not present in the user's input or if the user's input
   *           value doesn't match the defined input type.
   */
  public void validateUserInputs(Map<String, PropertyDefinition> templateInputs,
      Map<String, Object> inputs) throws ToscaException;

  /**
   * Replaces TOSCA input functions with the actual input values (user's input values or default
   * ones).
   * 
   * @param archiveRoot
   *          the in-memory TOSCA template.
   * @param inputs
   *          the user's inputs to the template.
   * @throws ToscaException
   *           if the input replacement fails TODO.
   */
  @Nonnull
  public void replaceInputFunctions(@Nonnull ArchiveRoot archiveRoot, Map<String, Object> inputs)
      throws ToscaException;

  /**
   * Parse the TOSCA template (string) and get the in-memory representation.<br/>
   * This also checks for validation errors.
   * 
   * @param toscaTemplate
   *          the TOSCA template as string.
   * @return an {@link ArchiveRoot} representing the template.
   * @throws IOException
   *           if an I/O error occurs (converting the string to a CSAR zipped archive internally).
   * @throws ParsingException
   *           if parsing errors occur.
   * @throws ToscaException
   *           if validation errors occur.
   */
  @Nonnull
  public ArchiveRoot parseTemplate(@Nonnull String toscaTemplate)
      throws IOException, ParsingException, ToscaException;

  /**
   * As for {@link #parseTemplate(String)} but also validates user's inputs.
   * 
   * @param toscaTemplate
   *          the TOSCA template as string.
   * @return an {@link ArchiveRoot} representing the template.
   * @throws IOException
   *           if an I/O error occurs (converting the string to a CSAR zipped archive internally).
   * @throws ParsingException
   *           if parsing errors occur.
   * @throws ToscaException
   *           if validation errors occur.
   */
  @Nonnull
  public ArchiveRoot parseAndValidateTemplate(@Nonnull String toscaTemplate,
      Map<String, Object> inputs) throws IOException, ParsingException, ToscaException;

  /**
   * As for {@link #parseAndValidateTemplate(String)} but also replaces the user's inputs.
   * 
   * @param toscaTemplate
   *          the TOSCA template as string.
   * @return an {@link ArchiveRoot} representing the template.
   * @throws IOException
   *           if an I/O error occurs (converting the string to a CSAR zipped archive internally).
   * @throws ParsingException
   *           if parsing errors occur.
   * @throws ToscaException
   *           if validation errors occur.
   */
  @Nonnull
  public ArchiveRoot prepareTemplate(@Nonnull String toscaTemplate, Map<String, Object> inputs)
      throws IOException, ParsingException, ToscaException;

  @Nonnull
  public Capability getNodeCapabilityByName(NodeTemplate node, String propertyName);

  @Nonnull
  public AbstractPropertyValue getNodePropertyByName(NodeTemplate node, String propertyName);

  @Nonnull
  public PropertyValue<?> getNodePropertyValueByName(NodeTemplate node, String propertyName);

  @Nonnull
  public PropertyValue<?> getCapabilityPropertyValueByName(Capability capability,
      String propertyName);

  public List<RelationshipTemplate> getRelationshipTemplatesByCapabilityName(
      Map<String, RelationshipTemplate> relationships, String capabilityName);

  /**
   * Finds all the nodes associated to the given {@link NodeTemplate} with a capability with the
   * given name.
   * 
   * @param nodes
   *          the template's node map.
   * @param nodeTemplate
   *          the origin node.
   * @param capabilityName
   *          the name of the capability.
   * @return a map with the nodes (and their names) associated to the origin node with given
   *         capability.
   */
  public Map<String, NodeTemplate> getAssociatedNodesByCapability(Map<String, NodeTemplate> nodes,
      NodeTemplate nodeTemplate, String capabilityName);

  public Map<String, NodeTemplate> getCountNodes(ArchiveRoot archiveRoot);

  public int getCount(NodeTemplate nodeTemplate);

  /**
   * Get the list of resources to be removed.
   * 
   * @param nodeTemplate
   *          {@link NodeTemplate}
   * @return the list of resources to be removed or an empty list
   */
  public List<String> getRemovalList(NodeTemplate nodeTemplate);

  public String updateTemplate(String template) throws IOException;

  public String updateCount(ArchiveRoot archiveRoot, int count) throws IOException;

  /**
   * Extracts OneData requirements (i.e. space, favorite providers, etc) from the TOSCA template.
   * 
   * @param archiveRoot
   *          an {@link ArchiveRoot} representing the template.
   * @param inputs
   *          the user's input list.
   * @return a Map of {@link OneData} requirement, index by node name.<br/>
   *         <b>WARNING:</b> (TEMPORARY) currently OneData nodes are not supported; thus the name
   *         used are hard-coded and are either 'input', 'output' or 'service'.
   */
  public Map<String, OneData> extractOneDataRequirements(ArchiveRoot archiveRoot,
      Map<String, Object> inputs);
}