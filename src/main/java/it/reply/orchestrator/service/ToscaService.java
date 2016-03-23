package it.reply.orchestrator.service;

import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;

import com.sun.istack.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

public interface ToscaService {

  @Nonnull
  public ParsingResult<ArchiveRoot> getArchiveRootFromTemplate(@Nonnull String toscaTemplate)
      throws IOException, ParsingException;

  @Nonnull
  public String getTemplateFromTopology(@Nonnull ArchiveRoot archiveRoot) throws IOException;

  @Nonnull
  public String customizeTemplate(@Nonnull String toscaTemplate, @NotNull String deploymentId)
      throws IOException;

  @Nonnull
  public Capability getNodeCapabilityByName(NodeTemplate node, String propertyName);

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
}
