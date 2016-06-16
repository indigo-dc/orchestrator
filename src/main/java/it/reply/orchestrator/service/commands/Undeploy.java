package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderService;

import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class Undeploy extends BaseDeployCommand {

  @Autowired
  @Qualifier("IM")
  private DeploymentProviderService imService;

  @Override
  protected ExecutionResults customExecute(CommandContext ctx,
      DeploymentMessage deploymentMessage) {
    boolean result = imService.doUndeploy(deploymentMessage);
    if (!result || deploymentMessage.isDeleteComplete()) {
      imService.finalizeUndeploy(deploymentMessage, result);
      deploymentMessage.setDeployment(null);
    }
    return resultOccurred(result);
  }

}
