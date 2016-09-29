package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderService;

import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class Update extends BaseDeployCommand {

  @Autowired
  @Qualifier("IM")
  private DeploymentProviderService imService;

  @Override
  protected String getErrorMessagePrefix() {
    return "Error updating deployment through IM";
  }

  @Override
  protected ExecutionResults customExecute(CommandContext ctx,
      DeploymentMessage deploymentMessage) {
    String template = (String) getWorkItem(ctx).getParameter("TOSCA_TEMPLATE");
    boolean result = imService.doUpdate(deploymentMessage, template);
    return resultOccurred(result);
  }

}
