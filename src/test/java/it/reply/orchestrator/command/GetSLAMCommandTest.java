package it.reply.orchestrator.command;

import static org.junit.Assert.assertEquals;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;

import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.service.SlamService;
import it.reply.orchestrator.service.commands.BaseRankCloudProvidersCommand;
import it.reply.orchestrator.service.commands.GetSLAM;
import it.reply.orchestrator.workflow.RankCloudProvidersWorkflowTest;

import org.junit.Test;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;

@DatabaseTearDown("/data/database-empty.xml")
public class GetSLAMCommandTest extends BaseRankCloudProviderCommandTest {

  @Autowired
  private GetSLAM getSLAMCommand;

  @Autowired
  private SlamService slamService;

  @Override
  protected BaseRankCloudProvidersCommand getCommand() {
    return getSLAMCommand;
  }

  @Test
  @DatabaseSetup("/data/database-init.xml")
  public void doexecuteSuccesfully() throws Exception {

    RankCloudProvidersWorkflowTest.mockSlam(mockServer, slamService.getUrl());

    ExecutionResults er = executeCommand(new RankCloudProvidersMessage(getDeploymentId()));

    assertEquals(true, commandSucceeded(er));
  }

}
