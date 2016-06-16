package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.service.WorkflowConstants;
import it.reply.orchestrator.service.deployment.providers.DeploymentStatusHelper;
import it.reply.workflowmanager.spring.orchestrator.bpm.ejbcommands.BaseCommand;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base behavior for all Deploy WF tasks. <br/>
 * This checks input parameters and manages output and errors (specifically, in case of errors, it
 * also updates directly the deployment status on DB).
 * 
 * @author l.biava
 *
 */
public abstract class BaseDeployCommand extends BaseCommand {

  private static final Logger LOG = LogManager.getLogger(BaseDeployCommand.class);

  @Autowired
  protected DeploymentStatusHelper deploymentStatusHelper;

  @Autowired
  protected DeploymentRepository deploymentRepository;

  /**
   * <b>This method SHOULD NOT be overridden! It cannot be final for INJECTION purpose!</b> <br/>
   * Use the {@link #customExecute(RankCloudProvidersMessage)} method to implement command logic.
   */
  @Override
  protected ExecutionResults customExecute(CommandContext ctx) throws Exception {
    DeploymentMessage deploymentMessage = (DeploymentMessage) getWorkItem(ctx)
        .getParameter(WorkflowConstants.WF_PARAM_DEPLOYMENT_MESSAGE);

    ExecutionResults exResults = new ExecutionResults();
    try {
      if (deploymentMessage == null) {
        throw new IllegalArgumentException(String.format("WF parameter <%s> cannot be null",
            WorkflowConstants.WF_PARAM_DEPLOYMENT_MESSAGE));
      }

      // Load the DB Deployment from ID (this way we avoid jBPM JPA serialization issues)
      deploymentMessage
          .setDeployment(deploymentRepository.findOne(deploymentMessage.getDeploymentId()));

      exResults.getData().putAll(customExecute(ctx, deploymentMessage).getData());
      exResults.setData(WorkflowConstants.WF_PARAM_DEPLOYMENT_MESSAGE, deploymentMessage);
    } catch (Exception ex) {
      LOG.error(ex);
      exResults.getData().putAll(resultOccurred(false).getData());

      // Update deployment with error
      // TODO: what if this fails??
      deploymentStatusHelper.updateOnError(deploymentMessage.getDeploymentId(), ex);
    }

    // Save and then remove entities (again for jBPM JPA serialization issues)
    if (deploymentMessage.getDeployment() != null) {
      deploymentRepository.save(deploymentMessage.getDeployment());
      deploymentMessage.setDeployment(null);
    }
    return exResults;
  }

  protected abstract ExecutionResults customExecute(CommandContext ctx,
      DeploymentMessage deploymentMessage);

}
