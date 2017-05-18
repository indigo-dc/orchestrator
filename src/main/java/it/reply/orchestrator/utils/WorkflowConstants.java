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

package it.reply.orchestrator.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class WorkflowConstants {
  public static final String WF_PARAM_DEPLOYMENT_ID = "DEPLOYMENT_ID";
  public static final String WF_PARAM_DEPLOYMENT_MESSAGE = "DeploymentMessage";
  public static final String WF_PARAM_RANK_CLOUD_PROVIDERS_MESSAGE = "RankCloudProvidersMessage";
  public static final String WF_PARAM_TOSCA_TEMPLATE = "TOSCA_TEMPLATE";
  public static final String WF_PARAM_LOGGER = "logger";
  public static final String WF_PARAM_POLLING_STATUS = "pollingStatus";

}
