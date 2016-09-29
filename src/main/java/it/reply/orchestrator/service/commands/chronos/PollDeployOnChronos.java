package it.reply.orchestrator.service.commands.chronos;

import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.commands.BaseDeployCommand;
import it.reply.orchestrator.service.deployment.providers.ChronosServiceImpl;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderService;
import it.reply.utils.misc.polling.AbstractPollingBehaviour;
import it.reply.utils.misc.polling.ExternallyControlledPoller;
import it.reply.utils.misc.polling.ExternallyControlledPoller.PollingStatus;
import it.reply.utils.misc.polling.PollingBehaviour;
import it.reply.utils.misc.polling.PollingException;
import it.reply.workflowmanager.spring.orchestrator.bpm.OrchestratorContextBean;

import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class PollDeployOnChronos extends BaseDeployCommand {

  public static final String WF_PARAM_POLLING_STATUS = "statusPoller";

  @Autowired
  @Qualifier("CHRONOS")
  private DeploymentProviderService chronosService;

  @Override
  protected String getErrorMessagePrefix() {
    return "Error deploying on chronos";
  }

  @Override
  protected ExecutionResults customExecute(CommandContext ctx,
      DeploymentMessage deploymentMessage) {
    ExecutionResults exResults = new ExecutionResults();

    ExternallyControlledPoller<DeploymentMessage, Status> statusPoller =
        getParameter(ctx, WF_PARAM_POLLING_STATUS);
    if (statusPoller == null) {
      statusPoller = getPoller();
    }
    exResults.setData(WF_PARAM_POLLING_STATUS, statusPoller);

    try {
      statusPoller.doPollEvent(deploymentMessage);
      if (statusPoller.getPollStatus() == PollingStatus.ENDED) {
        // Polling ended successfully -> Deployment completed -> Finalize (update template)
        chronosService.finalizeDeploy(deploymentMessage, true);
        return resultOccurred(true, exResults);
      } else {
        // Deployment is not ready yet
        return resultOccurred(false, exResults);
      }
    } catch (PollingException ex) {
      // Polling unsuccessful -> Deploy failed -> Finalize (update template)
      chronosService.finalizeDeploy(deploymentMessage, false);
      return resultOccurred(true, exResults);
    }
  }

  private static ExternallyControlledPoller<DeploymentMessage, Status> getPoller() {

    long timeoutTime = 30 * 60 * 1000;

    PollingBehaviour<DeploymentMessage, Status> pollBehavior =
        new AbstractPollingBehaviour<DeploymentMessage, Status>(timeoutTime) {

          private static final long serialVersionUID = -5994059867039967783L;

          @Override
          public Status doPolling(DeploymentMessage deploymentMessage) throws PollingException {
            try {
              ChronosServiceImpl chronosService =
                  OrchestratorContextBean.getBean(ChronosServiceImpl.class);
              if (chronosService.isDeployed(deploymentMessage)) {
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
          public boolean pollSuccessful(DeploymentMessage deploymentMessage, Status pollResult) {
            return pollResult != null && pollResult == Status.CREATE_COMPLETE;
          }

        };

    return new ExternallyControlledPoller<DeploymentMessage, Status>(pollBehavior, 3);
  }
}
