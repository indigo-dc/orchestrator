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
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.service.OneDataService;
import it.reply.orchestrator.utils.CommonUtils;
import it.reply.orchestrator.utils.WorkflowConstants;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component(WorkflowConstants.Delegate.GET_ONEDATA_DATA)
public class GetOneDataData extends BaseRankCloudProvidersCommand {

  @Autowired
  private OneDataService oneDataService;

  @Override
  public void execute(DelegateExecution execution,
      RankCloudProvidersMessage rankCloudProvidersMessage) {

    Map<String, OneData> oneDataRequirements = rankCloudProvidersMessage.getOneDataRequirements();

    CommonUtils
        .getFromOptionalMap(oneDataRequirements, "input")
        .ifPresent(oneDataService::populateProviderInfo);

    CommonUtils
        .getFromOptionalMap(oneDataRequirements, "output")
        .ifPresent(oneDataService::populateProviderInfo);
  }

  @Override
  protected String getErrorMessagePrefix() {
    return "Error retrieving info from OneData";
  }
}
