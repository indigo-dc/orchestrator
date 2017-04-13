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

package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.service.CmdbService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GetCmdbDataDeploy extends BaseRankCloudProvidersCommand {

  private static final Logger LOG = LoggerFactory.getLogger(GetCmdbDataDeploy.class);

  @Autowired
  private CmdbService cmdbService;

  @Override
  protected RankCloudProvidersMessage customExecute(
      RankCloudProvidersMessage rankCloudProvidersMessage) {

    // Get CMDB data for each Cloud Provider
    for (Map.Entry<String, CloudProvider> providerEntry : rankCloudProvidersMessage
        .getCloudProviders().entrySet()) {
      CloudProvider cp = providerEntry.getValue();
      cp = cmdbService.fillCloudProviderInfo(cp);
    }

    return rankCloudProvidersMessage;
  }

  @Override
  protected String getErrorMessagePrefix() {
    return "Error retrieving info from CMDB";
  }

}
