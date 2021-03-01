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

import it.reply.monitoringpillar.domain.dsl.monitoring.pillar.wrapper.paas.PaaSMetric;
import it.reply.monitoringpillar.domain.dsl.monitoring.pillar.wrapper.paas.PaasMachine;
import it.reply.monitoringpillar.domain.dsl.monitoring.pillar.wrapper.paas.Service;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.cmdb.CloudProvider;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.ranker.CloudProviderRankerRequest;
import it.reply.orchestrator.dto.ranker.Monitoring;
import it.reply.orchestrator.dto.ranker.MonitoringService;
import it.reply.orchestrator.dto.ranker.RankedCloudService;
import it.reply.orchestrator.dto.slam.Preference;
import it.reply.orchestrator.dto.slam.PreferenceCustomer;
import it.reply.orchestrator.service.CloudProviderRankerService;
import it.reply.orchestrator.utils.CommonUtils;
import it.reply.orchestrator.utils.WorkflowConstants;

import java.util.List;
import java.util.stream.Collectors;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(WorkflowConstants.Delegate.GET_PROVIDERS_RANK)
public class GetProvidersRank extends BaseRankCloudProvidersCommand {

  @Autowired
  private CloudProviderRankerService cloudProviderRankerService;

  @Override
  public void execute(DelegateExecution execution,
      RankCloudProvidersMessage rankCloudProvidersMessage) {

    // Prepare Ranker's request
    List<Monitoring> monitoring = rankCloudProvidersMessage
        .getCloudProvidersMonitoringData()
        .entrySet()
        .stream()
        .map(e -> generateMonitoringInfo(rankCloudProvidersMessage, e.getKey(), e.getValue()))
        .collect(Collectors.toList());

    List<PreferenceCustomer> preferences = rankCloudProvidersMessage
        .getSlamPreferences()
        .getPreferences()
        .stream()
        .map(Preference::getPreferences)
        .flatMap(List::stream)
        .collect(Collectors.toList());

    CloudProviderRankerRequest cprr = CloudProviderRankerRequest
        .builder()
        .preferences(preferences)
        .sla(rankCloudProvidersMessage.getSlamPreferences().getSla())
        .monitoring(monitoring)
        .build();

    // Get provider rank and save in message
    List<RankedCloudService> ranking = cloudProviderRankerService.getProviderServicesRanking(cprr);
    rankCloudProvidersMessage.setRankedCloudServices(ranking);
  }

  private Monitoring generateMonitoringInfo(RankCloudProvidersMessage rankCloudProvidersMessage,
      String providerId, List<PaasMachine> paasMachines) {
    List<MonitoringService> monitoringServices = paasMachines
        .stream()
        .map(paasMachine -> generateMonitoringService(rankCloudProvidersMessage, providerId,
            paasMachine))
        .collect(Collectors.toList());
    return Monitoring
        .builder()
        .provider(providerId)
        .services(monitoringServices)
        .build();
  }

  private MonitoringService generateMonitoringService(
      RankCloudProvidersMessage rankCloudProvidersMessage, String providerId,
      PaasMachine paasMachine) {

    String serviceType = paasMachine.getServiceCategory();
    String serviceId = paasMachine.getMachineName();
    String parentServiceId = CommonUtils
        .getFromOptionalMap(rankCloudProvidersMessage.getCloudProviders(), providerId)
        .map(CloudProvider::getServices)
        .map(cloudServices -> cloudServices.get(serviceId))
        .map(CloudService::getParentServiceId)
        .orElse(null);
    List<PaaSMetric> metrics = paasMachine
        .getServices()
        .stream()
        .map(Service::getPaasMetrics)
        .flatMap(List::stream)
        .collect(Collectors.toList());
    return MonitoringService
        .builder()
        .serviceId(serviceId)
        .parentServiceId(parentServiceId)
        .type(serviceType)
        .metrics(metrics)
        .build();
  }

  @Override
  protected String getErrorMessagePrefix() {
    return "Error retrieving info from Cloud Provider Ranker";
  }
}
