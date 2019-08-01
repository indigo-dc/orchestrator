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

package it.reply.orchestrator.dto.deployment;

import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dto.dynafed.Dynafed;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.policies.ToscaPolicy;
import it.reply.orchestrator.enums.DeploymentType;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Tolerate;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Data
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BaseWorkflowMessage {

  @Nullable
  private OidcTokenId requestedWithToken;

  @NonNull
  @NotNull
  private String deploymentId;

  @NonNull
  @NotNull
  private DeploymentType deploymentType;

  private boolean hybrid;

  @NonNull
  private Map<String, OneData> oneDataRequirements = new HashMap<>();

  @NonNull
  private Map<String, Dynafed> dynafedRequirements = new HashMap<>();

  @NonNull
  private Map<String, ToscaPolicy> placementPolicies = new HashMap<>();

  /**
   * Creates a new BaseWorkflowMessage coping the fields from another one.
   * 
   * @param other
   *          the other BaseWorkflowMessage
   */
  @Tolerate
  public BaseWorkflowMessage(BaseWorkflowMessage other) {
    requestedWithToken = other.requestedWithToken;
    deploymentId = other.deploymentId;
    deploymentType = other.deploymentType;
    hybrid = other.hybrid;
    oneDataRequirements = other.oneDataRequirements;
    dynafedRequirements = other.dynafedRequirements;
    placementPolicies = other.placementPolicies;
  }
}
