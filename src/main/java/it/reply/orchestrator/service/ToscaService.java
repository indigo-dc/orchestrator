package it.reply.orchestrator.service;

import java.io.IOException;

import javax.annotation.Nonnull;

import alien4cloud.model.topology.Topology;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingException;

public interface ToscaService {

  public @Nonnull ArchiveRoot getArchiveRootFromTemplate(@Nonnull String toscaTemplate)
      throws IOException, ParsingException;

  public @Nonnull String getTemplateFromTopology(@Nonnull ArchiveRoot archiveRoot)
      throws IOException;

}
