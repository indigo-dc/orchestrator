/*
 * Copyright © 2015-2018 Santer Reply S.p.A.
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class TestUtil {

  /**
   * Convert Object To a Json byte array.
   * 
   * @return byte[]
   */
  public static byte[] convertObjectToJsonBytes(Object object) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    return mapper.writeValueAsBytes(object);
  }

  public static String getFileContentAsString(String fileUri) throws IOException {
    return FileUtils.readFileToString(new File(fileUri), Charsets.UTF_8);
  }
  
  public static DeploymentMessage generateDeployDm(Deployment deployment) {
    DeploymentMessage dm = new DeploymentMessage();
    dm.setDeploymentId(deployment.getId());
    CloudProviderEndpoint chosenCloudProviderEndpoint = new CloudProviderEndpoint();
    chosenCloudProviderEndpoint.setCpComputeServiceId(UUID.randomUUID().toString());
    dm.setChosenCloudProviderEndpoint(chosenCloudProviderEndpoint);
    deployment.setCloudProviderEndpoint(chosenCloudProviderEndpoint);
    return dm;
  }

}
