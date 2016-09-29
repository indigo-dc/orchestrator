package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderService;
import it.reply.orchestrator.service.deployment.providers.ImServiceImpl;
import it.reply.utils.misc.polling.AbstractPollingBehaviour;
import it.reply.utils.misc.polling.ExternallyControlledPoller;
import it.reply.utils.misc.polling.PollingBehaviour;
import it.reply.utils.misc.polling.PollingException;
import it.reply.workflowmanager.spring.orchestrator.bpm.OrchestratorContextBean;

import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class PollUndeploy extends BaseDeployCommand {

  @Autowired
  @Qualifier("IM")
  private DeploymentProviderService imService;

  @Override
  protected String getErrorMessagePrefix() {
    return "Error undeploying through IM";
  }

  @Override
  protected ExecutionResults customExecute(CommandContext ctx,
      DeploymentMessage deploymentMessage) {

    ExecutionResults exResults = new ExecutionResults();
    ExternallyControlledPoller<DeploymentMessage, Boolean> pollingStatus =
        getParameter(ctx, "pollingStatus");
    if (pollingStatus == null) {
      pollingStatus = getPoller();
    }
    exResults.setData("pollingStatus", pollingStatus);
    try {
      Boolean result = pollingStatus.doPollEvent(deploymentMessage);
      if (result != null && result) {
        imService.finalizeUndeploy(deploymentMessage, result);
        deploymentMessage.setDeployment(null);
        return resultOccurred(true, exResults);
      } else {
        return resultOccurred(false, exResults);
      }
    } catch (PollingException ex) {
      imService.finalizeUndeploy(deploymentMessage, false);
      return resultOccurred(true, exResults);
    }
  }

  private static ExternallyControlledPoller<DeploymentMessage, Boolean> getPoller() {

    long timeoutTime = 30 * 60 * 1000;

    PollingBehaviour<DeploymentMessage, Boolean> pollBehavior =
        new AbstractPollingBehaviour<DeploymentMessage, Boolean>(timeoutTime) {

          private static final long serialVersionUID = -5994059867039967783L;

          @Override
          public Boolean doPolling(DeploymentMessage deploymentId) throws PollingException {
            try {
              ImServiceImpl imService = OrchestratorContextBean.getBean(ImServiceImpl.class);
              return imService.isUndeployed(deploymentId);
            } catch (Exception ex) {
              throw new PollingException("Polling for undeploy - error occured: " + ex.getMessage(),
                  ex);
            }
          }

          @Override
          public boolean pollExit(Boolean pollResult) {
            return pollResult != null && pollResult;
          }

        };

    return new ExternallyControlledPoller<DeploymentMessage, Boolean>(pollBehavior, 3);
  }
}
