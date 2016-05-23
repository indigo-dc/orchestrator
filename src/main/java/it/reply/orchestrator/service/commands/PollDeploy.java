package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderService;
import it.reply.orchestrator.service.deployment.providers.ImServiceImpl;
import it.reply.utils.misc.polling.AbstractPollingBehaviour;
import it.reply.utils.misc.polling.ExternallyControlledPoller;
import it.reply.utils.misc.polling.ExternallyControlledPoller.PollingStatus;
import it.reply.utils.misc.polling.PollingBehaviour;
import it.reply.utils.misc.polling.PollingException;
import it.reply.workflowmanager.spring.orchestrator.bpm.OrchestratorContextBean;
import it.reply.workflowmanager.spring.orchestrator.bpm.ejbcommands.BaseCommand;

import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class PollDeploy extends BaseCommand {

  public static final String WF_PARAM_POLLING_STATUS = "statusPoller";

  @Autowired
  @Qualifier("IM")
  private DeploymentProviderService imService;

  @Override
  protected ExecutionResults customExecute(CommandContext ctx) throws Exception {
    String deploymentId = getParameter(ctx, "DEPLOYMENT_ID");
    ExecutionResults exResults = new ExecutionResults();
    ExternallyControlledPoller<String, Status> statusPoller =
        getParameter(ctx, WF_PARAM_POLLING_STATUS);
    if (statusPoller == null) {
      statusPoller = getPoller();
    }
    exResults.setData(WF_PARAM_POLLING_STATUS, statusPoller);

    try {
      statusPoller.doPollEvent(deploymentId);
      if (statusPoller.getPollStatus() == PollingStatus.ENDED) {
        // Polling ended successfully -> Deployment completed -> Finalize (update template)
        imService.finalizeDeploy(deploymentId, true);
        return resultOccurred(true, exResults);
      } else {
        // Deployment is not ready yet
        return resultOccurred(false, exResults);
      }
    } catch (PollingException ex) {
      // Polling unsuccessful -> Deploy failed -> Finalize (update template)
      imService.finalizeDeploy(deploymentId, false);
      return resultOccurred(true, exResults);
    }
  }

  private static ExternallyControlledPoller<String, Status> getPoller() {

    long timeoutTime = 30 * 60 * 1000;

    PollingBehaviour<String, Status> pollBehavior =
        new AbstractPollingBehaviour<String, Status>(timeoutTime) {

          private static final long serialVersionUID = -5994059867039967783L;

          @Override
          public Status doPolling(String deploymentId) throws PollingException {
            try {
              ImServiceImpl imService = OrchestratorContextBean.getBean(ImServiceImpl.class);
              if (imService.isDeployed(deploymentId)) {
                return Status.CREATE_COMPLETE;
              } else {
                return Status.CREATE_IN_PROGRESS;
              }
            } catch (DeploymentException de) {
              return Status.CREATE_FAILED;
            } catch (Exception ex) {
              throw new PollingException("Polling for deploy - error occured: " + ex.getMessage(),
                  ex);
            }
          }

          @Override
          public boolean pollExit(Status pollResult) {
            return pollResult != null && pollResult != Status.CREATE_IN_PROGRESS;
          }

          @Override
          public boolean pollSuccessful(String params, Status pollResult) {
            return pollResult != null && pollResult == Status.CREATE_COMPLETE;
          }

        };

    return new ExternallyControlledPoller<String, Status>(pollBehavior, 3);
  }
}