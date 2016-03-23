package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.service.deployment.providers.DeploymentProviderService;
import it.reply.workflowManager.spring.orchestrator.bpm.ejbcommands.BaseCommand;

import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Update extends BaseCommand {

  @Autowired
  private DeploymentProviderService imService;

  @Override
  protected ExecutionResults customExecute(CommandContext ctx) throws Exception {
    String deploymentId = (String) getWorkItem(ctx).getParameter("DEPLOYMENT_ID");
    String template = (String) getWorkItem(ctx).getParameter("TOSCA_TEMPLATE");
    imService.doUpdate(deploymentId, template);
    return new ExecutionResults();
  }

}
