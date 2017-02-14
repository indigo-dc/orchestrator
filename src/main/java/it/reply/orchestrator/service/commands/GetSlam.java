package it.reply.orchestrator.service.commands;

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

import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.slam.Service;
import it.reply.orchestrator.dto.slam.Sla;
import it.reply.orchestrator.service.SlamService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GetSlam extends BaseRankCloudProvidersCommand {

  @Autowired
  private SlamService slamService;

  @Override
  protected RankCloudProvidersMessage customExecute(
      RankCloudProvidersMessage rankCloudProvidersMessage) {
    rankCloudProvidersMessage.setSlamPreferences(slamService.getCustomerPreferences());

    // Get VO (customer) preferences and SLAs (infer available Cloud Providers from it)
    for (Sla sla : rankCloudProvidersMessage.getSlamPreferences().getSla()) {
      // Create Cloud Provider, add to the list
      CloudProvider cp =
          rankCloudProvidersMessage.getCloudProviders().get(sla.getCloudProviderId());
      if (cp == null) {
        cp = new CloudProvider(sla.getCloudProviderId());
        rankCloudProvidersMessage.getCloudProviders().put(sla.getCloudProviderId(), cp);
      }

      // Get provider's services
      for (Service service : sla.getServices()) {
        cp.getCmdbProviderServices().put(service.getServiceId(), null);
      }
    }

    return rankCloudProvidersMessage;
  }

  @Override
  protected String getErrorMessagePrefix() {
    return "Error retrieving SLAs from SLAM";
  }
}
