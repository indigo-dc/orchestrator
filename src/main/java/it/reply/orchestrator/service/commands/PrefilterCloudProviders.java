package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.dto.RankCloudProvidersMessage;

import org.springframework.stereotype.Component;

@Component
public class PrefilterCloudProviders extends BaseRankCloudProvidersCommand {

  @Override
  protected RankCloudProvidersMessage
      customExecute(RankCloudProvidersMessage rankCloudProvidersMessage) {
    // TODO Filter cloud providers (i.e. based on OneData)
    return rankCloudProvidersMessage;
  }

}
