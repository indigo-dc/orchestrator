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

package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.cmdb.CloudProvider;
import it.reply.orchestrator.dto.slam.Service;
import it.reply.orchestrator.service.CmdbService;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.orchestrator.utils.WorkflowConstants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(WorkflowConstants.Delegate.GET_CMDB_DATA_DEPLOY)
public class GetCmdbDataDeploy extends BaseRankCloudProvidersCommand {

  @Autowired
  private CmdbService cmdbService;

  @Autowired
  private OAuth2TokenService oauth2TokenService;

  @Override
  public void execute(DelegateExecution execution,
      RankCloudProvidersMessage rankCloudProvidersMessage) {

    Map<String, Set<String>> servicesWithSla = new HashMap<>();

    String organisation = oauth2TokenService.getOrganization(
        rankCloudProvidersMessage.getRequestedWithToken());

    rankCloudProvidersMessage
        .getSlamPreferences()
        .getSla()
        .forEach(sla -> {
          String providerId = sla.getCloudProviderId();
          servicesWithSla
              .computeIfAbsent(providerId, id -> new HashSet<>())
              .addAll(sla
                  .getServices()
                  .stream()
                  .map(Service::getServiceId)
                  .collect(Collectors.toSet()));
        });
    Map<String, CloudProvider> cloudProviders = servicesWithSla
        .entrySet()
        .stream()
        .map(entry -> cmdbService.fillCloudProviderInfo(entry.getKey(),
            entry.getValue(), organisation, false))
        .collect(Collectors.toMap(CloudProvider::getId, Function.identity()));
    rankCloudProvidersMessage.setCloudProviders(cloudProviders);

  }

  @Override
  protected String getErrorMessagePrefix() {
    return "Error retrieving info from CMDB";
  }

}
