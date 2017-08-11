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

import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.utils.WorkflowConstants;

/**
 * Base behavior for all RankCloudProvider WF tasks. <br/>
 * This checks input parameters and manages output and errors (specifically, in case of errors, it
 * also updates directly the deployment status on DB).
 * 
 * @author l.biava
 *
 */
public abstract class BaseRankCloudProvidersCommand<T extends BaseRankCloudProvidersCommand<T>>
    extends BaseWorkflowCommand<RankCloudProvidersMessage, T> {

  @Override
  protected String getMessageParameterName() {
    return WorkflowConstants.WF_PARAM_RANK_CLOUD_PROVIDERS_MESSAGE;
  }
}
