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

package it.reply.orchestrator.util;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

public class IntegrationTestUtil {

  static final Logger LOG = LoggerFactory.getLogger(IntegrationTestUtil.class);

  public static void mockSlam(MockRestServiceServer mockServer, URI baseUrl) throws Exception {
    String response =
        "{\n" +
        "  \"preferences\": [\n" +
        "    {\n" +
        "      \"customer\": \"indigo-dc\",\n" +
        "      \"preferences\": [\n" +
        "        {\n" +
        "          \"service_type\": \"compute\",\n" +
        "          \"priority\": [\n" +
        "            {\n" +
        "              \"sla_id\": \"4401ac5dc8cfbbb737b0a02575ee53f6\",\n" +
        "              \"service_id\": \"4401ac5dc8cfbbb737b0a02575e81d9b\",\n" +
        "              \"weight\": 0.5\n" +
        "            },\n" +
        "            {\n" +
        "              \"sla_id\": \"4401ac5dc8cfbbb737b0a02575ee3b58\",\n" +
        "              \"service_id\": \"4401ac5dc8cfbbb737b0a02575e6f4bc\",\n" +
        "              \"weight\": 0.5\n" +
        "            }\n" +
        "          ]\n" +
        "        }\n" +
        "      ],\n" +
        "      \"id\": \"4401ac5dc8cfbbb737b0a02575ee0e55\"\n" +
        "    }\n" +
        "  ],\n" +
        "  \"sla\": [\n" +
        "    {\n" +
        "      \"customer\": \"indigo-dc\",\n" +
        "      \"provider\": \"provider-UPV-GRyCAP\",\n" +
        "      \"start_date\": \"11.01.2016+15:50:00\",\n" +
        "      \"end_date\": \"11.02.2016+15:50:00\",\n" +
        "      \"services\": [\n" +
        "        {\n" +
        "          \"type\": \"compute\",\n" +
        "          \"service_id\": \"4401ac5dc8cfbbb737b0a02575e81d9b\",\n" +
        "          \"targets\": [\n" +
        "            {\n" +
        "              \"type\": \"public_ip\",\n" +
        "              \"unit\": \"none\",\n" +
        "              \"restrictions\": {\n" +
        "                \"total_limit\": 100,\n" +
        "                \"total_guaranteed\": 10\n" +
        "              }\n" +
        "            }\n" +
        "          ]\n" +
        "        }\n" +
        "      ],\n" +
        "      \"id\": \"4401ac5dc8cfbbb737b0a02575ee3b58\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"customer\": \"indigo-dc\",\n" +
        "      \"provider\": \"provider-RECAS-BARI\",\n" +
        "      \"start_date\": \"11.01.2016+15:50:00\",\n" +
        "      \"end_date\": \"11.02.2016+15:50:00\",\n" +
        "      \"services\": [\n" +
        "        {\n" +
        "          \"type\": \"compute\",\n" +
        "          \"service_id\": \"4401ac5dc8cfbbb737b0a02575e6f4bc\",\n" +
        "          \"targets\": [\n" +
        "            {\n" +
        "              \"type\": \"computing_time\",\n" +
        "              \"unit\": \"h\",\n" +
        "              \"restrictions\": {\n" +
        "                \"total_guaranteed\": 200\n" +
        "              }\n" +
        "            }\n" +
        "          ]\n" +
        "        }\n" +
        "      ],\n" +
        "      \"id\": \"4401ac5dc8cfbbb737b0a02575ee53f6\"\n" +
        "    }\n" +
        "  ]\n" +
        "}";

    mockServer.expect(requestTo(baseUrl + "/preferences/indigo-dc"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
  }

  public static void mockCmdb(MockRestServiceServer mockServer, URI baseUrl) throws Exception {
    // Provider: provider-RECAS-BARI
    mockServer.expect(requestTo(baseUrl + "/provider/id/provider-RECAS-BARI?include_docs=true"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(
            "{\n" +
            "  \"_id\": \"provider-RECAS-BARI\",\n" +
            "  \"_rev\": \"1-c7dbe4d8be30aa4c0f14d3ad0411d962\",\n" +
            "  \"data\": {\n" +
            "    \"id\": \"476\",\n" +
            "    \"primary_key\": \"83757G0\",\n" +
            "    \"name\": \"RECAS-BARI\",\n" +
            "    \"country\": \"Italy\",\n" +
            "    \"country_code\": \"IT\",\n" +
            "    \"roc\": \"NGI_IT\",\n" +
            "    \"subgrid\": \"\",\n" +
            "    \"giis_url\": \"ldap://cloud-bdii.recas.ba.infn.it:2170/GLUE2DomainID=RECAS-BARI,o=glue\"\n" +
            "  },\n" +
            "  \"type\": \"provider\"\n" +
            "}",
            MediaType.APPLICATION_JSON));

    // Service: Compute on provider-RECAS-BARI
    mockServer
        .expect(requestTo(
            baseUrl + "/provider/id/provider-RECAS-BARI/has_many/services?include_docs=true"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(
            "{\n" +
            "  \"total_rows\": 60,\n" +
            "  \"offset\": 49,\n" +
            "  \"rows\": [\n" +
            "    {\n" +
            "      \"id\": \"4401ac5dc8cfbbb737b0a025758cfd60\",\n" +
            "      \"key\": [\n" +
            "        \"provider-RECAS-BARI\",\n" +
            "        \"services\"\n" +
            "      ],\n" +
            "      \"value\": {\n" +
            "        \"sitename\": \"RECAS-BARI\",\n" +
            "        \"provider_id\": \"provider-RECAS-BARI\",\n" +
            "        \"hostname\": \"cloud.recas.ba.infn.it\",\n" +
            "        \"type\": \"compute\"\n" +
            "      },\n" +
            "      \"doc\": {\n" +
            "        \"_id\": \"4401ac5dc8cfbbb737b0a025758cfd60\",\n" +
            "        \"_rev\": \"2-6540bc334d76090c53399c7bd5bc0aae\",\n" +
            "        \"data\": {\n" +
            "          \"primary_key\": \"8015G0\",\n" +
            "          \"hostname\": \"cloud.recas.ba.infn.it\",\n" +
            "          \"gocdb_portal_url\": \"https://goc.egi.eu/portal/index.php?Page_Type=Service&id=8015\",\n" +
            "          \"hostdn\": \"/C=IT/O=INFN/OU=Host/L=Bari/CN=cloud.recas.ba.infn.it\",\n" +
            "          \"beta\": \"N\",\n" +
            "          \"service_type\": \"eu.egi.cloud.vm-management.occi\",\n" +
            "          \"core\": null,\n" +
            "          \"in_production\": \"Y\",\n" +
            "          \"node_monitored\": \"Y\",\n" +
            "          \"sitename\": \"RECAS-BARI\",\n" +
            "          \"country_name\": \"Italy\",\n" +
            "          \"country_code\": \"IT\",\n" +
            "          \"roc_name\": \"NGI_IT\",\n" +
            "          \"scopes\": {\n" +
            "            \"scope\": [\n" +
            "              \"EGI\",\n" +
            "              \"wlcg\",\n" +
            "              \"lhcb\"\n" +
            "            ]\n" +
            "          },\n" +
            "          \"extensions\": null,\n" +
            "          \"type\": \"compute\",\n" +
            "          \"provider_id\": \"provider-RECAS-BARI\",\n" +
            "          \"endpoint\": \"http://cloud.recas.ba.infn.it:8787/occi\"\n" +
            "        },\n" +
            "        \"type\": \"service\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"4401ac5dc8cfbbb737b0a02575e6f4bc\",\n" +
            "      \"key\": [\n" +
            "        \"provider-RECAS-BARI\",\n" +
            "        \"services\"\n" +
            "      ],\n" +
            "      \"value\": {\n" +
            "        \"provider_id\": \"provider-RECAS-BARI\",\n" +
            "        \"type\": \"compute\"\n" +
            "      },\n" +
            "      \"doc\": {\n" +
            "        \"_id\": \"4401ac5dc8cfbbb737b0a02575e6f4bc\",\n" +
            "        \"_rev\": \"1-256d36283315ea9bb045e6d5038657b6\",\n" +
            "        \"data\": {\n" +
            "          \"service_type\": \"org.openstack.nova\",\n" +
            "          \"endpoint\": \"http://cloud.recas.ba.infn.it:5000/v2.0\",\n" +
            "          \"provider_id\": \"provider-RECAS-BARI\",\n" +
            "          \"type\": \"compute\"\n" +
            "        },\n" +
            "        \"type\": \"service\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"7efc59c5db69ea67c5100de0f73ab567\",\n" +
            "      \"key\": [\n" +
            "        \"provider-RECAS-BARI\",\n" +
            "        \"services\"\n" +
            "      ],\n" +
            "      \"value\": {\n" +
            "        \"provider_id\": \"provider-RECAS-BARI\",\n" +
            "        \"type\": \"storage\"\n" +
            "      },\n" +
            "      \"doc\": {\n" +
            "        \"_id\": \"7efc59c5db69ea67c5100de0f73ab567\",\n" +
            "        \"_rev\": \"4-6e2921c359fb76118616e26c7de76397\",\n" +
            "        \"data\": {\n" +
            "          \"service_type\": \"eu.egi.cloud.storage-management.oneprovider\",\n" +
            "          \"endpoint\": \"E1u8A4FgR6C1UgbD2JOoP9OQIG43q-zDsXkx1PoaaI4\",\n" +
            "          \"provider_id\": \"provider-RECAS-BARI\",\n" +
            "          \"type\": \"storage\"\n" +
            "        },\n" +
            "        \"type\": \"service\"\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}",
            MediaType.APPLICATION_JSON));

    // Tenants: Compute service on provider-RECAS-BARI
    mockServer
        .expect(requestTo(baseUrl
            + "/service/id/4401ac5dc8cfbbb737b0a02575e6f4bc/has_many/tenants?include_docs=true"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(
            "{\n" +
            "  \"total_rows\": 264,\n" +
            "  \"offset\": 263,\n" +
            "  \"rows\": [\n" +
            "    {\n" +
            "      \"id\": \"8a5377c6-a7f4-4d1c-a4cd-074ab92b6035\",\n" +
            "      \"key\": [\n" +
            "        \"RECAS-BARI_6be36515-aa38-44ec-905e-be9df893bd95\",\n" +
            "        \"tenants\"\n" +
            "      ],\n" +
            "      \"value\": {\n" +
            "        \"tenant_id\": \"78d9ecb64353402bb621b569891c633a\",\n" +
            "        \"tenant_name\": \"DEEP_DEMO\",\n" +
            "        \"service\": \"RECAS-BARI_6be36515-aa38-44ec-905e-be9df893bd95\",\n" +
            "        \"iam_organisation\": \"deep-hdc\"\n" +
            "      },\n" +
            "      \"doc\": {\n" +
            "        \"_id\": \"8a5377c6-a7f4-4d1c-a4cd-074ab92b6035\",\n" +
            "        \"_rev\": \"32-2d1d8159db1d9e5361e6b537644054e5\",\n" +
            "        \"type\": \"tenant\",\n" +
            "        \"data\": {\n" +
            "          \"iam_organisation\": \"deep-hdc\",\n" +
            "          \"tenant_id\": \"78d9ecb64353402bb621b569891c633a\",\n" +
            "          \"service\": \"RECAS-BARI_6be36515-aa38-44ec-905e-be9df893bd95\",\n" +
            "          \"tenant_name\": \"DEEP_DEMO\"\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}",
            MediaType.APPLICATION_JSON));

    // Tenants: Organization 8a5377c6-a7f4-4d1c-a4cd-074ab92b6035
    mockServer
        .expect(requestTo(baseUrl
            + "/tenant/filters/iam_organisation/8a5377c6-a7f4-4d1c-a4cd-074ab92b6035?include_docs=true"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(
            "{\n" +
            "  \"total_rows\": 803,\n" +
            "  \"offset\": 795,\n" +
            "  \"rows\": [\n" +
            "    {\n" +
            "      \"id\": \"891970a2-58c0-4cee-be10-b162ff44a0c1\",\n" +
            "      \"key\": [\n" +
            "        \"tenant\",\n" +
            "        \"iam_organisation\",\n" +
            "        \"deep-hdc\"\n" +
            "      ],\n" +
            "      \"value\": {\n" +
            "        \"tenant_id\": \"36fb9668c47148d9b1bedba88eb2460a\",\n" +
            "        \"tenant_name\": \"deep-hybrid-datacloud.eu\",\n" +
            "        \"iam_organisation\": \"deep-hdc\"\n" +
            "      },\n" +
            "      \"doc\": {\n" +
            "        \"_id\": \"891970a2-58c0-4cee-be10-b162ff44a0c1\",\n" +
            "        \"_rev\": \"35-5c69984370d9723ca1ad5b58a7037db5\",\n" +
            "        \"type\": \"tenant\",\n" +
            "        \"data\": {\n" +
            "          \"iam_organisation\": \"deep-hdc\",\n" +
            "          \"tenant_id\": \"36fb9668c47148d9b1bedba88eb2460a\",\n" +
            "          \"service\": \"IFCA-LCG2_9fe3a4b5-095d-4ca0-9a25-61f37c3f1081\",\n" +
            "          \"tenant_name\": \"deep-hybrid-datacloud.eu\"\n" +
            "        }\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"8a5377c6-a7f4-4d1c-a4cd-074ab92b6035\",\n" +
            "      \"key\": [\n" +
            "        \"tenant\",\n" +
            "        \"iam_organisation\",\n" +
            "        \"deep-hdc\"\n" +
            "      ],\n" +
            "      \"value\": {\n" +
            "        \"tenant_id\": \"78d9ecb64353402bb621b569891c633a\",\n" +
            "        \"tenant_name\": \"DEEP_DEMO\",\n" +
            "        \"iam_organisation\": \"deep-hdc\"\n" +
            "      },\n" +
            "      \"doc\": {\n" +
            "        \"_id\": \"8a5377c6-a7f4-4d1c-a4cd-074ab92b6035\",\n" +
            "        \"_rev\": \"32-2d1d8159db1d9e5361e6b537644054e5\",\n" +
            "        \"type\": \"tenant\",\n" +
            "        \"data\": {\n" +
            "          \"iam_organisation\": \"deep-hdc\",\n" +
            "          \"tenant_id\": \"78d9ecb64353402bb621b569891c633a\",\n" +
            "          \"service\": \"RECAS-BARI_6be36515-aa38-44ec-905e-be9df893bd95\",\n" +
            "          \"tenant_name\": \"DEEP_DEMO\"\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}",
            MediaType.APPLICATION_JSON));

    // Images: Tenant 8a5377c6-a7f4-4d1c-a4cd-074ab92b6035 on provider-RECAS-BARI
    mockServer
        .expect(requestTo(baseUrl
            + "/tenant/id/8a5377c6-a7f4-4d1c-a4cd-074ab92b6035/has_many/images?include_docs=true"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(
            "{\n" +
            "  \"total_rows\": 264,\n" +
            "  \"offset\": 231,\n" +
            "  \"rows\": [\n" +
            "    {\n" +
            "      \"id\": \"a8f0d13b52dca71703d23f7c7d6a23b0\",\n" +
            "      \"key\": [\n" +
            "        \"8a5377c6-a7f4-4d1c-a4cd-074ab92b6035\",\n" +
            "        \"images\"\n" +
            "      ],\n" +
            "      \"value\": {\n" +
            "        \"image_id\": \"303d8324-69a7-4372-be24-1d68703affd7\",\n" +
            "        \"image_name\": \"Ubuntu 14.04.3 LTS\",\n" +
            "        \"tenant_id\": \"8a5377c6-a7f4-4d1c-a4cd-074ab92b6035\"\n" +
            "      },\n" +
            "      \"doc\": {\n" +
            "        \"_id\": \"a8f0d13b52dca71703d23f7c7d6a23b0\",\n" +
            "        \"_rev\": \"33-469105457d542af7e009e5c4dafa8f6f\",\n" +
            "        \"type\": \"image\",\n" +
            "        \"data\": {\n" +
            "          \"image_marketplace_id\": \"http://cloud.recas.ba.infn.it:9292/v2/images/303d8324-69a7-4372-be24-1d68703affd7/file\",\n" +
            "          \"service\": \"https://cloud.recas.ba.infn.it:5000/v3\",\n" +
            "          \"gpu_driver\": false,\n" +
            "          \"tenant_id\": \"8a5377c6-a7f4-4d1c-a4cd-074ab92b6035\",\n" +
            "          \"image_name\": \"Ubuntu 14.04.3 LTS\",\n" +
            "          \"image_id\": \"303d8324-69a7-4372-be24-1d68703affd7\",\n" +
            "          \"architecture\": \"x86_64\",\n" +
            "          \"tenant_name\": \"DEEP_DEMO\",\n" +
            "          \"type\": \"Linux\",\n" +
            "          \"cuda_support\": false\n" +
            "        }\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"a8f0d13b52dca71703d23f7c7d6a30d2\",\n" +
            "      \"key\": [\n" +
            "        \"8a5377c6-a7f4-4d1c-a4cd-074ab92b6035\",\n" +
            "        \"images\"\n" +
            "      ],\n" +
            "      \"value\": {\n" +
            "        \"image_id\": \"399a8e3e-3f49-47e8-806b-ee7251fd1e03\",\n" +
            "        \"image_name\": \"ubuntu-16.04-vmi\",\n" +
            "        \"tenant_id\": \"8a5377c6-a7f4-4d1c-a4cd-074ab92b6035\"\n" +
            "      },\n" +
            "      \"doc\": {\n" +
            "        \"_id\": \"a8f0d13b52dca71703d23f7c7d6a30d2\",\n" +
            "        \"_rev\": \"33-e1aecde6780b9c15615a898df7027ee5\",\n" +
            "        \"type\": \"image\",\n" +
            "        \"data\": {\n" +
            "          \"image_marketplace_id\": \"http://cloud.recas.ba.infn.it:9292/v2/images/399a8e3e-3f49-47e8-806b-ee7251fd1e03/file\",\n" +
            "          \"service\": \"https://cloud.recas.ba.infn.it:5000/v3\",\n" +
            "          \"gpu_driver\": false,\n" +
            "          \"tenant_id\": \"8a5377c6-a7f4-4d1c-a4cd-074ab92b6035\",\n" +
            "          \"image_name\": \"ubuntu-16.04-vmi\",\n" +
            "          \"image_id\": \"399a8e3e-3f49-47e8-806b-ee7251fd1e03\",\n" +
            "          \"architecture\": \"x86_64\",\n" +
            "          \"tenant_name\": \"DEEP_DEMO\",\n" +
            "          \"type\": \"Linux\",\n" +
            "          \"cuda_support\": false\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}",
            MediaType.APPLICATION_JSON));

    // Flavor: Tenant 8a5377c6-a7f4-4d1c-a4cd-074ab92b6035 on provider-RECAS-BARI
    mockServer
        .expect(requestTo(baseUrl
            + "/tenant/id/8a5377c6-a7f4-4d1c-a4cd-074ab92b6035/has_many/flavors?include_docs=true"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(
            "{\n" +
            "  \"total_rows\": 147,\n" +
            "  \"offset\": 0,\n" +
            "  \"rows\": [\n" +
            "    {\n" +
            "      \"id\": \"5f94fc673a476e2a9fa63c713000b45b\",\n" +
            "      \"key\": [\n" +
            "        \"4401ac5dc8cfbbb737b0a02575e6f4bc\",\n" +
            "        \"flavors\"\n" +
            "      ],\n" +
            "      \"value\": {\n" +
            "        \"flavor_id\": \"7\",\n" +
            "        \"flavor_name\": \"small\",\n" +
            "        \"service\": \"4401ac5dc8cfbbb737b0a02575e6f4bc\"\n" +
            "      },\n" +
            "      \"doc\": {\n" +
            "        \"_id\": \"5f94fc673a476e2a9fa63c713000b45b\",\n" +
            "        \"_rev\": \"1-0de347bde54f5dc2fe44606fc42553f7\",\n" +
            "        \"type\": \"flavor\",\n" +
            "        \"data\": {\n" +
            "          \"gpu_vendor\": null,\n" +
            "          \"service\": \"4401ac5dc8cfbbb737b0a02575e6f4bc\",\n" +
            "          \"num_gpus\": 0,\n" +
            "          \"tenant_id\": \"5f94fc673a476e2a9fa63c713000acda\",\n" +
            "          \"ram\": 2048,\n" +
            "          \"num_vcpus\": 1,\n" +
            "          \"flavor_id\": \"7\",\n" +
            "          \"tenant_name\": \"DEEP_DEMO\",\n" +
            "          \"gpu_model\": null,\n" +
            "          \"disk\": 20,\n" +
            "          \"flavor_name\": \"small\"\n" +
            "        }\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"5f94fc673a476e2a9fa63c713000bd0f\",\n" +
            "      \"key\": [\n" +
            "        \"4401ac5dc8cfbbb737b0a02575e6f4bc\",\n" +
            "        \"flavors\"\n" +
            "      ],\n" +
            "      \"value\": {\n" +
            "        \"flavor_id\": \"8\",\n" +
            "        \"flavor_name\": \"medium\",\n" +
            "        \"service\": \"4401ac5dc8cfbbb737b0a02575e6f4bc\"\n" +
            "      },\n" +
            "      \"doc\": {\n" +
            "        \"_id\": \"5f94fc673a476e2a9fa63c713000bd0f\",\n" +
            "        \"_rev\": \"1-ab7cd100475d996cac5cb078e8579f1a\",\n" +
            "        \"type\": \"flavor\",\n" +
            "        \"data\": {\n" +
            "          \"gpu_vendor\": null,\n" +
            "          \"service\": \"4401ac5dc8cfbbb737b0a02575e6f4bc\",\n" +
            "          \"num_gpus\": 0,\n" +
            "          \"tenant_id\": \"5f94fc673a476e2a9fa63c713000acda\",\n" +
            "          \"ram\": 4096,\n" +
            "          \"num_vcpus\": 2,\n" +
            "          \"flavor_id\": \"8\",\n" +
            "          \"tenant_name\": \"DEEP_DEMO\",\n" +
            "          \"gpu_model\": null,\n" +
            "          \"disk\": 20,\n" +
            "          \"flavor_name\": \"medium\"\n" +
            "        }\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"5f94fc673a476e2a9fa63c713000cc17\",\n" +
            "      \"key\": [\n" +
            "        \"4401ac5dc8cfbbb737b0a02575e6f4bc\",\n" +
            "        \"flavors\"\n" +
            "      ],\n" +
            "      \"value\": {\n" +
            "        \"flavor_id\": \"9\",\n" +
            "        \"flavor_name\": \"large\",\n" +
            "        \"service\": \"4401ac5dc8cfbbb737b0a02575e6f4bc\"\n" +
            "      },\n" +
            "      \"doc\": {\n" +
            "        \"_id\": \"5f94fc673a476e2a9fa63c713000cc17\",\n" +
            "        \"_rev\": \"1-9bbae4c4e42e3b4a5b14de72a20379ac\",\n" +
            "        \"type\": \"flavor\",\n" +
            "        \"data\": {\n" +
            "          \"gpu_vendor\": null,\n" +
            "          \"service\": \"4401ac5dc8cfbbb737b0a02575e6f4bc\",\n" +
            "          \"num_gpus\": 0,\n" +
            "          \"tenant_id\": \"5f94fc673a476e2a9fa63c713000acda\",\n" +
            "          \"ram\": 8192,\n" +
            "          \"num_vcpus\": 4,\n" +
            "          \"flavor_id\": \"9\",\n" +
            "          \"tenant_name\": \"DEEP_DEMO\",\n" +
            "          \"gpu_model\": null,\n" +
            "          \"disk\": 20,\n" +
            "          \"flavor_name\": \"large\"\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}",
            MediaType.APPLICATION_JSON));

    // Provider: provider-UPV-GRyCAP
    mockServer.expect(requestTo(baseUrl + "/provider/id/provider-UPV-GRyCAP?include_docs=true"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(
            "{\n" +
            "  \"_id\": \"provider-UPV-GRyCAP\",\n" +
            "  \"_rev\": \"1-0a5ba48b2d6e0c26d36b0e3e81175352\",\n" +
            "  \"data\": {\n" +
            "    \"id\": \"458\",\n" +
            "    \"primary_key\": \"135G0\",\n" +
            "    \"name\": \"UPV-GRyCAP\",\n" +
            "    \"country\": \"Spain\",\n" +
            "    \"country_code\": \"ES\",\n" +
            "    \"roc\": \"NGI_IBERGRID\",\n" +
            "    \"subgrid\": \"\",\n" +
            "    \"giis_url\": \"ldap://ngiesbdii.i3m.upv.es:2170/mds-vo-name=UPV-GRyCAP,o=grid\"\n" +
            "  },\n" +
            "  \"type\": \"provider\"\n" +
            "}",
            MediaType.APPLICATION_JSON));

    // Service: Compute on provider-UPV-GRyCAP
    mockServer
        .expect(requestTo(
            baseUrl + "/provider/id/provider-UPV-GRyCAP/has_many/services?include_docs=true"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(
            "{\n" +
            "  \"total_rows\": 60,\n" +
            "  \"offset\": 55,\n" +
            "  \"rows\": [\n" +
            "    {\n" +
            "      \"id\": \"4401ac5dc8cfbbb737b0a025758d6e05\",\n" +
            "      \"key\": [\n" +
            "        \"provider-UPV-GRyCAP\",\n" +
            "        \"services\"\n" +
            "      ],\n" +
            "      \"value\": {\n" +
            "        \"sitename\": \"UPV-GRyCAP\",\n" +
            "        \"provider_id\": \"provider-UPV-GRyCAP\",\n" +
            "        \"hostname\": \"fc-one.i3m.upv.es\",\n" +
            "        \"type\": \"compute\"\n" +
            "      },\n" +
            "      \"doc\": {\n" +
            "        \"_id\": \"4401ac5dc8cfbbb737b0a025758d6e05\",\n" +
            "        \"_rev\": \"2-5f5a496ffa93b98cedc8b19b16224763\",\n" +
            "        \"data\": {\n" +
            "          \"primary_key\": \"5973G0\",\n" +
            "          \"hostname\": \"fc-one.i3m.upv.es\",\n" +
            "          \"gocdb_portal_url\": \"https://goc.egi.eu/portal/index.php?Page_Type=Service&id=5973\",\n" +
            "          \"hostdn\": \"/DC=es/DC=irisgrid/O=cesga/CN=host/fc-one.i3m.upv.es\",\n" +
            "          \"host_os\": \"Ubuntu 12.04\",\n" +
            "          \"host_arch\": \"x86_64\",\n" +
            "          \"beta\": \"N\",\n" +
            "          \"service_type\": \"eu.egi.cloud.vm-management.occi\",\n" +
            "          \"host_ip\": \"158.42.104.226\",\n" +
            "          \"core\": null,\n" +
            "          \"in_production\": \"Y\",\n" +
            "          \"node_monitored\": \"Y\",\n" +
            "          \"sitename\": \"UPV-GRyCAP\",\n" +
            "          \"country_name\": \"Spain\",\n" +
            "          \"country_code\": \"ES\",\n" +
            "          \"roc_name\": \"NGI_IBERGRID\",\n" +
            "          \"scopes\": {\n" +
            "            \"scope\": \"EGI\"\n" +
            "          },\n" +
            "          \"extensions\": null,\n" +
            "          \"type\": \"compute\",\n" +
            "          \"provider_id\": \"provider-UPV-GRyCAP\",\n" +
            "          \"endpoint\": \"https://fc-one.i3m.upv.es:11443/\"\n" +
            "        },\n" +
            "        \"type\": \"service\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"4401ac5dc8cfbbb737b0a02575e8040f\",\n" +
            "      \"key\": [\n" +
            "        \"provider-UPV-GRyCAP\",\n" +
            "        \"services\"\n" +
            "      ],\n" +
            "      \"value\": {\n" +
            "        \"provider_id\": \"provider-UPV-GRyCAP\",\n" +
            "        \"type\": \"compute\"\n" +
            "      },\n" +
            "      \"doc\": {\n" +
            "        \"_id\": \"4401ac5dc8cfbbb737b0a02575e8040f\",\n" +
            "        \"_rev\": \"2-be00f87438604f04d353233daabc562c\",\n" +
            "        \"data\": {\n" +
            "          \"service_type\": \"eu.egi.cloud.vm-management.occi\",\n" +
            "          \"endpoint\": \"http://onedock.i3m.upv.es:11443\",\n" +
            "          \"provider_id\": \"provider-UPV-GRyCAP\",\n" +
            "          \"type\": \"compute\"\n" +
            "        },\n" +
            "        \"type\": \"service\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"4401ac5dc8cfbbb737b0a02575e81d9b\",\n" +
            "      \"key\": [\n" +
            "        \"provider-UPV-GRyCAP\",\n" +
            "        \"services\"\n" +
            "      ],\n" +
            "      \"value\": {\n" +
            "        \"provider_id\": \"provider-UPV-GRyCAP\",\n" +
            "        \"type\": \"compute\"\n" +
            "      },\n" +
            "      \"doc\": {\n" +
            "        \"_id\": \"4401ac5dc8cfbbb737b0a02575e81d9b\",\n" +
            "        \"_rev\": \"3-cb010c6d67f8d723777a777d96ae70ff\",\n" +
            "        \"data\": {\n" +
            "          \"service_type\": \"eu.egi.cloud.vm-management.opennebula\",\n" +
            "          \"endpoint\": \"http://onedock.i3m.upv.es:2633/\",\n" +
            "          \"provider_id\": \"provider-UPV-GRyCAP\",\n" +
            "          \"type\": \"compute\"\n" +
            "        },\n" +
            "        \"type\": \"service\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"7efc59c5db69ea67c5100de0f73981be\",\n" +
            "      \"key\": [\n" +
            "        \"provider-UPV-GRyCAP\",\n" +
            "        \"services\"\n" +
            "      ],\n" +
            "      \"value\": {\n" +
            "        \"provider_id\": \"provider-UPV-GRyCAP\",\n" +
            "        \"type\": \"compute\"\n" +
            "      },\n" +
            "      \"doc\": {\n" +
            "        \"_id\": \"7efc59c5db69ea67c5100de0f73981be\",\n" +
            "        \"_rev\": \"1-d75826f3a04af0d843d6d7ee48fc4ce4\",\n" +
            "        \"data\": {\n" +
            "          \"service_type\": \"eu.egi.cloud.vm-management.opennebula\",\n" +
            "          \"endpoint\": \"http://onecloud.i3m.upv.es:2633/\",\n" +
            "          \"provider_id\": \"provider-UPV-GRyCAP\",\n" +
            "          \"type\": \"compute\"\n" +
            "        },\n" +
            "        \"type\": \"service\"\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}",
            MediaType.APPLICATION_JSON));

    // Tenants: Compute service on provider-RECAS-BARI
    mockServer
        .expect(requestTo(baseUrl
            + "/service/id/4401ac5dc8cfbbb737b0a02575e81d9b/has_many/tenants?include_docs=true"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(
            "{}",
            MediaType.APPLICATION_JSON));

    // Tenants: Organization deep-hdc
    mockServer
        .expect(requestTo(baseUrl
            + "/tenant/filters/iam_organisation/8a5377c6-a7f4-4d1c-a4cd-074ab92b6035?include_docs=true"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(
            "{}",
            MediaType.APPLICATION_JSON));

  }

  public static void mockMonitoring(MockRestServiceServer mockServer, URI baseUrl)
      throws Exception {

    // provider-RECAS-BARI
    mockServer.expect(requestTo(baseUrl + "/monitoring/adapters/zabbix/zones/indigo/types/"
          + "infrastructure/groups/provider-RECAS-BARI")).andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(
            "{\n" +
            "  \"meta\": {\n" +
            "    \"status\": 200,\n" +
            "    \"additionalProperties\": {}\n" +
            "  },\n" +
            "  \"result\": {\n" +
            "    \"groups\": [\n" +
            "      {\n" +
            "        \"groupName\": \"provider-RECAS-BARI\",\n" +
            "        \"paasMachines\": [\n" +
            "          {\n" +
            "            \"machineName\": \"4401ac5dc8cfbbb737b0a02575e6f4bc\",\n" +
            "            \"ip\": \"127.0.0.1\",\n" +
            "            \"serviceCategory\": \"org.openstack.nova\",\n" +
            "            \"serviceId\": \"IaaS_provider-RECAS-BARI_4401ac5dc8cfbbb737b0a02575e6f4bc\",\n" +
            "            \"services\": [\n" +
            "              {\n" +
            "                \"serviceName\": \"\",\n" +
            "                \"paasMetrics\": [\n" +
            "                  {\n" +
            "                    \"metricName\": \"clear_responseTime\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.4401ac5dc8cfbbb737b0a02575e6f4bc..clear_responseTime\",\n" +
            "                    \"metricValue\": 34272,\n" +
            "                    \"metricTime\": \"03-09-201916:20:08\",\n" +
            "                    \"metricUnit\": \"ms\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"clear_result\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.4401ac5dc8cfbbb737b0a02575e6f4bc..clear_result\",\n" +
            "                    \"metricValue\": 1,\n" +
            "                    \"metricTime\": \"03-09-201916:20:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"clear_status\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.4401ac5dc8cfbbb737b0a02575e6f4bc..clear_status\",\n" +
            "                    \"metricValue\": 204,\n" +
            "                    \"metricTime\": \"03-09-201916:20:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"create_responseTime\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.4401ac5dc8cfbbb737b0a02575e6f4bc..create_responseTime\",\n" +
            "                    \"metricValue\": 711,\n" +
            "                    \"metricTime\": \"03-09-201916:20:08\",\n" +
            "                    \"metricUnit\": \"ms\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"create_result\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.4401ac5dc8cfbbb737b0a02575e6f4bc..create_result\",\n" +
            "                    \"metricValue\": 1,\n" +
            "                    \"metricTime\": \"03-09-201916:20:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"create_status\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.4401ac5dc8cfbbb737b0a02575e6f4bc..create_status\",\n" +
            "                    \"metricValue\": 200,\n" +
            "                    \"metricTime\": \"03-09-201916:20:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"delete_responseTime\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.4401ac5dc8cfbbb737b0a02575e6f4bc..delete_responseTime\",\n" +
            "                    \"metricValue\": 856,\n" +
            "                    \"metricTime\": \"03-09-201916:20:08\",\n" +
            "                    \"metricUnit\": \"ms\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"delete_result\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.4401ac5dc8cfbbb737b0a02575e6f4bc..delete_result\",\n" +
            "                    \"metricValue\": 1,\n" +
            "                    \"metricTime\": \"03-09-201916:20:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"delete_status\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.4401ac5dc8cfbbb737b0a02575e6f4bc..delete_status\",\n" +
            "                    \"metricValue\": 200,\n" +
            "                    \"metricTime\": \"03-09-201916:20:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"openstack_responseTime\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.4401ac5dc8cfbbb737b0a02575e6f4bc..openstack_responseTime\",\n" +
            "                    \"metricValue\": 0,\n" +
            "                    \"metricTime\": \"Instantnullbecausenometricswerereturnedinthelast24hs\",\n" +
            "                    \"metricUnit\": \"ms\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"openstack_result\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.4401ac5dc8cfbbb737b0a02575e6f4bc..openstack_result\",\n" +
            "                    \"metricValue\": 0,\n" +
            "                    \"metricTime\": \"Instantnullbecausenometricswerereturnedinthelast24hs\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"openstack_status\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.4401ac5dc8cfbbb737b0a02575e6f4bc..openstack_status\",\n" +
            "                    \"metricValue\": 0,\n" +
            "                    \"metricTime\": \"Instantnullbecausenometricswerereturnedinthelast24hs\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"run_responseTime\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.4401ac5dc8cfbbb737b0a02575e6f4bc..run_responseTime\",\n" +
            "                    \"metricValue\": 393,\n" +
            "                    \"metricTime\": \"03-09-201916:20:08\",\n" +
            "                    \"metricUnit\": \"ms\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"run_result\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.4401ac5dc8cfbbb737b0a02575e6f4bc..run_result\",\n" +
            "                    \"metricValue\": 1,\n" +
            "                    \"metricTime\": \"03-09-201916:20:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"run_status\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.4401ac5dc8cfbbb737b0a02575e6f4bc..run_status\",\n" +
            "                    \"metricValue\": 200,\n" +
            "                    \"metricTime\": \"03-09-201916:20:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  }\n" +
            "                ]\n" +
            "              }\n" +
            "            ]\n" +
            "          },\n" +
            "          {\n" +
            "            \"machineName\": \"61b5f554-c44c-4dcc-8373-3dade70b1901\",\n" +
            "            \"ip\": \"127.0.0.1\",\n" +
            "            \"serviceCategory\": \"eu.indigo-datacloud.mesos\",\n" +
            "            \"serviceId\": \"IaaS_provider-RECAS-BARI_61b5f554-c44c-4dcc-8373-3dade70b1901\",\n" +
            "            \"services\": [\n" +
            "              {\n" +
            "                \"serviceName\": \"\",\n" +
            "                \"paasMetrics\": [\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.cpus_percent\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.61b5f554-c44c-4dcc-8373-3dade70b1901..master.cpus_percent\",\n" +
            "                    \"metricValue\": 0.0556,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.cpus_total\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.61b5f554-c44c-4dcc-8373-3dade70b1901..master.cpus_total\",\n" +
            "                    \"metricValue\": 72,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.cpus_used\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.61b5f554-c44c-4dcc-8373-3dade70b1901..master.cpus_used\",\n" +
            "                    \"metricValue\": 4,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.disk_percent\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.61b5f554-c44c-4dcc-8373-3dade70b1901..master.disk_percent\",\n" +
            "                    \"metricValue\": 0,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.disk_total\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.61b5f554-c44c-4dcc-8373-3dade70b1901..master.disk_total\",\n" +
            "                    \"metricValue\": 3952957,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.disk_used\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.61b5f554-c44c-4dcc-8373-3dade70b1901..master.disk_used\",\n" +
            "                    \"metricValue\": 0,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.dropped_messages\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.61b5f554-c44c-4dcc-8373-3dade70b1901..master.dropped_messages\",\n" +
            "                    \"metricValue\": 13,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.elected\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.61b5f554-c44c-4dcc-8373-3dade70b1901..master.elected\",\n" +
            "                    \"metricValue\": 1,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.frameworks_disconnected\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.61b5f554-c44c-4dcc-8373-3dade70b1901..master.frameworks_disconnected\",\n" +
            "                    \"metricValue\": 0,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.frameworks_inactive\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.61b5f554-c44c-4dcc-8373-3dade70b1901..master.frameworks_inactive\",\n" +
            "                    \"metricValue\": 0,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.gpus_percent\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.61b5f554-c44c-4dcc-8373-3dade70b1901..master.gpus_percent\",\n" +
            "                    \"metricValue\": 0.6667,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.gpus_total\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.61b5f554-c44c-4dcc-8373-3dade70b1901..master.gpus_total\",\n" +
            "                    \"metricValue\": 3,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.gpus_used\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.61b5f554-c44c-4dcc-8373-3dade70b1901..master.gpus_used\",\n" +
            "                    \"metricValue\": 2,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.mem_percent\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.61b5f554-c44c-4dcc-8373-3dade70b1901..master.mem_percent\",\n" +
            "                    \"metricValue\": 0.0639,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.mem_total\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.61b5f554-c44c-4dcc-8373-3dade70b1901..master.mem_total\",\n" +
            "                    \"metricValue\": 320265,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.mem_used\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.61b5f554-c44c-4dcc-8373-3dade70b1901..master.mem_used\",\n" +
            "                    \"metricValue\": 20480,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.slaves_active\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.61b5f554-c44c-4dcc-8373-3dade70b1901..master.slaves_active\",\n" +
            "                    \"metricValue\": 2,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.tasks_lost\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.61b5f554-c44c-4dcc-8373-3dade70b1901..master.tasks_lost\",\n" +
            "                    \"metricValue\": 0,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.uptime_secs\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.61b5f554-c44c-4dcc-8373-3dade70b1901..master.uptime_secs\",\n" +
            "                    \"metricValue\": 9012352,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"secs\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  }\n" +
            "                ]\n" +
            "              }\n" +
            "            ]\n" +
            "          },\n" +
            "          {\n" +
            "            \"machineName\": \"8922daf1-1a53-41b4-9f18-7ed536b44f20\",\n" +
            "            \"ip\": \"127.0.0.1\",\n" +
            "            \"serviceCategory\": \"eu.indigo-datacloud.marathon\",\n" +
            "            \"serviceId\": \"IaaS_provider-RECAS-BARI_8922daf1-1a53-41b4-9f18-7ed536b44f20\",\n" +
            "            \"services\": [\n" +
            "              {\n" +
            "                \"serviceName\": \"\",\n" +
            "                \"paasMetrics\": [\n" +
            "                  {\n" +
            "                    \"metricName\": \"clear_responseTime\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.8922daf1-1a53-41b4-9f18-7ed536b44f20..clear_responseTime\",\n" +
            "                    \"metricValue\": 132,\n" +
            "                    \"metricTime\": \"03-09-201915:30:08\",\n" +
            "                    \"metricUnit\": \"ms\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"clear_result\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.8922daf1-1a53-41b4-9f18-7ed536b44f20..clear_result\",\n" +
            "                    \"metricValue\": 1,\n" +
            "                    \"metricTime\": \"03-09-201915:30:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"clear_status\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.8922daf1-1a53-41b4-9f18-7ed536b44f20..clear_status\",\n" +
            "                    \"metricValue\": 404,\n" +
            "                    \"metricTime\": \"03-09-201915:30:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"create_responseTime\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.8922daf1-1a53-41b4-9f18-7ed536b44f20..create_responseTime\",\n" +
            "                    \"metricValue\": 110,\n" +
            "                    \"metricTime\": \"03-09-201915:30:08\",\n" +
            "                    \"metricUnit\": \"ms\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"create_result\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.8922daf1-1a53-41b4-9f18-7ed536b44f20..create_result\",\n" +
            "                    \"metricValue\": 1,\n" +
            "                    \"metricTime\": \"03-09-201915:30:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"create_status\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.8922daf1-1a53-41b4-9f18-7ed536b44f20..create_status\",\n" +
            "                    \"metricValue\": 200,\n" +
            "                    \"metricTime\": \"03-09-201915:30:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"delete_responseTime\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.8922daf1-1a53-41b4-9f18-7ed536b44f20..delete_responseTime\",\n" +
            "                    \"metricValue\": 51,\n" +
            "                    \"metricTime\": \"03-09-201915:30:08\",\n" +
            "                    \"metricUnit\": \"ms\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"delete_result\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.8922daf1-1a53-41b4-9f18-7ed536b44f20..delete_result\",\n" +
            "                    \"metricValue\": 1,\n" +
            "                    \"metricTime\": \"03-09-201915:30:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"delete_status\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.8922daf1-1a53-41b4-9f18-7ed536b44f20..delete_status\",\n" +
            "                    \"metricValue\": 200,\n" +
            "                    \"metricTime\": \"03-09-201915:30:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"run_responseTime\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.8922daf1-1a53-41b4-9f18-7ed536b44f20..run_responseTime\",\n" +
            "                    \"metricValue\": 20,\n" +
            "                    \"metricTime\": \"03-09-201915:30:08\",\n" +
            "                    \"metricUnit\": \"ms\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"run_result\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.8922daf1-1a53-41b4-9f18-7ed536b44f20..run_result\",\n" +
            "                    \"metricValue\": 1,\n" +
            "                    \"metricTime\": \"03-09-201915:30:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"run_status\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.8922daf1-1a53-41b4-9f18-7ed536b44f20..run_status\",\n" +
            "                    \"metricValue\": 200,\n" +
            "                    \"metricTime\": \"03-09-201915:30:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  }\n" +
            "                ]\n" +
            "              }\n" +
            "            ]\n" +
            "          },\n" +
            "          {\n" +
            "            \"machineName\": \"8490545c-f5ec-4330-9116-604dc0084caa\",\n" +
            "            \"ip\": \"127.0.0.1\",\n" +
            "            \"serviceCategory\": \"eu.indigo-datacloud.chronos\",\n" +
            "            \"serviceId\": \"IaaS_provider-RECAS-BARI_8490545c-f5ec-4330-9116-604dc0084caa\",\n" +
            "            \"services\": [\n" +
            "              {\n" +
            "                \"serviceName\": \"\",\n" +
            "                \"paasMetrics\": [\n" +
            "                  {\n" +
            "                    \"metricName\": \"clear_responseTime\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.8490545c-f5ec-4330-9116-604dc0084caa..clear_responseTime\",\n" +
            "                    \"metricValue\": 200,\n" +
            "                    \"metricTime\": \"03-09-201916:15:03\",\n" +
            "                    \"metricUnit\": \"ms\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"clear_result\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.8490545c-f5ec-4330-9116-604dc0084caa..clear_result\",\n" +
            "                    \"metricValue\": 1,\n" +
            "                    \"metricTime\": \"03-09-201916:15:03\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"clear_status\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.8490545c-f5ec-4330-9116-604dc0084caa..clear_status\",\n" +
            "                    \"metricValue\": 404,\n" +
            "                    \"metricTime\": \"03-09-201916:15:03\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"create_responseTime\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.8490545c-f5ec-4330-9116-604dc0084caa..create_responseTime\",\n" +
            "                    \"metricValue\": 110,\n" +
            "                    \"metricTime\": \"03-09-201916:15:03\",\n" +
            "                    \"metricUnit\": \"ms\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"create_result\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.8490545c-f5ec-4330-9116-604dc0084caa..create_result\",\n" +
            "                    \"metricValue\": 1,\n" +
            "                    \"metricTime\": \"03-09-201916:15:03\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"create_status\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.8490545c-f5ec-4330-9116-604dc0084caa..create_status\",\n" +
            "                    \"metricValue\": 200,\n" +
            "                    \"metricTime\": \"03-09-201916:15:03\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"delete_responseTime\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.8490545c-f5ec-4330-9116-604dc0084caa..delete_responseTime\",\n" +
            "                    \"metricValue\": 15,\n" +
            "                    \"metricTime\": \"03-09-201916:15:03\",\n" +
            "                    \"metricUnit\": \"ms\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"delete_result\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.8490545c-f5ec-4330-9116-604dc0084caa..delete_result\",\n" +
            "                    \"metricValue\": 1,\n" +
            "                    \"metricTime\": \"03-09-201916:15:03\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"delete_status\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.8490545c-f5ec-4330-9116-604dc0084caa..delete_status\",\n" +
            "                    \"metricValue\": 200,\n" +
            "                    \"metricTime\": \"03-09-201916:15:03\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"run_responseTime\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.8490545c-f5ec-4330-9116-604dc0084caa..run_responseTime\",\n" +
            "                    \"metricValue\": 13,\n" +
            "                    \"metricTime\": \"03-09-201916:15:03\",\n" +
            "                    \"metricUnit\": \"ms\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"run_result\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.8490545c-f5ec-4330-9116-604dc0084caa..run_result\",\n" +
            "                    \"metricValue\": 1,\n" +
            "                    \"metricTime\": \"03-09-201916:15:03\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"run_status\",\n" +
            "                    \"metricKey\": \"provider-RECAS-BARI.8490545c-f5ec-4330-9116-604dc0084caa..run_status\",\n" +
            "                    \"metricValue\": 200,\n" +
            "                    \"metricTime\": \"03-09-201916:15:03\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  }\n" +
            "                ]\n" +
            "              }\n" +
            "            ]\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  \"additionalProperties\": {}\n" +
            "}",
            MediaType.APPLICATION_JSON));

    // provider-UPV-GRyCAP
    mockServer.expect(requestTo(baseUrl + "/monitoring/adapters/zabbix/zones/indigo/types/"
          + "infrastructure/groups/provider-UPV-GRyCAP")).andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(
            "{\n" +
            "  \"meta\": {\n" +
            "    \"status\": 200,\n" +
            "    \"additionalProperties\": {}\n" +
            "  },\n" +
            "  \"result\": {\n" +
            "    \"groups\": [\n" +
            "      {\n" +
            "        \"groupName\": \"provider-UPV-GRyCAP\",\n" +
            "        \"paasMachines\": [\n" +
            "          {\n" +
            "            \"machineName\": \"4401ac5dc8cfbbb737b0a02575e81d9b\",\n" +
            "            \"ip\": \"127.0.0.1\",\n" +
            "            \"serviceCategory\": \"org.openstack.nova\",\n" +
            "            \"serviceId\": \"IaaS_provider-UPV-GRyCAP_4401ac5dc8cfbbb737b0a02575e81d9b\",\n" +
            "            \"services\": [\n" +
            "              {\n" +
            "                \"serviceName\": \"\",\n" +
            "                \"paasMetrics\": [\n" +
            "                  {\n" +
            "                    \"metricName\": \"clear_responseTime\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.4401ac5dc8cfbbb737b0a02575e81d9b..clear_responseTime\",\n" +
            "                    \"metricValue\": 34272,\n" +
            "                    \"metricTime\": \"03-09-201916:20:08\",\n" +
            "                    \"metricUnit\": \"ms\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"clear_result\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.4401ac5dc8cfbbb737b0a02575e81d9b..clear_result\",\n" +
            "                    \"metricValue\": 1,\n" +
            "                    \"metricTime\": \"03-09-201916:20:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"clear_status\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.4401ac5dc8cfbbb737b0a02575e81d9b..clear_status\",\n" +
            "                    \"metricValue\": 204,\n" +
            "                    \"metricTime\": \"03-09-201916:20:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"create_responseTime\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.4401ac5dc8cfbbb737b0a02575e81d9b..create_responseTime\",\n" +
            "                    \"metricValue\": 711,\n" +
            "                    \"metricTime\": \"03-09-201916:20:08\",\n" +
            "                    \"metricUnit\": \"ms\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"create_result\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.4401ac5dc8cfbbb737b0a02575e81d9b..create_result\",\n" +
            "                    \"metricValue\": 1,\n" +
            "                    \"metricTime\": \"03-09-201916:20:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"create_status\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.4401ac5dc8cfbbb737b0a02575e81d9b..create_status\",\n" +
            "                    \"metricValue\": 200,\n" +
            "                    \"metricTime\": \"03-09-201916:20:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"delete_responseTime\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.4401ac5dc8cfbbb737b0a02575e81d9b..delete_responseTime\",\n" +
            "                    \"metricValue\": 856,\n" +
            "                    \"metricTime\": \"03-09-201916:20:08\",\n" +
            "                    \"metricUnit\": \"ms\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"delete_result\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.4401ac5dc8cfbbb737b0a02575e81d9b..delete_result\",\n" +
            "                    \"metricValue\": 1,\n" +
            "                    \"metricTime\": \"03-09-201916:20:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"delete_status\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.4401ac5dc8cfbbb737b0a02575e81d9b..delete_status\",\n" +
            "                    \"metricValue\": 200,\n" +
            "                    \"metricTime\": \"03-09-201916:20:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"openstack_responseTime\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.4401ac5dc8cfbbb737b0a02575e81d9b..openstack_responseTime\",\n" +
            "                    \"metricValue\": 0,\n" +
            "                    \"metricTime\": \"Instantnullbecausenometricswerereturnedinthelast24hs\",\n" +
            "                    \"metricUnit\": \"ms\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"openstack_result\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.4401ac5dc8cfbbb737b0a02575e81d9b..openstack_result\",\n" +
            "                    \"metricValue\": 0,\n" +
            "                    \"metricTime\": \"Instantnullbecausenometricswerereturnedinthelast24hs\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"openstack_status\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.4401ac5dc8cfbbb737b0a02575e81d9b..openstack_status\",\n" +
            "                    \"metricValue\": 0,\n" +
            "                    \"metricTime\": \"Instantnullbecausenometricswerereturnedinthelast24hs\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"run_responseTime\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.4401ac5dc8cfbbb737b0a02575e81d9b..run_responseTime\",\n" +
            "                    \"metricValue\": 393,\n" +
            "                    \"metricTime\": \"03-09-201916:20:08\",\n" +
            "                    \"metricUnit\": \"ms\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"run_result\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.4401ac5dc8cfbbb737b0a02575e81d9b..run_result\",\n" +
            "                    \"metricValue\": 1,\n" +
            "                    \"metricTime\": \"03-09-201916:20:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"run_status\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.4401ac5dc8cfbbb737b0a02575e81d9b..run_status\",\n" +
            "                    \"metricValue\": 200,\n" +
            "                    \"metricTime\": \"03-09-201916:20:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  }\n" +
            "                ]\n" +
            "              }\n" +
            "            ]\n" +
            "          },\n" +
            "          {\n" +
            "            \"machineName\": \"61b5f554-c44c-4dcc-8373-3dade70b1901\",\n" +
            "            \"ip\": \"127.0.0.1\",\n" +
            "            \"serviceCategory\": \"eu.indigo-datacloud.mesos\",\n" +
            "            \"serviceId\": \"IaaS_provider-UPV-GRyCAP_61b5f554-c44c-4dcc-8373-3dade70b1901\",\n" +
            "            \"services\": [\n" +
            "              {\n" +
            "                \"serviceName\": \"\",\n" +
            "                \"paasMetrics\": [\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.cpus_percent\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.61b5f554-c44c-4dcc-8373-3dade70b1901..master.cpus_percent\",\n" +
            "                    \"metricValue\": 0.0556,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.cpus_total\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.61b5f554-c44c-4dcc-8373-3dade70b1901..master.cpus_total\",\n" +
            "                    \"metricValue\": 72,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.cpus_used\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.61b5f554-c44c-4dcc-8373-3dade70b1901..master.cpus_used\",\n" +
            "                    \"metricValue\": 4,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.disk_percent\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.61b5f554-c44c-4dcc-8373-3dade70b1901..master.disk_percent\",\n" +
            "                    \"metricValue\": 0,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.disk_total\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.61b5f554-c44c-4dcc-8373-3dade70b1901..master.disk_total\",\n" +
            "                    \"metricValue\": 3952957,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.disk_used\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.61b5f554-c44c-4dcc-8373-3dade70b1901..master.disk_used\",\n" +
            "                    \"metricValue\": 0,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.dropped_messages\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.61b5f554-c44c-4dcc-8373-3dade70b1901..master.dropped_messages\",\n" +
            "                    \"metricValue\": 13,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.elected\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.61b5f554-c44c-4dcc-8373-3dade70b1901..master.elected\",\n" +
            "                    \"metricValue\": 1,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.frameworks_disconnected\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.61b5f554-c44c-4dcc-8373-3dade70b1901..master.frameworks_disconnected\",\n" +
            "                    \"metricValue\": 0,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.frameworks_inactive\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.61b5f554-c44c-4dcc-8373-3dade70b1901..master.frameworks_inactive\",\n" +
            "                    \"metricValue\": 0,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.gpus_percent\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.61b5f554-c44c-4dcc-8373-3dade70b1901..master.gpus_percent\",\n" +
            "                    \"metricValue\": 0.6667,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.gpus_total\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.61b5f554-c44c-4dcc-8373-3dade70b1901..master.gpus_total\",\n" +
            "                    \"metricValue\": 3,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.gpus_used\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.61b5f554-c44c-4dcc-8373-3dade70b1901..master.gpus_used\",\n" +
            "                    \"metricValue\": 2,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.mem_percent\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.61b5f554-c44c-4dcc-8373-3dade70b1901..master.mem_percent\",\n" +
            "                    \"metricValue\": 0.0639,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.mem_total\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.61b5f554-c44c-4dcc-8373-3dade70b1901..master.mem_total\",\n" +
            "                    \"metricValue\": 320265,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.mem_used\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.61b5f554-c44c-4dcc-8373-3dade70b1901..master.mem_used\",\n" +
            "                    \"metricValue\": 20480,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.slaves_active\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.61b5f554-c44c-4dcc-8373-3dade70b1901..master.slaves_active\",\n" +
            "                    \"metricValue\": 2,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.tasks_lost\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.61b5f554-c44c-4dcc-8373-3dade70b1901..master.tasks_lost\",\n" +
            "                    \"metricValue\": 0,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"master.uptime_secs\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.61b5f554-c44c-4dcc-8373-3dade70b1901..master.uptime_secs\",\n" +
            "                    \"metricValue\": 9012352,\n" +
            "                    \"metricTime\": \"03-09-201916:00:08\",\n" +
            "                    \"metricUnit\": \"secs\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  }\n" +
            "                ]\n" +
            "              }\n" +
            "            ]\n" +
            "          },\n" +
            "          {\n" +
            "            \"machineName\": \"8922daf1-1a53-41b4-9f18-7ed536b44f20\",\n" +
            "            \"ip\": \"127.0.0.1\",\n" +
            "            \"serviceCategory\": \"eu.indigo-datacloud.marathon\",\n" +
            "            \"serviceId\": \"IaaS_provider-UPV-GRyCAP_8922daf1-1a53-41b4-9f18-7ed536b44f20\",\n" +
            "            \"services\": [\n" +
            "              {\n" +
            "                \"serviceName\": \"\",\n" +
            "                \"paasMetrics\": [\n" +
            "                  {\n" +
            "                    \"metricName\": \"clear_responseTime\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.8922daf1-1a53-41b4-9f18-7ed536b44f20..clear_responseTime\",\n" +
            "                    \"metricValue\": 132,\n" +
            "                    \"metricTime\": \"03-09-201915:30:08\",\n" +
            "                    \"metricUnit\": \"ms\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"clear_result\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.8922daf1-1a53-41b4-9f18-7ed536b44f20..clear_result\",\n" +
            "                    \"metricValue\": 1,\n" +
            "                    \"metricTime\": \"03-09-201915:30:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"clear_status\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.8922daf1-1a53-41b4-9f18-7ed536b44f20..clear_status\",\n" +
            "                    \"metricValue\": 404,\n" +
            "                    \"metricTime\": \"03-09-201915:30:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"create_responseTime\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.8922daf1-1a53-41b4-9f18-7ed536b44f20..create_responseTime\",\n" +
            "                    \"metricValue\": 110,\n" +
            "                    \"metricTime\": \"03-09-201915:30:08\",\n" +
            "                    \"metricUnit\": \"ms\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"create_result\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.8922daf1-1a53-41b4-9f18-7ed536b44f20..create_result\",\n" +
            "                    \"metricValue\": 1,\n" +
            "                    \"metricTime\": \"03-09-201915:30:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"create_status\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.8922daf1-1a53-41b4-9f18-7ed536b44f20..create_status\",\n" +
            "                    \"metricValue\": 200,\n" +
            "                    \"metricTime\": \"03-09-201915:30:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"delete_responseTime\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.8922daf1-1a53-41b4-9f18-7ed536b44f20..delete_responseTime\",\n" +
            "                    \"metricValue\": 51,\n" +
            "                    \"metricTime\": \"03-09-201915:30:08\",\n" +
            "                    \"metricUnit\": \"ms\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"delete_result\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.8922daf1-1a53-41b4-9f18-7ed536b44f20..delete_result\",\n" +
            "                    \"metricValue\": 1,\n" +
            "                    \"metricTime\": \"03-09-201915:30:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"delete_status\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.8922daf1-1a53-41b4-9f18-7ed536b44f20..delete_status\",\n" +
            "                    \"metricValue\": 200,\n" +
            "                    \"metricTime\": \"03-09-201915:30:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"run_responseTime\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.8922daf1-1a53-41b4-9f18-7ed536b44f20..run_responseTime\",\n" +
            "                    \"metricValue\": 20,\n" +
            "                    \"metricTime\": \"03-09-201915:30:08\",\n" +
            "                    \"metricUnit\": \"ms\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"run_result\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.8922daf1-1a53-41b4-9f18-7ed536b44f20..run_result\",\n" +
            "                    \"metricValue\": 1,\n" +
            "                    \"metricTime\": \"03-09-201915:30:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"run_status\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.8922daf1-1a53-41b4-9f18-7ed536b44f20..run_status\",\n" +
            "                    \"metricValue\": 200,\n" +
            "                    \"metricTime\": \"03-09-201915:30:08\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  }\n" +
            "                ]\n" +
            "              }\n" +
            "            ]\n" +
            "          },\n" +
            "          {\n" +
            "            \"machineName\": \"8490545c-f5ec-4330-9116-604dc0084caa\",\n" +
            "            \"ip\": \"127.0.0.1\",\n" +
            "            \"serviceCategory\": \"eu.indigo-datacloud.chronos\",\n" +
            "            \"serviceId\": \"IaaS_provider-UPV-GRyCAP_8490545c-f5ec-4330-9116-604dc0084caa\",\n" +
            "            \"services\": [\n" +
            "              {\n" +
            "                \"serviceName\": \"\",\n" +
            "                \"paasMetrics\": [\n" +
            "                  {\n" +
            "                    \"metricName\": \"clear_responseTime\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.8490545c-f5ec-4330-9116-604dc0084caa..clear_responseTime\",\n" +
            "                    \"metricValue\": 200,\n" +
            "                    \"metricTime\": \"03-09-201916:15:03\",\n" +
            "                    \"metricUnit\": \"ms\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"clear_result\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.8490545c-f5ec-4330-9116-604dc0084caa..clear_result\",\n" +
            "                    \"metricValue\": 1,\n" +
            "                    \"metricTime\": \"03-09-201916:15:03\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"clear_status\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.8490545c-f5ec-4330-9116-604dc0084caa..clear_status\",\n" +
            "                    \"metricValue\": 404,\n" +
            "                    \"metricTime\": \"03-09-201916:15:03\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"create_responseTime\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.8490545c-f5ec-4330-9116-604dc0084caa..create_responseTime\",\n" +
            "                    \"metricValue\": 110,\n" +
            "                    \"metricTime\": \"03-09-201916:15:03\",\n" +
            "                    \"metricUnit\": \"ms\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"create_result\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.8490545c-f5ec-4330-9116-604dc0084caa..create_result\",\n" +
            "                    \"metricValue\": 1,\n" +
            "                    \"metricTime\": \"03-09-201916:15:03\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"create_status\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.8490545c-f5ec-4330-9116-604dc0084caa..create_status\",\n" +
            "                    \"metricValue\": 200,\n" +
            "                    \"metricTime\": \"03-09-201916:15:03\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"delete_responseTime\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.8490545c-f5ec-4330-9116-604dc0084caa..delete_responseTime\",\n" +
            "                    \"metricValue\": 15,\n" +
            "                    \"metricTime\": \"03-09-201916:15:03\",\n" +
            "                    \"metricUnit\": \"ms\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"delete_result\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.8490545c-f5ec-4330-9116-604dc0084caa..delete_result\",\n" +
            "                    \"metricValue\": 1,\n" +
            "                    \"metricTime\": \"03-09-201916:15:03\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"delete_status\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.8490545c-f5ec-4330-9116-604dc0084caa..delete_status\",\n" +
            "                    \"metricValue\": 200,\n" +
            "                    \"metricTime\": \"03-09-201916:15:03\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"run_responseTime\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.8490545c-f5ec-4330-9116-604dc0084caa..run_responseTime\",\n" +
            "                    \"metricValue\": 13,\n" +
            "                    \"metricTime\": \"03-09-201916:15:03\",\n" +
            "                    \"metricUnit\": \"ms\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"run_result\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.8490545c-f5ec-4330-9116-604dc0084caa..run_result\",\n" +
            "                    \"metricValue\": 1,\n" +
            "                    \"metricTime\": \"03-09-201916:15:03\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"metricName\": \"run_status\",\n" +
            "                    \"metricKey\": \"provider-UPV-GRyCAP.8490545c-f5ec-4330-9116-604dc0084caa..run_status\",\n" +
            "                    \"metricValue\": 200,\n" +
            "                    \"metricTime\": \"03-09-201916:15:03\",\n" +
            "                    \"metricUnit\": \"\",\n" +
            "                    \"paasThresholds\": [],\n" +
            "                    \"historyClocks\": [],\n" +
            "                    \"historyValues\": []\n" +
            "                  }\n" +
            "                ]\n" +
            "              }\n" +
            "            ]\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  \"additionalProperties\": {}\n" +
            "}",
            MediaType.APPLICATION_JSON));
  }

}
