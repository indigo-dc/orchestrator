package it.reply.orchestrator.workflow;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import it.reply.orchestrator.config.WorkflowConfigProducerBean;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.ranker.RankedCloudProvider;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.service.CloudProviderRankerService;
import it.reply.orchestrator.service.CmdbService;
import it.reply.orchestrator.service.MonitoringService;
import it.reply.orchestrator.service.SlamService;
import it.reply.orchestrator.service.WorkflowConstants;
import it.reply.utils.json.JsonUtility;
import it.reply.workflowmanager.exceptions.WorkflowException;
import it.reply.workflowmanager.orchestrator.bpm.BusinessProcessManager;
import it.reply.workflowmanager.orchestrator.bpm.BusinessProcessManager.RUNTIME_STRATEGY;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jbpm.workflow.instance.WorkflowProcessInstance;
import org.kie.api.runtime.process.ProcessInstance;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RankCloudProvidersWorkflowTest {// extends WebAppConfigurationAware {

  static final Logger LOG = LogManager.getLogger(RankCloudProvidersWorkflowTest.class);

  // @Mock
  // private WorkItemHandlersProducer testWorkItemHandlersProducer;

  @Autowired
  private SlamService slamService;

  @Autowired
  private CmdbService cmdbService;

  @Autowired
  private MonitoringService monitoringService;

  @Autowired
  private CloudProviderRankerService cloudProviderRankerService;

  // @InjectMocks
  @Autowired
  private BusinessProcessManager wfService;

  private MockRestServiceServer mockServer;

  @Autowired
  private RestTemplate restTemplate;

  // @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mockServer = MockRestServiceServer.createServer(restTemplate);
  }

  /**
   * Test the RankCloudProviders WF (with success and mocked external services).
   * 
   * @throws Exception
   *           in case something went wrong...
   */
  // @Test //FIXME Re-enable when jBPM serialization problems are solved
  @Transactional
  public void testProcess() throws Exception {

    // Requests must be in the exact consumption order !
    mockSlam(mockServer);
    mockCmdb(mockServer);
    mockMonitoring(mockServer);
    mockCpr(mockServer);

    // Init params: empty RCPM
    Map<String, Object> params = new HashMap<>();
    RankCloudProvidersMessage rcpm = new RankCloudProvidersMessage();
    params.put(WorkflowConstants.WF_PARAM_RANK_CLOUD_PROVIDERS_MESSAGE, rcpm);

    ProcessInstance processInstance = null;
    try {
      processInstance =
          wfService.startProcess(WorkflowConfigProducerBean.RANK_CLOUD_PROVIDERS.getProcessId(),
              params, RUNTIME_STRATEGY.PER_PROCESS_INSTANCE);
    } catch (WorkflowException ex) {
      throw new OrchestratorException(ex);
    }
    while (processInstance.getState() != ProcessInstance.STATE_COMPLETED
        && processInstance.getState() != ProcessInstance.STATE_ABORTED) {
      try {
        Thread.sleep(1000);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }

    assertEquals(ProcessInstance.STATE_COMPLETED, processInstance.getState());

    WorkflowProcessInstance wfInstance = (WorkflowProcessInstance) processInstance;
    rcpm = (RankCloudProvidersMessage) wfInstance
        .getVariable(WorkflowConstants.WF_PARAM_RANK_CLOUD_PROVIDERS_MESSAGE);

    // Log JSON serialized for debug purpose
    LOG.debug(WorkflowConstants.WF_PARAM_RANK_CLOUD_PROVIDERS_MESSAGE + ": "
        + JsonUtility.serializeJson(rcpm));

    assertEquals(false, rcpm.getSlamPreferences().getPreferences().isEmpty());
    assertEquals(false, rcpm.getSlamPreferences().getSla().isEmpty());
    assertEquals(false, rcpm.getCloudProviders().isEmpty());
    assertEquals(false, rcpm.getCloudProvidersMonitoringData().isEmpty());
    assertEquals(false, rcpm.getRankedCloudProviders().isEmpty());

    // JbpmJUnitTestCaseHelper a = new JbpmJUnitTestCaseHelper();
    // a.assertProcessInstanceCompleted(pi.getId());
    //
    // assertNodeTriggered(pi.getId(), "Get SLAM");
  }

  protected void mockSlam(MockRestServiceServer mockServer) throws Exception {
    String response =
        "{\"preferences\":[{\"customer\":\"indigo-dc\",\"preferences\":[{\"service_type\":\"compute\",\"priority\":[{\"sla_id\":\"4401ac5dc8cfbbb737b0a02575ee53f6\",\"service_id\":\"4401ac5dc8cfbbb737b0a02575e8040f\",\"weight\":0.5},{\"sla_id\":\"4401ac5dc8cfbbb737b0a02575ee3b58\",\"service_id\":\"4401ac5dc8cfbbb737b0a02575e6f4bc\",\"weight\":0.5}]}],\"id\":\"4401ac5dc8cfbbb737b0a02575ee0e55\"}],\"sla\":[{\"customer\":\"indigo-dc\",\"provider\":\"provider-UPV-GRyCAP\",\"start_date\":\"11.01.2016+15:50:00\",\"end_date\":\"11.02.2016+15:50:00\",\"services\":[{\"type\":\"compute\",\"service_id\":\"4401ac5dc8cfbbb737b0a02575e6f4bc\",\"targets\":[{\"type\":\"public_ip\",\"unit\":\"none\",\"restrictions\":{\"total_limit\":100,\"total_guaranteed\":10}}]}],\"id\":\"4401ac5dc8cfbbb737b0a02575ee3b58\"},{\"customer\":\"indigo-dc\",\"provider\":\"provider-RECAS-BARI\",\"start_date\":\"11.01.2016+15:50:00\",\"end_date\":\"11.02.2016+15:50:00\",\"services\":[{\"type\":\"compute\",\"service_id\":\"4401ac5dc8cfbbb737b0a02575e8040f\",\"targets\":[{\"type\":\"computing_time\",\"unit\":\"h\",\"restrictions\":{\"total_guaranteed\":200}}]}],\"id\":\"4401ac5dc8cfbbb737b0a02575ee53f6\"}]}";

    mockServer.expect(requestTo(slamService.getUrl() + "preferences/indigo-dc"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
  }

  protected void mockCmdb(MockRestServiceServer mockServer) throws Exception {
    // Provider: provider-RECAS-BARI
    mockServer.expect(requestTo(cmdbService.getUrl() + "provider/id/provider-RECAS-BARI"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(
            "{\"_id\":\"provider-RECAS-BARI\",\"_rev\":\"1-c7dbe4d8be30aa4c0f14d3ad0411d962\",\"data\":{\"id\":\"476\",\"primary_key\":\"83757G0\",\"name\":\"RECAS-BARI\",\"country\":\"Italy\",\"country_code\":\"IT\",\"roc\":\"NGI_IT\",\"subgrid\":\"\",\"giis_url\":\"ldap://cloud-bdii.recas.ba.infn.it:2170/GLUE2DomainID=RECAS-BARI,o=glue\"},\"type\":\"provider\"}",
            MediaType.APPLICATION_JSON));

    // Service: Compute on provider-RECAS-BARI
    mockServer
        .expect(requestTo(cmdbService.getUrl() + "service/id/4401ac5dc8cfbbb737b0a02575e8040f"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(
            "{\"_id\":\"4401ac5dc8cfbbb737b0a02575e8040f\",\"_rev\":\"2-be00f87438604f04d353233daabc562c\",\"data\":{\"service_type\":\"eu.egi.cloud.vm-management.occi\",\"endpoint\":\"http://onedock.i3m.upv.es:11443\",\"provider_id\":\"provider-UPV-GRyCAP\",\"type\":\"compute\"},\"type\":\"service\"}",
            MediaType.APPLICATION_JSON));

    // Provider: provider-UPV-GRyCAP
    mockServer.expect(requestTo(cmdbService.getUrl() + "provider/id/provider-UPV-GRyCAP"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(
            "{\"_id\":\"provider-UPV-GRyCAP\",\"_rev\":\"1-0a5ba48b2d6e0c26d36b0e3e81175352\",\"data\":{\"id\":\"458\",\"primary_key\":\"135G0\",\"name\":\"UPV-GRyCAP\",\"country\":\"Spain\",\"country_code\":\"ES\",\"roc\":\"NGI_IBERGRID\",\"subgrid\":\"\",\"giis_url\":\"ldap://ngiesbdii.i3m.upv.es:2170/mds-vo-name=UPV-GRyCAP,o=grid\"},\"type\":\"provider\"}",
            MediaType.APPLICATION_JSON));

    // Service: Compute on provider-UPV-GRyCAP
    mockServer
        .expect(requestTo(cmdbService.getUrl() + "service/id/4401ac5dc8cfbbb737b0a02575e6f4bc"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(
            "{\"_id\":\"4401ac5dc8cfbbb737b0a02575e6f4bc\",\"_rev\":\"1-256d36283315ea9bb045e6d5038657b6\",\"data\":{\"service_type\":\"eu.egi.cloud.vm-management.openstack\",\"endpoint\":\"http://cloud.recas.ba.infn.it:5000/v2.0\",\"provider_id\":\"provider-RECAS-BARI\",\"type\":\"compute\"},\"type\":\"service\"}",
            MediaType.APPLICATION_JSON));

  }

  protected void mockMonitoring(MockRestServiceServer mockServer) throws Exception {

    // provider-RECAS-BARI
    mockServer.expect(requestTo(monitoringService.getUrl() + "provider-RECAS-BARI"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(
            "{\"meta\":{\"status\":200,\"additionalProperties\":{}},\"result\":{\"groups\":[{\"groupName\":\"Cloud_Providers\",\"paasMachines\":[{\"machineName\":\"provider-RECAS-BARI\",\"ip\":\"127.0.0.1\",\"serviceCategory\":\"\",\"serviceId\":\"ID\",\"services\":[{\"serviceName\":\"\",\"paasMetrics\":[{\"metricName\":\"OCCI Create VM availability\",\"metricKey\":\"Cloud_Providers.provider-RECAS-BARI..OCCI Create VM availability\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI CreateVM Response Time\",\"metricKey\":\"Cloud_Providers.provider-RECAS-BARI..OCCI CreateVM Response Time\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"ms\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI CreateVM Result\",\"metricKey\":\"Cloud_Providers.provider-RECAS-BARI..OCCI CreateVM Result\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI Delete VM Availability\",\"metricKey\":\"Cloud_Providers.provider-RECAS-BARI..OCCI Delete VM Availability\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI DeleteVM Response Time\",\"metricKey\":\"Cloud_Providers.provider-RECAS-BARI..OCCI DeleteVM Response Time\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"ms\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI DeleteVM Result\",\"metricKey\":\"Cloud_Providers.provider-RECAS-BARI..OCCI DeleteVM Result\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"General OCCI API Availability\",\"metricKey\":\"Cloud_Providers.provider-RECAS-BARI..General OCCI API Availability\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"General OCCI API Response Time\",\"metricKey\":\"Cloud_Providers.provider-RECAS-BARI..General OCCI API Response Time\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"ms\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"General OCCI API Result\",\"metricKey\":\"Cloud_Providers.provider-RECAS-BARI..General OCCI API Result\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI Inspect VM availability\",\"metricKey\":\"Cloud_Providers.provider-RECAS-BARI..OCCI Inspect VM availability\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI InspectVM Response Time\",\"metricKey\":\"Cloud_Providers.provider-RECAS-BARI..OCCI InspectVM Response Time\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"ms\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI InspectVM Result\",\"metricKey\":\"Cloud_Providers.provider-RECAS-BARI..OCCI InspectVM Result\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]}]}]}]}]},\"additionalProperties\":{}}",
            MediaType.APPLICATION_JSON));

    // provider-UPV-GRyCAP
    mockServer.expect(requestTo(monitoringService.getUrl() + "provider-UPV-GRyCAP"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(
            "{\"meta\":{\"status\":200,\"additionalProperties\":{}},\"result\":{\"groups\":[{\"groupName\":\"Cloud_Providers\",\"paasMachines\":[{\"machineName\":\"provider-UPV-GRyCAP\",\"ip\":\"127.0.0.1\",\"serviceCategory\":\"\",\"serviceId\":\"id\",\"services\":[{\"serviceName\":\"\",\"paasMetrics\":[{\"metricName\":\"OCCI Create VM availability\",\"metricKey\":\"Cloud_Providers.provider-UPV-GRyCAP..OCCI Create VM availability\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI CreateVM Response Time\",\"metricKey\":\"Cloud_Providers.provider-UPV-GRyCAP..OCCI CreateVM Response Time\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"ms\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI CreateVM Result\",\"metricKey\":\"Cloud_Providers.provider-UPV-GRyCAP..OCCI CreateVM Result\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI Delete VM Availability\",\"metricKey\":\"Cloud_Providers.provider-UPV-GRyCAP..OCCI Delete VM Availability\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI DeleteVM Response Time\",\"metricKey\":\"Cloud_Providers.provider-UPV-GRyCAP..OCCI DeleteVM Response Time\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"ms\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI DeleteVM Result\",\"metricKey\":\"Cloud_Providers.provider-UPV-GRyCAP..OCCI DeleteVM Result\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"General OCCI API Availability\",\"metricKey\":\"Cloud_Providers.provider-UPV-GRyCAP..General OCCI API Availability\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"General OCCI API Response Time\",\"metricKey\":\"Cloud_Providers.provider-UPV-GRyCAP..General OCCI API Response Time\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"ms\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"General OCCI API Result\",\"metricKey\":\"Cloud_Providers.provider-UPV-GRyCAP..General OCCI API Result\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI Inspect VM availability\",\"metricKey\":\"Cloud_Providers.provider-UPV-GRyCAP..OCCI Inspect VM availability\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI InspectVM Response Time\",\"metricKey\":\"Cloud_Providers.provider-UPV-GRyCAP..OCCI InspectVM Response Time\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"ms\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI InspectVM Result\",\"metricKey\":\"Cloud_Providers.provider-UPV-GRyCAP..OCCI InspectVM Result\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]}]}]}]}]},\"additionalProperties\":{}}",
            MediaType.APPLICATION_JSON));
  }

  protected void mockCpr(MockRestServiceServer mockServer) throws Exception {
    List<RankedCloudProvider> response =
        Arrays.asList(new RankedCloudProvider("Name1", 1, true, ""),
            new RankedCloudProvider("Name2", 0, false, "Error msg"));

    mockServer.expect(requestTo(cloudProviderRankerService.getUrl()))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(JsonUtility.serializeJson(response), MediaType.APPLICATION_JSON));
  }

}
