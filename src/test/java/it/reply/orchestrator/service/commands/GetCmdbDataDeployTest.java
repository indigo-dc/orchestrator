/*
 * Copyright © 2015-2021 I.N.F.N.
 * Copyright © 2015-2020 Santer Reply S.p.A.
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
import it.reply.orchestrator.dal.entity.OidcEntityId;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.service.CmdbServiceV1Impl;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.orchestrator.util.IntegrationTestUtil;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.net.URI;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;


public class GetCmdbDataDeployTest extends BaseRankCloudProvidersCommandTest<GetCmdbDataDeploy> {

  private static final String oidcSubject = "55555555-6666-7777-8888-999999999990";
  private static final String oidcIssuer = "RECAS";
  private static final OidcTokenId oidcTokenId = new OidcTokenId();

  @Spy
  @InjectMocks
  private CmdbServiceV1Impl cmdbService;

  @Spy
  private CmdbProperties cmdbProperties;

  @Mock
  private OAuth2TokenService oauth2TokenService;

  public GetCmdbDataDeployTest() {
    super(new GetCmdbDataDeploy());
  }

  @Before
  public void setup() {
    cmdbProperties.setUrl(URI.create("https://www.example.com"));

    OidcEntityId oeid = new OidcEntityId();
    oeid.setSubject(oidcSubject);
    oeid.setIssuer(oidcIssuer);
    oidcTokenId.setOidcEntityId(oeid);

    when(oauth2TokenService
        .getOrganization(eq(oidcTokenId)))
        .thenReturn("8a5377c6-a7f4-4d1c-a4cd-074ab92b6035");
  }

  @Test
  public void doexecuteSuccesfully() throws Exception {

    IntegrationTestUtil.mockCmdb(mockServer, cmdbProperties.getUrl());

    String serializedRankCloudProvidersMessage =
        "{\n" +
        "  \"requestedWithToken\": {\n" +
        "    \"clientsId\": [],\n" +
        "    \"oidcEntityId\": {\n" +
        "      \"issuer\": \"RECAS\",\n" +
        "      \"subject\": \"55555555-6666-7777-8888-999999999990\"\n" +
        "    }\n" +
        "  },\n" +
        "  \"deploymentId\": \"mmd34483-d937-4578-bfdb-ebe196bf82dd\",\n" +
        "  \"slamPreferences\": {\n" +
        "    \"preferences\": [\n" +
        "      {\n" +
        "        \"customer\": \"indigo-dc\",\n" +
        "        \"preferences\": [\n" +
        "          {\n" +
        "            \"service_type\": \"compute\",\n" +
        "            \"priority\": [\n" +
        "              {\n" +
        "                \"sla_id\": \"4401ac5dc8cfbbb737b0a02575ee53f6\",\n" +
        "                \"service_id\": \"4401ac5dc8cfbbb737b0a02575e81d9b\",\n" +
        "                \"weight\": 0.5\n" +
        "              },\n" +
        "              {\n" +
        "                \"sla_id\": \"4401ac5dc8cfbbb737b0a02575ee3b58\",\n" +
        "                \"service_id\": \"4401ac5dc8cfbbb737b0a02575e6f4bc\",\n" +
        "                \"weight\": 0.5\n" +
        "              }\n" +
        "            ]\n" +
        "          }\n" +
        "        ],\n" +
        "        \"id\": \"4401ac5dc8cfbbb737b0a02575ee0e55\"\n" +
        "      }\n" +
        "    ],\n" +
        "    \"sla\": [\n" +
        "      {\n" +
        "        \"customer\": \"indigo-dc\",\n" +
        "        \"provider\": \"provider-UPV-GRyCAP\",\n" +
        "        \"start_date\": \"11.01.2016+15:50:00\",\n" +
        "        \"end_date\": \"11.02.2016+15:50:00\",\n" +
        "        \"services\": [\n" +
        "          {\n" +
        "            \"type\": \"compute\",\n" +
        "            \"service_id\": \"4401ac5dc8cfbbb737b0a02575e81d9b\",\n" +
        "            \"targets\": [\n" +
        "              {\n" +
        "                \"type\": \"public_ip\",\n" +
        "                \"unit\": \"none\",\n" +
        "                \"restrictions\": {\n" +
        "                  \"total_guaranteed\": 10\n" +
        "                }\n" +
        "              }\n" +
        "            ]\n" +
        "          }\n" +
        "        ],\n" +
        "        \"id\": \"4401ac5dc8cfbbb737b0a02575ee3b58\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"customer\": \"indigo-dc\",\n" +
        "        \"provider\": \"provider-RECAS-BARI\",\n" +
        "        \"start_date\": \"11.01.2016+15:50:00\",\n" +
        "        \"end_date\": \"11.02.2016+15:50:00\",\n" +
        "        \"services\": [\n" +
        "          {\n" +
        "            \"type\": \"compute\",\n" +
        "            \"service_id\": \"4401ac5dc8cfbbb737b0a02575e6f4bc\",\n" +
        "            \"targets\": [\n" +
        "              {\n" +
        "                \"type\": \"computing_time\",\n" +
        "                \"unit\": \"h\",\n" +
        "                \"restrictions\": {\n" +
        "                  \"total_guaranteed\": 200\n" +
        "                }\n" +
        "              }\n" +
        "            ]\n" +
        "          }\n" +
        "        ],\n" +
        "        \"id\": \"4401ac5dc8cfbbb737b0a02575ee53f6\"\n" +
        "      }\n" +
        "    ]\n" +
        "  },\n" +
        "  \"cloudProviders\": {\n" +
        "    \"provider-RECAS-BARI\": {\n" +
        "      \"id\": \"provider-RECAS-BARI\",\n" +
        "      \"cmdbProviderData\": null,\n" +
        "      \"cmdbProviderServices\": {\n" +
        "        \"4401ac5dc8cfbbb737b0a02575e6f4bc\": null\n" +
        "      },\n" +
        "      \"cmdbProviderImages\": {}\n" +
        "    },\n" +
        "    \"provider-UPV-GRyCAP\": {\n" +
        "      \"id\": \"provider-UPV-GRyCAP\",\n" +
        "      \"cmdbProviderData\": null,\n" +
        "      \"cmdbProviderServices\": {\n" +
        "        \"4401ac5dc8cfbbb737b0a02575e81d9b\": null\n" +
        "      },\n" +
        "      \"cmdbProviderImages\": {}\n" +
        "    }\n" +
        "  },\n" +
        "  \"cloudProvidersMonitoringData\": {},\n" +
        "  \"rankedCloudProviders\": []\n" +
        "}";

    Assertions
        .assertThatCode(() -> execute(serializedRankCloudProvidersMessage))
        .doesNotThrowAnyException();

  }

}
