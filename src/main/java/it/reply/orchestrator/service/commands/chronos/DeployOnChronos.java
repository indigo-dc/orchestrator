package it.reply.orchestrator.service.commands.chronos;

import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.service.commands.BaseDeployCommand;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderService;

import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class DeployOnChronos extends BaseDeployCommand {

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
    return resultOccurred(chronosService.doDeploy(deploymentMessage));
  }

}
