package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.service.CallbackService;
import it.reply.workflowmanager.spring.orchestrator.bpm.ejbcommands.BaseCommand;

import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Notify extends BaseCommand {

  @Autowired
  private CallbackService callbackService;

  @Override
  protected ExecutionResults customExecute(CommandContext ctx) throws Exception {
    String deploymentId = (String) getWorkItem(ctx).getParameter("DEPLOYMENT_ID");
    boolean result = callbackService.doCallback(deploymentId);
    return resultOccurred(result);
  }

}
