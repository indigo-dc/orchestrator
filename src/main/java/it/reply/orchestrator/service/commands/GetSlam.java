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

package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.service.SlamService;
import it.reply.orchestrator.utils.WorkflowConstants;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(WorkflowConstants.Delegate.GET_SLAM)
public class GetSlam extends BaseRankCloudProvidersCommand {

  @Autowired
  private SlamService slamService;

  @Override
  public void execute(DelegateExecution execution,
      RankCloudProvidersMessage rankCloudProvidersMessage) {
    rankCloudProvidersMessage.setSlamPreferences(
        slamService.getCustomerPreferences(rankCloudProvidersMessage.getRequestedWithToken()));

    // Get VO (customer) preferences and SLAs (infer available Cloud Providers from it)
    rankCloudProvidersMessage
        .getSlamPreferences()
        .getSla()
        .forEach(sla -> {
          // Create Cloud Provider, add to the list
          CloudProvider cp = rankCloudProvidersMessage
              .getCloudProviders()
              .computeIfAbsent(sla.getCloudProviderId(),
                  cloudProviderId -> CloudProvider.builder().id(cloudProviderId).build());

          // Get provider's services
          sla
              .getServices()
              .forEach(
                  service -> cp
                      .getCmdbProviderServices()
                      .put(service.getServiceId(), null));
        });
  }

  @Override
  protected String getErrorMessagePrefix() {
    return "Error retrieving SLAs from SLAM";
  }
}
