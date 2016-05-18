package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.service.deployment.providers.DeploymentProviderService;
import it.reply.workflowmanager.spring.orchestrator.bpm.ejbcommands.BaseCommand;

import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class Update extends BaseCommand {

  @Autowired
  @Qualifier("IM")
  private DeploymentProviderService imService;

  @Override
  protected ExecutionResults customExecute(CommandContext ctx) throws Exception {
    String deploymentId = (String) getWorkItem(ctx).getParameter("DEPLOYMENT_ID");
    String template = (String) getWorkItem(ctx).getParameter("TOSCA_TEMPLATE");
    boolean result = imService.doUpdate(deploymentId, template);
    return resultOccurred(result);
  }

}
