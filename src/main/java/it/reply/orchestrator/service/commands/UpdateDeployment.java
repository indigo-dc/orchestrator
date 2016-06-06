package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.ranker.RankedCloudProvider;
import it.reply.orchestrator.service.WorkflowConstants;
import it.reply.orchestrator.service.deployment.providers.DeploymentStatusHelper;
import it.reply.workflowmanager.spring.orchestrator.bpm.ejbcommands.BaseCommand;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UpdateDeployment extends BaseCommand {

  private static final Logger LOG = LogManager.getLogger(UpdateDeployment.class);

  @Autowired
  private DeploymentRepository deploymentRepository;

  @Autowired
  private DeploymentStatusHelper deploymentStatusHelper;

  @Override
  public ExecutionResults customExecute(CommandContext ctx) throws Exception {

    RankCloudProvidersMessage rankCloudProvidersMessage =
        (RankCloudProvidersMessage) getWorkItem(ctx)
            .getParameter(WorkflowConstants.WF_PARAM_RANK_CLOUD_PROVIDERS_MESSAGE);
    //
    // String deploymentId =
    // (String) getWorkItem(ctx).getParameter(WorkflowConstants.WF_PARAM_DEPLOYMENT_ID);

    ExecutionResults exResults = new ExecutionResults();
    try {
      if (rankCloudProvidersMessage == null) {
        throw new IllegalArgumentException(String.format("WF parameter <%s> cannot be null",
            WorkflowConstants.WF_PARAM_RANK_CLOUD_PROVIDERS_MESSAGE));
      }

      // if (deploymentId == null) {
      // throw new IllegalArgumentException(String.format("WF parameter <%s> cannot be null",
      // WorkflowConstants.WF_PARAM_DEPLOYMENT_ID));
      // }

      // TODO: Move elsewhere
      // Choose Cloud Provider
      LOG.debug("Choosing Cloud Provider based on: {}",
          rankCloudProvidersMessage.getRankedCloudProviders());

      // TODO Check ranker errors (i.e. providers with ranked = false)
      RankedCloudProvider chosenCp = null;
      for (RankedCloudProvider rcp : rankCloudProvidersMessage.getRankedCloudProviders()) {
        if (chosenCp == null || rcp.getRank() < chosenCp.getRank()) {
          chosenCp = rcp;
        }
      }
      LOG.debug("Selected Cloud Provider is: {}", chosenCp);

      // Update Deployment
      Deployment deployment =
          deploymentRepository.findOne(rankCloudProvidersMessage.getDeploymentId());
      deployment.setCloudProviderName(chosenCp.getName());

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

}
