package it.reply.orchestrator.service.commands.chronos;

import it.reply.orchestrator.service.DeploymentService;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderService;
import it.reply.workflowmanager.spring.orchestrator.bpm.ejbcommands.BaseCommand;

import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class UndeployOnChronos extends BaseCommand {

  @Autowired
  @Qualifier("CHRONOS")
  private DeploymentProviderService chronosService;

  @Override
  protected ExecutionResults customExecute(CommandContext ctx) throws Exception {
    String deploymentId =
        (String) getWorkItem(ctx).getParameter(DeploymentService.WF_PARAM_DEPLOYMENT_ID);
    boolean result = chronosService.doUndeploy(deploymentId);
    chronosService.finalizeUndeploy(deploymentId, result);
    return resultOccurred(result);
  }

}
