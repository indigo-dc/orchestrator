package it.reply.orchestrator.command;

import it.reply.orchestrator.config.WebAppConfigurationAware;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.service.WorkflowConstants;
import it.reply.orchestrator.service.commands.BaseRankCloudProvidersCommand;
import it.reply.utils.json.JsonUtility;
import it.reply.workflowmanager.utils.Constants;

import org.junit.Before;
import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

public abstract class BaseRankCloudProviderCommandTest extends WebAppConfigurationAware {

  protected MockRestServiceServer mockServer;

  @Autowired
  protected RestTemplate restTemplate;

  @Before
  public void setUp() {
    mockServer = MockRestServiceServer.createServer(restTemplate);
  }

  protected String getDeploymentId() {
    return "mmd34483-d937-4578-bfdb-ebe196bf82dd";
  }

  protected CommandContext
      buildCommandContext(RankCloudProvidersMessage rankCloudProvidersMessage) {
    String deploymentId = getDeploymentId();

    return TestCommandHelper.buildCommandContext()
        .withParam(WorkflowConstants.WF_PARAM_DEPLOYMENT_ID, deploymentId)
        .withParam(WorkflowConstants.WF_PARAM_RANK_CLOUD_PROVIDERS_MESSAGE,
            rankCloudProvidersMessage)
        .get();
  }

  protected ExecutionResults executeCommand(RankCloudProvidersMessage rankCloudProvidersMessage)
      throws Exception {
    ExecutionResults er = getCommand().execute(buildCommandContext(rankCloudProvidersMessage));
    serializeRankCloudProvidersMessage(er);
    mockServer.verify();
    return er;
  }

  protected boolean commandSucceeded(ExecutionResults er) {
    return (boolean) er.getData(Constants.RESULT);
  }

  protected void serializeRankCloudProvidersMessage(ExecutionResults er) throws Exception {
    System.out.println(JsonUtility
        .serializeJson(er.getData(WorkflowConstants.WF_PARAM_RANK_CLOUD_PROVIDERS_MESSAGE)));
  }

  protected abstract BaseRankCloudProvidersCommand getCommand();
}
