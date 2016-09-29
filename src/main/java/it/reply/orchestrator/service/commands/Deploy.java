package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderService;

import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class Deploy extends BaseDeployCommand {

  @Autowired
  @Qualifier("IM")
  private DeploymentProviderService imService;

  @Override
  protected ExecutionResults customExecute(CommandContext ctx,
      DeploymentMessage deploymentMessage) {
    return resultOccurred(imService.doDeploy(deploymentMessage));
  }

  @Override
  protected String getErrorMessagePrefix() {
    return "Error deploying through IM";
  }

}