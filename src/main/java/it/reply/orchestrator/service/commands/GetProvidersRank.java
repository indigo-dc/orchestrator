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

import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.ranker.CloudProviderRankerRequest;
import it.reply.orchestrator.dto.ranker.Monitoring;
import it.reply.orchestrator.dto.ranker.RankedCloudProvider;
import it.reply.orchestrator.dto.slam.Preference;
import it.reply.orchestrator.dto.slam.PreferenceCustomer;
import it.reply.orchestrator.service.CloudProviderRankerService;
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
        .map(e -> Monitoring.builder().provider(e.getKey()).metrics(e.getValue()).build())
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
    List<RankedCloudProvider> ranking = cloudProviderRankerService.getProviderRanking(cprr);
    rankCloudProvidersMessage.setRankedCloudProviders(ranking);
  }

  @Override
  protected String getErrorMessagePrefix() {
    return "Error retrieving info from Cloud Provider Ranker";
  }
}
