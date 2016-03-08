package it.reply.orchestrator.service;

import java.io.IOException;

import javax.annotation.Nonnull;

import alien4cloud.model.topology.Topology;
import alien4cloud.tosca.parser.ParsingException;

public interface ToscaService {

  public @Nonnull String getTemplateFromTopology(@Nonnull Topology topology);

  public @Nonnull Topology getTopologyFromTemplate(@Nonnull String toscaTemplate)
      throws IOException, ParsingException;

}
