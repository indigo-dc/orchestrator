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

import it.reply.monitoringpillar.domain.dsl.monitoring.pillar.wrapper.paas.PaaSMetric;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.Type;
import it.reply.orchestrator.service.MonitoringService;

import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class GetMonitoringData extends BaseRankCloudProvidersCommand<GetMonitoringData> {

  @Autowired
  private MonitoringService monitoringService;

  @Override
  @Transactional
  public ExecutionResults customExecute(CommandContext ctx,
      RankCloudProvidersMessage rankCloudProvidersMessage) {

    // Get monitoring data for each Cloud Provider
    rankCloudProvidersMessage
        .getCloudProviders()
        .forEach((cloudProviderId, cloudProvider) -> {

          List<CloudService> computeServices =
              cloudProvider.getCmbdProviderServicesByType(Type.COMPUTE);
          // TODO use cloudService field
          boolean isPublicCloud = computeServices
              .stream()
              .parallel()
              .unordered()
              .anyMatch(service -> service.getData().isPublicService());
          if (!isPublicCloud) {
            List<PaaSMetric> metrics = monitoringService.getProviderData(cloudProviderId);
            rankCloudProvidersMessage
                .getCloudProvidersMonitoringData()
                .put(cloudProviderId, metrics);
          }
        });
    return resultOccurred(rankCloudProvidersMessage.getCloudProvidersMonitoringData());
  }

  @Override
  protected String getErrorMessagePrefix() {
    return "Error retrieving info from monitoring service";
  }
}
