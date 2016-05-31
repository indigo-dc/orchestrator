package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.service.WorkflowConstants;
import it.reply.workflowmanager.spring.orchestrator.bpm.ejbcommands.BaseCommand;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.stereotype.Component;

@Component
public abstract class BaseRankCloudProvidersCommand extends BaseCommand {

  private static final Logger LOG = LogManager.getLogger(BaseRankCloudProvidersCommand.class);

  @Override
  protected final ExecutionResults customExecute(CommandContext ctx) throws Exception {
    RankCloudProvidersMessage rankCloudProvidersMessage =
        (RankCloudProvidersMessage) getWorkItem(ctx)
            .getParameter(WorkflowConstants.WF_PARAM_RANK_CLOUD_PROVIDERS_MESSAGE);

    ExecutionResults exResults = new ExecutionResults();
    try {
      rankCloudProvidersMessage = customExecute(rankCloudProvidersMessage);
      exResults.setData(WorkflowConstants.WF_PARAM_RANK_CLOUD_PROVIDERS_MESSAGE,
          rankCloudProvidersMessage);
      exResults.getData().putAll(resultOccurred(true).getData());
    } catch (Exception e) {
      LOG.error(e);
      exResults.getData().putAll(resultOccurred(false).getData());
    }

    return exResults;
  }

  protected abstract RankCloudProvidersMessage
      customExecute(RankCloudProvidersMessage rankCloudProvidersMessage);

}
