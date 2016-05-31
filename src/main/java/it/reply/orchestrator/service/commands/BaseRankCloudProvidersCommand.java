package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.service.WorkflowConstants;
import it.reply.orchestrator.service.deployment.providers.DeploymentStatusHelper;
import it.reply.workflowmanager.spring.orchestrator.bpm.ejbcommands.BaseCommand;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base behavior for all RankCloudProvider WF tasks. <br/>
 * This checks input parameters and manages output and errors (specifically, in case of errors, it
 * also updates directly the deployment status on DB).
 * 
 * @author l.biava
 *
 */
public abstract class BaseRankCloudProvidersCommand extends BaseCommand {

  private static final Logger LOG = LogManager.getLogger(BaseRankCloudProvidersCommand.class);

  @Autowired
  protected DeploymentStatusHelper deploymentStatusHelper;

  /**
   * <b>This method SHOULD NOT be overridden! It cannot be final for INJECTION purpose!</b> <br/>
   * Use the {@link #customExecute(RankCloudProvidersMessage)} method to implement command logic.
   */
  @Override
  protected ExecutionResults customExecute(CommandContext ctx) throws Exception {
    RankCloudProvidersMessage rankCloudProvidersMessage =
        (RankCloudProvidersMessage) getWorkItem(ctx)
            .getParameter(WorkflowConstants.WF_PARAM_RANK_CLOUD_PROVIDERS_MESSAGE);

    ExecutionResults exResults = new ExecutionResults();
    try {
      if (rankCloudProvidersMessage == null) {
        throw new IllegalArgumentException(String.format("WF parameter <%s> cannot be null",
            WorkflowConstants.WF_PARAM_RANK_CLOUD_PROVIDERS_MESSAGE));
      }

      rankCloudProvidersMessage = customExecute(rankCloudProvidersMessage);
      exResults.setData(WorkflowConstants.WF_PARAM_RANK_CLOUD_PROVIDERS_MESSAGE,
          rankCloudProvidersMessage);
      exResults.getData().putAll(resultOccurred(true).getData());
    } catch (Exception ex) {
      LOG.error(ex);
      exResults.getData().putAll(resultOccurred(false).getData());

      // Update deployment with error
      // TODO: what if this fails??
      deploymentStatusHelper.updateOnError(rankCloudProvidersMessage.getDeploymentId(), ex);
    }

    return exResults;
  }

  protected abstract RankCloudProvidersMessage
      customExecute(RankCloudProvidersMessage rankCloudProvidersMessage);

}
