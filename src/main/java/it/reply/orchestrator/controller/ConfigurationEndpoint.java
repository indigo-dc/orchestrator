package it.reply.orchestrator.controller;

import it.reply.orchestrator.dto.SystemEndpoints;
import it.reply.orchestrator.service.ConfigurationService;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "endpoints.configuration")
public class ConfigurationEndpoint extends AbstractEndpoint<SystemEndpoints> {

  private ConfigurationService configurationService;

  public ConfigurationEndpoint(ConfigurationService configurationService) {
    super("configuration", false);
    this.configurationService = configurationService;
  }

  @Override
  public SystemEndpoints invoke() {
    return configurationService.getConfiguration();
  }
}
