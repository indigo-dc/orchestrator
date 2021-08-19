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

package it.reply.orchestrator.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@JsonIgnoreProperties(ignoreUnknown = false)
public class DeploymentScheduleRequest extends DeploymentRequest {

  @NotNull(message = "The expression to be used to monitor new files available")
  @NonNull
  private String fileExpression;

  @NotNull(message = "The expression to be used to replicate the files")
  @NonNull
  private String replicationExpression;

  @NotNull(message = "The number of file replicas required")
  @NonNull
  private Integer numberOfReplicas;

  /**
   * Create a new DeploymentScheduleRequest.
   * @param template the template
   * @param parameters the parameters
   * @param callback the callback
   * @param timeoutMins the global timeout in mins
   * @param providerTimeoutMins the provider timeout in mins
   * @param maxProvidersRetry the max num of retries
   * @param keepLastAttempt whether to keep the last attempt or not
   * @param fileExpression the file expression
   * @param replicationExpression the replication expression
   * @param numberOfReplicas the number of replicas
   * @param group the user group 
   */
  @Builder(builderMethodName = "deploymentScheduleBuilder")
  public DeploymentScheduleRequest(@NonNull String template,
      @NonNull Map<String, Object> parameters, @Nullable String callback,
      @Nullable Integer timeoutMins, @Nullable Integer providerTimeoutMins,
      @Nullable Integer maxProvidersRetry, boolean keepLastAttempt, @NonNull String fileExpression,
      @NonNull String replicationExpression, @NonNull Integer numberOfReplicas,
      @Nullable String group) {
    super(template, parameters, callback, timeoutMins, providerTimeoutMins, maxProvidersRetry,
        keepLastAttempt, group);
    this.fileExpression = fileExpression;
    this.replicationExpression = replicationExpression;
    this.numberOfReplicas = numberOfReplicas;
  }

}
