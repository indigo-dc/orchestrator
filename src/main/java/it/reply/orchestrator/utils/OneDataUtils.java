/*
 * Copyright © 2020 I.N.F.N.
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

package it.reply.orchestrator.utils;

import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.service.IndigoInputsPreProcessorService.RuntimeProperties;

import java.util.Map;

public class OneDataUtils {

  private OneDataUtils() {    
  }

  /**
   * Extract OneData parameters from DeploymentMessage.
   *
   * @param deploymentMessage the DeploymentMessageObject
   * @return oneData parameters as RuntimeProperties
   */
  public static RuntimeProperties getOneDataRuntimeProperties(DeploymentMessage deploymentMessage) {
    Map<String, OneData> odParameters = deploymentMessage.getOneDataParameters();
    RuntimeProperties runtimeProperties = new RuntimeProperties();
    odParameters.forEach((nodeName, odParameter) -> {
      runtimeProperties.put(odParameter.getOnezone(), nodeName, "onezone");
      runtimeProperties.put(odParameter.getToken(), nodeName, "token");
      runtimeProperties
        .put(odParameter.getSelectedOneprovider().getEndpoint(), nodeName, "selected_provider");
      if (odParameter.isServiceSpace()) {
        runtimeProperties.put(odParameter.getSpace(), nodeName, "space");
        runtimeProperties.put(odParameter.getPath(), nodeName, "path");
      }
    });
    return runtimeProperties;
  }
}
