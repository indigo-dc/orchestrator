package it.reply.orchestrator.service.commands.chronos;

import it.reply.orchestrator.controller.DeploymentController;
import it.reply.orchestrator.service.DeploymentService;
import it.reply.orchestrator.service.deployment.providers.ChronosServiceImpl;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderService;
import it.reply.workflowmanager.spring.orchestrator.bpm.ejbcommands.BaseCommand;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class CleanChronos extends BaseCommand {

  private static final Logger LOG = LogManager.getLogger(DeploymentController.class);

  @Autowired
  @Qualifier("CHRONOS")
  private DeploymentProviderService chronosService;

  @Autowired
  private ChronosServiceImpl chronosServiceImpl;

  @Override
  protected ExecutionResults customExecute(CommandContext ctx) throws Exception {
    String deploymentId =
        (String) getWorkItem(ctx).getParameter(DeploymentService.WF_PARAM_DEPLOYMENT_ID);
    // boolean result = chronosServiceImpl.cleanChronos(deploymentId);
    // FIXME Remove if actualiy not used !
    LOG.warn("NO OP HERE !");
    return resultOccurred(true);
  }

}
