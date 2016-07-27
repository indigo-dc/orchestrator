package it.reply.orchestrator.command;

import static org.junit.Assert.assertEquals;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;

import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.service.CmdbService;
import it.reply.orchestrator.service.commands.BaseRankCloudProvidersCommand;
import it.reply.orchestrator.service.commands.GetCmdbDataDeploy;
import it.reply.orchestrator.workflow.RankCloudProvidersWorkflowTest;
import it.reply.utils.json.JsonUtility;

import org.junit.Test;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;

@DatabaseTearDown("/data/database-empty.xml")
public class GetCMDBDataCommandTest extends BaseRankCloudProviderCommandTest {

  @Autowired
  private GetCmdbDataDeploy getCMDBDataCommand;

  @Autowired
  private CmdbService cmdbService;

  @Override
  protected BaseRankCloudProvidersCommand getCommand() {
    return getCMDBDataCommand;
  }

  @Test
  @DatabaseSetup("/data/database-init.xml")
  public void doexecuteSuccesfully() throws Exception {

    RankCloudProvidersWorkflowTest.mockCmdb(mockServer, cmdbService.getUrl());

    ExecutionResults er = executeCommand(JsonUtility.deserializeJson(
        "{\"deploymentId\":\"mmd34483-d937-4578-bfdb-ebe196bf82dd\",\"slamPreferences\":{\"preferences\":[{\"customer\":\"indigo-dc\",\"preferences\":[{\"service_type\":\"compute\",\"priority\":[{\"sla_id\":\"4401ac5dc8cfbbb737b0a02575ee53f6\",\"service_id\":\"4401ac5dc8cfbbb737b0a02575e81d9b\",\"weight\":0.5},{\"sla_id\":\"4401ac5dc8cfbbb737b0a02575ee3b58\",\"service_id\":\"4401ac5dc8cfbbb737b0a02575e6f4bc\",\"weight\":0.5}]}],\"id\":\"4401ac5dc8cfbbb737b0a02575ee0e55\"}],\"sla\":[{\"customer\":\"indigo-dc\",\"provider\":\"provider-UPV-GRyCAP\",\"start_date\":\"11.01.2016+15:50:00\",\"end_date\":\"11.02.2016+15:50:00\",\"services\":[{\"type\":\"compute\",\"service_id\":\"4401ac5dc8cfbbb737b0a02575e81d9b\",\"targets\":[{\"type\":\"public_ip\",\"unit\":\"none\",\"restrictions\":{\"total_guaranteed\":10}}]}],\"id\":\"4401ac5dc8cfbbb737b0a02575ee3b58\"},{\"customer\":\"indigo-dc\",\"provider\":\"provider-RECAS-BARI\",\"start_date\":\"11.01.2016+15:50:00\",\"end_date\":\"11.02.2016+15:50:00\",\"services\":[{\"type\":\"compute\",\"service_id\":\"4401ac5dc8cfbbb737b0a02575e6f4bc\",\"targets\":[{\"type\":\"computing_time\",\"unit\":\"h\",\"restrictions\":{\"total_guaranteed\":200}}]}],\"id\":\"4401ac5dc8cfbbb737b0a02575ee53f6\"}]},\"cloudProviders\":{\"provider-RECAS-BARI\":{\"name\":null,\"id\":\"provider-RECAS-BARI\",\"cmdbProviderData\":null,\"cmdbProviderServices\":{\"4401ac5dc8cfbbb737b0a02575e6f4bc\":null},\"cmdbProviderImages\":[]},\"provider-UPV-GRyCAP\":{\"name\":null,\"id\":\"provider-UPV-GRyCAP\",\"cmdbProviderData\":null,\"cmdbProviderServices\":{\"4401ac5dc8cfbbb737b0a02575e81d9b\":null},\"cmdbProviderImages\":[]}},\"cloudProvidersMonitoringData\":{},\"rankedCloudProviders\":[]}",
        RankCloudProvidersMessage.class));

    assertEquals(true, commandSucceeded(er));
  }

}
