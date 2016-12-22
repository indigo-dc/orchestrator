package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.service.CmdbService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GetCmdbDataDeploy extends BaseRankCloudProvidersCommand {

  private static final Logger LOG = LoggerFactory.getLogger(GetCmdbDataDeploy.class);

  @Autowired
  private CmdbService cmdbService;

  @Override
  protected RankCloudProvidersMessage customExecute(
      RankCloudProvidersMessage rankCloudProvidersMessage) {

    // Get CMDB data for each Cloud Provider
    for (Map.Entry<String, CloudProvider> providerEntry : rankCloudProvidersMessage
        .getCloudProviders().entrySet()) {
      CloudProvider cp = providerEntry.getValue();
      cp = cmdbService.fillCloudProviderInfo(cp);
    }

    return rankCloudProvidersMessage;
  }

  @Override
  protected String getErrorMessagePrefix() {
    return "Error retrieving info from CMDB";
  }

}
