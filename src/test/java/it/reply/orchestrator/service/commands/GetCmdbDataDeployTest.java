/*
 * Copyright © 2015-2019 Santer Reply S.p.A.
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

package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.config.properties.CmdbProperties;
import it.reply.orchestrator.service.CmdbServiceImpl;
import it.reply.orchestrator.util.IntegrationTestUtil;

import java.net.URI;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;

public class GetCmdbDataDeployTest extends BaseRankCloudProvidersCommandTest<GetCmdbDataDeploy> {

  @Spy
  @InjectMocks
  private CmdbServiceImpl cmdbService;

  @Spy
  private CmdbProperties cmdbProperties;

  public GetCmdbDataDeployTest() {
    super(new GetCmdbDataDeploy());
  }

  @Before
  public void setup() {
    cmdbProperties.setUrl(URI.create("https://www.example.com"));
  }

  @Test
  public void doexecuteSuccesfully() throws Exception {

    IntegrationTestUtil.mockCmdb(mockServer, cmdbProperties.getUrl());

    String serializedRankCloudProvidersMessage =
        "{\"deploymentId\":\"mmd34483-d937-4578-bfdb-ebe196bf82dd\",\"slamPreferences\":{\"preferences\":[{\"customer\":\"indigo-dc\",\"preferences\":[{\"service_type\":\"compute\",\"priority\":[{\"sla_id\":\"4401ac5dc8cfbbb737b0a02575ee53f6\",\"service_id\":\"4401ac5dc8cfbbb737b0a02575e81d9b\",\"weight\":0.5},{\"sla_id\":\"4401ac5dc8cfbbb737b0a02575ee3b58\",\"service_id\":\"4401ac5dc8cfbbb737b0a02575e6f4bc\",\"weight\":0.5}]}],\"id\":\"4401ac5dc8cfbbb737b0a02575ee0e55\"}],\"sla\":[{\"customer\":\"indigo-dc\",\"provider\":\"provider-UPV-GRyCAP\",\"start_date\":\"11.01.2016+15:50:00\",\"end_date\":\"11.02.2016+15:50:00\",\"services\":[{\"type\":\"compute\",\"service_id\":\"4401ac5dc8cfbbb737b0a02575e81d9b\",\"targets\":[{\"type\":\"public_ip\",\"unit\":\"none\",\"restrictions\":{\"total_guaranteed\":10}}]}],\"id\":\"4401ac5dc8cfbbb737b0a02575ee3b58\"},{\"customer\":\"indigo-dc\",\"provider\":\"provider-RECAS-BARI\",\"start_date\":\"11.01.2016+15:50:00\",\"end_date\":\"11.02.2016+15:50:00\",\"services\":[{\"type\":\"compute\",\"service_id\":\"4401ac5dc8cfbbb737b0a02575e6f4bc\",\"targets\":[{\"type\":\"computing_time\",\"unit\":\"h\",\"restrictions\":{\"total_guaranteed\":200}}]}],\"id\":\"4401ac5dc8cfbbb737b0a02575ee53f6\"}]},\"cloudProviders\":{\"provider-RECAS-BARI\":{\"id\":\"provider-RECAS-BARI\",\"cmdbProviderData\":null,\"cmdbProviderServices\":{\"4401ac5dc8cfbbb737b0a02575e6f4bc\":null},\"cmdbProviderImages\":{}},\"provider-UPV-GRyCAP\":{\"id\":\"provider-UPV-GRyCAP\",\"cmdbProviderData\":null,\"cmdbProviderServices\":{\"4401ac5dc8cfbbb737b0a02575e81d9b\":null},\"cmdbProviderImages\":{}}},\"cloudProvidersMonitoringData\":{},\"rankedCloudProviders\":[]}";

    Assertions
        .assertThatCode(() -> execute(serializedRankCloudProvidersMessage))
        .doesNotThrowAnyException();

  }

}
