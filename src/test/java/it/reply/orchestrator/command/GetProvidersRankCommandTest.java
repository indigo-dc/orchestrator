/*
 * Copyright © 2015-2017 Santer Reply S.p.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.reply.orchestrator.command;

import static org.junit.Assert.assertEquals;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;

import it.reply.orchestrator.config.properties.CprProperties;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.ranker.RankedCloudProvider;
import it.reply.orchestrator.service.CloudProviderRankerService;
import it.reply.orchestrator.service.CloudProviderRankerServiceIT;
import it.reply.orchestrator.service.CloudProviderRankerServiceTest;
import it.reply.orchestrator.service.commands.BaseRankCloudProvidersCommand;
import it.reply.orchestrator.service.commands.GetProvidersRank;
import it.reply.orchestrator.workflow.RankCloudProvidersWorkflowTest;
import it.reply.utils.json.JsonUtility;

import org.junit.Test;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@DatabaseTearDown("/data/database-empty.xml")
public class GetProvidersRankCommandTest extends BaseRankCloudProviderCommandTest {

  @Autowired
  private GetProvidersRank getProvidersRankCommand;
  
  @Autowired
  private CprProperties cprProperties;

  @Override
  protected BaseRankCloudProvidersCommand getCommand() {
    return getProvidersRankCommand;
  }

  @Test
  @DatabaseSetup("/data/database-init.xml")
  public void doexecuteSuccesfully() throws Exception {
    List<RankedCloudProvider> providers =
        CloudProviderRankerServiceTest.generateMockedRankedProviders();
    CloudProviderRankerServiceIT.mockCpr(mockServer, cprProperties.getUrl(), providers);

    ExecutionResults er = executeCommand(JsonUtility.deserializeJson(
        "{\"deploymentId\":\"mmd34483-d937-4578-bfdb-ebe196bf82dd\",\"slamPreferences\":{\"preferences\":[{\"customer\":\"indigo-dc\",\"preferences\":[{\"service_type\":\"compute\",\"priority\":[{\"sla_id\":\"4401ac5dc8cfbbb737b0a02575ee53f6\",\"service_id\":\"4401ac5dc8cfbbb737b0a02575e81d9b\",\"weight\":0.5},{\"sla_id\":\"4401ac5dc8cfbbb737b0a02575ee3b58\",\"service_id\":\"4401ac5dc8cfbbb737b0a02575e6f4bc\",\"weight\":0.5}]}],\"id\":\"4401ac5dc8cfbbb737b0a02575ee0e55\"}],\"sla\":[{\"customer\":\"indigo-dc\",\"provider\":\"provider-UPV-GRyCAP\",\"start_date\":\"11.01.2016+15:50:00\",\"end_date\":\"11.02.2016+15:50:00\",\"services\":[{\"type\":\"compute\",\"service_id\":\"4401ac5dc8cfbbb737b0a02575e6f4bc\",\"targets\":[{\"type\":\"public_ip\",\"unit\":\"none\",\"restrictions\":{\"total_guaranteed\":10}}]}],\"id\":\"4401ac5dc8cfbbb737b0a02575ee3b58\"},{\"customer\":\"indigo-dc\",\"provider\":\"provider-RECAS-BARI\",\"start_date\":\"11.01.2016+15:50:00\",\"end_date\":\"11.02.2016+15:50:00\",\"services\":[{\"type\":\"compute\",\"service_id\":\"4401ac5dc8cfbbb737b0a02575e6f4bc\",\"targets\":[{\"type\":\"computing_time\",\"unit\":\"h\",\"restrictions\":{\"total_guaranteed\":200}}]}],\"id\":\"4401ac5dc8cfbbb737b0a02575ee53f6\"}]},\"cloudProviders\":{\"provider-RECAS-BARI\":{\"id\":\"provider-RECAS-BARI\",\"cmdbProviderData\":{\"_id\":\"provider-RECAS-BARI\",\"_rev\":\"1-c7dbe4d8be30aa4c0f14d3ad0411d962\",\"data\":{\"id\":\"476\",\"primary_key\":\"83757G0\",\"name\":\"RECAS-BARI\",\"country\":\"Italy\",\"country_code\":\"IT\",\"roc\":\"NGI_IT\",\"subgrid\":\"\",\"giis_url\":\"ldap://cloud-bdii.recas.ba.infn.it:2170/GLUE2DomainID=RECAS-BARI,o=glue\"},\"type\":\"provider\"},\"cmdbProviderServices\":{\"4401ac5dc8cfbbb737b0a02575e81d9b\":{\"_id\":\"4401ac5dc8cfbbb737b0a02575e81d9b\",\"_rev\":\"2-be00f87438604f04d353233daabc562c\",\"data\":{\"service_type\":\"eu.egi.cloud.vm-management.occi\",\"endpoint\":\"http://onedock.i3m.upv.es:11443\",\"provider_id\":\"provider-UPV-GRyCAP\",\"type\":\"compute\"},\"type\":\"service\"}}},\"provider-UPV-GRyCAP\":{\"id\":\"provider-UPV-GRyCAP\",\"cmdbProviderData\":{\"_id\":\"provider-UPV-GRyCAP\",\"_rev\":\"1-0a5ba48b2d6e0c26d36b0e3e81175352\",\"data\":{\"id\":\"458\",\"primary_key\":\"135G0\",\"name\":\"UPV-GRyCAP\",\"country\":\"Spain\",\"country_code\":\"ES\",\"roc\":\"NGI_IBERGRID\",\"subgrid\":\"\",\"giis_url\":\"ldap://ngiesbdii.i3m.upv.es:2170/mds-vo-name=UPV-GRyCAP,o=grid\"},\"type\":\"provider\"},\"cmdbProviderServices\":{\"4401ac5dc8cfbbb737b0a02575e81d9b\":{\"_id\":\"4401ac5dc8cfbbb737b0a02575e6f4bc\",\"_rev\":\"1-256d36283315ea9bb045e6d5038657b6\",\"data\":{\"service_type\":\"eu.egi.cloud.vm-management.openstack\",\"endpoint\":\"http://cloud.recas.ba.infn.it:5000/v2.0\",\"provider_id\":\"provider-RECAS-BARI\",\"type\":\"compute\"},\"type\":\"service\"}}}},\"cloudProvidersMonitoringData\":{\"provider-RECAS-BARI\":[{\"metricName\":\"OCCI Create VM availability\",\"metricKey\":\"Cloud_Providers.provider-RECAS-BARI..OCCI Create VM availability\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI CreateVM Response Time\",\"metricKey\":\"Cloud_Providers.provider-RECAS-BARI..OCCI CreateVM Response Time\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"ms\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI CreateVM Result\",\"metricKey\":\"Cloud_Providers.provider-RECAS-BARI..OCCI CreateVM Result\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI Delete VM Availability\",\"metricKey\":\"Cloud_Providers.provider-RECAS-BARI..OCCI Delete VM Availability\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI DeleteVM Response Time\",\"metricKey\":\"Cloud_Providers.provider-RECAS-BARI..OCCI DeleteVM Response Time\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"ms\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI DeleteVM Result\",\"metricKey\":\"Cloud_Providers.provider-RECAS-BARI..OCCI DeleteVM Result\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"General OCCI API Availability\",\"metricKey\":\"Cloud_Providers.provider-RECAS-BARI..General OCCI API Availability\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"General OCCI API Response Time\",\"metricKey\":\"Cloud_Providers.provider-RECAS-BARI..General OCCI API Response Time\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"ms\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"General OCCI API Result\",\"metricKey\":\"Cloud_Providers.provider-RECAS-BARI..General OCCI API Result\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI Inspect VM availability\",\"metricKey\":\"Cloud_Providers.provider-RECAS-BARI..OCCI Inspect VM availability\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI InspectVM Response Time\",\"metricKey\":\"Cloud_Providers.provider-RECAS-BARI..OCCI InspectVM Response Time\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"ms\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI InspectVM Result\",\"metricKey\":\"Cloud_Providers.provider-RECAS-BARI..OCCI InspectVM Result\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]}],\"provider-UPV-GRyCAP\":[{\"metricName\":\"OCCI Create VM availability\",\"metricKey\":\"Cloud_Providers.provider-UPV-GRyCAP..OCCI Create VM availability\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI CreateVM Response Time\",\"metricKey\":\"Cloud_Providers.provider-UPV-GRyCAP..OCCI CreateVM Response Time\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"ms\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI CreateVM Result\",\"metricKey\":\"Cloud_Providers.provider-UPV-GRyCAP..OCCI CreateVM Result\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI Delete VM Availability\",\"metricKey\":\"Cloud_Providers.provider-UPV-GRyCAP..OCCI Delete VM Availability\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI DeleteVM Response Time\",\"metricKey\":\"Cloud_Providers.provider-UPV-GRyCAP..OCCI DeleteVM Response Time\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"ms\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI DeleteVM Result\",\"metricKey\":\"Cloud_Providers.provider-UPV-GRyCAP..OCCI DeleteVM Result\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"General OCCI API Availability\",\"metricKey\":\"Cloud_Providers.provider-UPV-GRyCAP..General OCCI API Availability\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"General OCCI API Response Time\",\"metricKey\":\"Cloud_Providers.provider-UPV-GRyCAP..General OCCI API Response Time\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"ms\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"General OCCI API Result\",\"metricKey\":\"Cloud_Providers.provider-UPV-GRyCAP..General OCCI API Result\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI Inspect VM availability\",\"metricKey\":\"Cloud_Providers.provider-UPV-GRyCAP..OCCI Inspect VM availability\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI InspectVM Response Time\",\"metricKey\":\"Cloud_Providers.provider-UPV-GRyCAP..OCCI InspectVM Response Time\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"ms\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]},{\"metricName\":\"OCCI InspectVM Result\",\"metricKey\":\"Cloud_Providers.provider-UPV-GRyCAP..OCCI InspectVM Result\",\"metricValue\":0.0,\"metricTime\":\"Instant null because no metrics were returned in the last 24hs\",\"metricUnit\":\"bit\",\"paasThresholds\":[],\"historyClocks\":[],\"historyValues\":[]}]},\"rankedCloudProviders\":[]}",
        RankCloudProvidersMessage.class));

    assertEquals(true, commandSucceeded(er));
  }

}
