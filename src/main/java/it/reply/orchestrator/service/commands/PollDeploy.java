package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.service.deployment.providers.DeploymentProviderService;
import it.reply.orchestrator.service.deployment.providers.ImServiceImpl;
import it.reply.utils.misc.polling.AbstractPollingBehaviour;
import it.reply.utils.misc.polling.ExternallyControlledPoller;
import it.reply.utils.misc.polling.PollingBehaviour;
import it.reply.utils.misc.polling.PollingException;
import it.reply.workflowmanager.spring.orchestrator.bpm.OrchestratorContextBean;
import it.reply.workflowmanager.spring.orchestrator.bpm.ejbcommands.BaseCommand;

import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PollDeploy extends BaseCommand {

  @Autowired
  private DeploymentProviderService imService;

  @Override
  protected ExecutionResults customExecute(CommandContext ctx) throws Exception {
    String deploymentId = getParameter(ctx, "DEPLOYMENT_ID");
    ExecutionResults exResults = new ExecutionResults();
    ExternallyControlledPoller<String, Boolean> pollingStatus = getParameter(ctx, "pollingStatus");
    if (pollingStatus == null) {
      pollingStatus = getPoller();
      exResults.setData("pollingStatus", pollingStatus);
    }
    try {
      Boolean result = pollingStatus.doPollEvent(deploymentId);
      if (result != null && result) {
        imService.finalizeDeploy(deploymentId, result);
        return resultOccurred(true, exResults);
      } else {
        return resultOccurred(false, exResults);
      }
    } catch (PollingException ex) {
      imService.finalizeDeploy(deploymentId, false);
      return resultOccurred(true, exResults);
    }
  }

  private static ExternallyControlledPoller<String, Boolean> getPoller() {

    long timeoutTime = 30 * 60 * 1000;

    PollingBehaviour<String, Boolean> pollBehavior = new AbstractPollingBehaviour<String, Boolean>(
        timeoutTime) {

      private static final long serialVersionUID = -5994059867039967783L;

      @Override
      public Boolean doPolling(String deploymentId) throws PollingException {
        try {
          ImServiceImpl imService = OrchestratorContextBean.getBean(ImServiceImpl.class);
          return imService.isDeployed(deploymentId);
        } catch (Exception ex) {
          throw new PollingException("Polling for deploy - error occured: " + ex.getMessage(), ex);
        }
      }

      @Override
      public boolean pollExit(Boolean pollResult) {
        return pollResult != null && pollResult;
      }

    };

    return new ExternallyControlledPoller<String, Boolean>(pollBehavior, 3);
  }
}
