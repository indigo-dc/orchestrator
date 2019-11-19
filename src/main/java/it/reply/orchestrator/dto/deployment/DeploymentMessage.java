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

import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.workflow.CloudServicesOrderedIterator;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.checkerframework.checker.nullness.qual.NonNull;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DeploymentMessage extends BaseWorkflowMessage {

  // Max value allowed by SQL
  // private static final Instant MAX_TIMEOUT = Instant.parse("9999-12-31T23:59:59.999Z");
  private static final Instant MAX_TIMEOUT = Instant.parse("2038-01-19T03:14:07.999Z");

  @NonNull
  @NotNull
  private String timeout = MAX_TIMEOUT.toString();

  /**
   * Sets the Deployment timeout.
   *
   * @param timeoutMins
   *          the timeout in Minutes
   */
  public void setTimeoutInMins(Integer timeoutMins) {
    this.timeout = Optional
        .ofNullable(timeoutMins)
        .map(value -> Instant.now().plus(Duration.ofMinutes(value)))
        .filter(value -> value.isBefore(MAX_TIMEOUT))
        .orElse(MAX_TIMEOUT)
        .toString();
  }

  @Nullable
  private QcgJobsOrderedIterator qcgJobsIterator;

  @Nullable
  private ChronosJobsOrderedIterator chronosJobsIterator;

  private boolean createComplete;
  private boolean deleteComplete;
  private boolean pollComplete;
  private boolean skipPollInterval;

  @NonNull
  @NotNull
  private String providerTimeout = MAX_TIMEOUT.toString();

  /**
   * Sets the Deployment per provider timeout.
   *
   * @param timeoutMins
   *          the timeout in Minutes
   */
  public void setProviderTimeoutInMins(Integer timeoutMins) {
    this.providerTimeout = Optional
        .ofNullable(timeoutMins)
        .map(value -> Instant.now().plus(Duration.ofMinutes(value)))
        .filter(value -> value.isBefore(MAX_TIMEOUT))
        .orElse(MAX_TIMEOUT)
        .toString();
  }

  @Nullable
  private CloudServicesOrderedIterator cloudServicesOrderedIterator;

  @Nullable
  private CloudProviderEndpoint chosenCloudProviderEndpoint;

  /**
   * The OneData information generated after the best provider choice.
   */
  @NonNull
  private Map<String, OneData> oneDataParameters = new HashMap<>();

  @Nullable
  private Integer maxProvidersRetry;

  private boolean keepLastAttempt = false;

}
