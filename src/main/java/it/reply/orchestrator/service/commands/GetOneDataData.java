package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.service.OneDataService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GetOneDataData extends BaseRankCloudProvidersCommand {

  private static final Logger LOG = LogManager.getLogger(GetOneDataData.class);

  @Autowired
  private OneDataService oneDataService;

  @Override
  protected RankCloudProvidersMessage customExecute(
      RankCloudProvidersMessage rankCloudProvidersMessage) {

    Map<String, OneData> oneDataRequirements = rankCloudProvidersMessage.getOneDataRequirements();

    OneData inputRequirement = oneDataRequirements.get("input");
    if (inputRequirement != null) {
      oneDataService.populateProviderInfo(inputRequirement);
    }

    OneData outputRequirement = oneDataRequirements.get("output");
    if (outputRequirement != null) {
      oneDataService.populateProviderInfo(outputRequirement);
    }

    return rankCloudProvidersMessage;
  }

}
