package it.reply.orchestrator.service;

import it.reply.orchestrator.config.WebAppConfigurationAware;
import it.reply.orchestrator.service.commands.chronos.DeployOnChronos;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ChronosServiceTest extends WebAppConfigurationAware {

  @Autowired
  private DeployOnChronos service;

  private String deploymentId = "deployment_id";

  @Test
  public void customizeTemplateWithDeplymentIdSuccessfully() throws Exception {

    // service.chronosHelloWorld(deploymentId, false);
    // service.chronosHelloWorld(deploymentId, true);

    // assertEquals(deploymentId, templateDeploymentId);
  }

}
