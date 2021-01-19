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

package it.reply.orchestrator.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class WorkflowConstants {

  @UtilityClass
  public static class Param {
    public static final String DEPLOYMENT_ID = "DEPLOYMENT_ID";
    public static final String REQUEST_ID = "REQUEST_ID";
    public static final String DEPLOYMENT_MESSAGE = "DeploymentMessage";
    public static final String RANK_CLOUD_PROVIDERS_MESSAGE = "RankCloudProvidersMessage";
    public static final String TOSCA_TEMPLATE = "TOSCA_TEMPLATE";
    public static final String EXCEPTION = "exception";
  }

  @UtilityClass
  public static class ErrorCode {

    public static final String CLOUD_PROVIDER_ERROR = "CloudProviderError";
    public static final String RUNTIME_ERROR = "RuntimeError";
  }

  @UtilityClass
  public static class Process {
    public static final String DEPLOY = "Deploy";
    public static final String UNDEPLOY = "Undeploy";
    public static final String UPDATE = "Update";
    public static final String RANK_CLOUD_PROVIDERS = "RankCloudProviders";
    public static final String MOVE_DATA_AND_DEPLOY = "Move Data and Deploy";
  }

  @UtilityClass
  public static class Delegate {
    public static final String UPDATE_DEPLOYMENT = "updateDeployment";
    public static final String UPDATE = "update";
    public static final String UNDEPLOY = "undeploy";
    public static final String PREFILTER_CLOUD_PROVIDERS = "prefilterCloudProviders";
    public static final String POLL_UNDEPLOY = "pollUndeploy";
    public static final String POLL_DEPLOY = "pollDeploy";
    public static final String NOTIFY = "notify";
    public static final String HANDLE_ERROR = "handleError";
    public static final String HANDLE_TIMEOUT = "handleTimeout";
    public static final String GET_SLAM = "getSlam";
    public static final String GET_PROVIDERS_RANK = "getProvidersRank";
    public static final String GET_ONEDATA_DATA = "getOneDataData";
    public static final String GET_DYNAFED_DATA = "getDynafedData";
    public static final String GET_MONITORING_DATA = "getMonitoringData";
    public static final String GET_CMDB_DATA_UPDATE = "getCmdbDataUpdate";
    public static final String GET_CMDB_DATA_DEPLOY = "getCmdbDataDeploy";
    public static final String FINALIZE_UNDEPLOY = "finalizeUndeploy";
    public static final String FINALIZE_DEPLOY = "finalizeDeploy";
    public static final String DEPLOY = "deploy";
    public static final String CLEAN_FAILED_DEPLOY = "cleanFailedDeploy";
    public static final String CLEAN_FAILED_UPDATE = "cleanFailedUpdate";
    public static final String PROVIDER_TIMEOUT = "providerTimeout";
    public static final String CREATE_MAIN_REPLICATION_RULE = "createMainReplicationRule";
    public static final String CREATE_TEMP_REPLICATION_RULE = "createTempReplicationRule";
    public static final String CHECK_MAIN_REPLICATION_RULE = "checkMainReplicationRule";
    public static final String CHECK_TEMP_REPLICATION_RULE = "checkTempReplicationRule";
    public static final String DELETE_TEMP_REPLICATION_RULE = "deleteTempReplicationRule";
  }
}
