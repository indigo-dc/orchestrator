package it.reply.orchestrator.service;

import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.stereotype.Component;
import it.reply.workflowManager.spring.orchestrator.bpm.ejbcommands.BaseCommand;

@Component
public class TestCommand extends BaseCommand {

  @Override
  protected ExecutionResults customExecute(CommandContext ctx) throws Exception {
    logger.error("TEST");
    return new ExecutionResults();
  }

}
