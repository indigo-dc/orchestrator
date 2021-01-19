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

package it.reply.orchestrator.util;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.OidcEntity;
import it.reply.orchestrator.dal.entity.OidcEntityId;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.CloudProviderEndpoint.IaaSType;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.onedata.OneData.OneDataProviderInfo;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TestUtil {

  public static String getFileContentAsString(String fileUri) throws IOException {
    return FileUtils.readFileToString(new File(fileUri), Charsets.UTF_8);
  }

  public static DeploymentMessage generateDeployDm(Deployment deployment) {
    DeploymentMessage dm = new DeploymentMessage();
    dm.setDeploymentId(deployment.getId());
    CloudProviderEndpoint chosenCloudProviderEndpoint = CloudProviderEndpoint
        .builder()
        .cpComputeServiceId(UUID.randomUUID().toString())
        .cpEndpoint("http://example.com")
        .iaasType(IaaSType.OPENSTACK)
        .build();
    dm.setChosenCloudProviderEndpoint(chosenCloudProviderEndpoint);
    deployment.setCloudProviderEndpoint(chosenCloudProviderEndpoint);
    Map<String, OneData> oneDataParameters = new HashMap<>();
    OneDataProviderInfo providerInfo = OneDataProviderInfo
        .builder()
        .cloudProviderId("provider-1")
        .cloudServiceId(UUID.randomUUID().toString())
        .endpoint("http://example.onedata.com")
        .id("test")
        .build();
    List<OneDataProviderInfo> oneproviders = new ArrayList<OneDataProviderInfo>();
    oneproviders.add(providerInfo);
    OneData parameter = OneData
        .builder()
        .oneproviders(oneproviders)
        .onezone("test")
        .path("/tmp/")
        .selectedOneprovider(providerInfo)
        .serviceSpace(true)
        .smartScheduling(false)
        .space("test")
        .token("0123456789-onedata-token")
        .build();
    oneDataParameters.put("provider-1", parameter);
    dm.setOneDataParameters(oneDataParameters);
    OidcEntity oe = new OidcEntity();
    OidcEntityId oeid = new OidcEntityId();
    oeid.setIssuer("https://iam-test.com");
    oeid.setSubject("0123456789-subject");
    oe.setOidcEntityId(oeid);
    deployment.setOwner(oe);
    return dm;
  }

}
