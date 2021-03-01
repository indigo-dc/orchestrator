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

package it.reply.orchestrator.resource;

import com.fasterxml.jackson.annotation.JsonInclude;

import it.reply.orchestrator.dto.CloudProviderEndpoint.IaaSType;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import org.checkerframework.checker.nullness.qual.NonNull;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class CloudProviderEndpointResource {

  @NonNull
  @NotNull
  private String cpEndpoint;

  @NonNull
  @NotNull
  private String cpComputeServiceId;

  private String vaultUri;

  @NonNull
  @NotNull
  private IaaSType deploymentType;

  @NonNull
  @NotNull
  @Builder.Default
  private Map<String, CloudProviderEndpointResource> hybridCloudProviderEndpoints = new HashMap<>();

}
