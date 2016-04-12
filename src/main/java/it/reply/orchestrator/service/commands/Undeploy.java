package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.service.deployment.providers.DeploymentProviderService;
import it.reply.workflowmanager.spring.orchestrator.bpm.ejbcommands.BaseCommand;

import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Undeploy extends BaseCommand {

  @Autowired
  private DeploymentProviderService imService;

  @Override
  protected ExecutionResults customExecute(CommandContext ctx) throws Exception {
    String deploymentId = (String) getWorkItem(ctx).getParameter("DEPLOYMENT_ID");
    boolean result = imService.doUndeploy(deploymentId);
    return resultOccurred(result);
  }

}
