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

package it.reply.orchestrator.dto.cmdb;

import com.fasterxml.jackson.annotation.JsonProperty;

import it.reply.orchestrator.dto.cmdb.MarathonService.MarathonServiceProperties;
import it.reply.orchestrator.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarathonService extends MesosFrameworkService<MarathonServiceProperties> {

  /**
   * Create a new MarathonService.
   * */

  @Builder(builderMethodName = "marathonBuilder")
  public MarathonService(
      @NonNull String id,
      @NonNull String serviceType,
      @NonNull String endpoint,
      @Nullable String microversion,
      @NonNull String providerId,
      @NonNull CloudServiceType type,
      boolean publicService,
      @Nullable String tenant,
      @Nullable String region,
      @NonNull String hostname,
      @Nullable String parentServiceId,
      boolean iamEnabled,
      @NonNull String idpProtocol,
      boolean publicIpAssignable,
      @NonNull List<SupportedIdp> supportedIdps,
      @NonNull MarathonServiceProperties properties) {
    super(id, serviceType, endpoint, microversion, providerId, type, publicService,
          tenant, region, hostname, parentServiceId, iamEnabled, idpProtocol,
          publicIpAssignable, supportedIdps, properties);
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class MarathonServiceProperties extends MesosFrameworkServiceProperties {

    @NonNull
    @NotNull
    @JsonProperty("load_balancer_ips")
    private List<String> loadBalancerIps = new ArrayList<>();

    @JsonProperty("secrets_support")
    private boolean secretSupport;

    @Builder
    protected MarathonServiceProperties(
        @Nullable String localVolumesHostBasePath,
        boolean gpuSupport,
        @NonNull List<String> persistentStorageDrivers,
        @NonNull List<String> loadBalancerIps) {
      super(localVolumesHostBasePath, gpuSupport,
          CommonUtils.notNullOrDefaultValue(persistentStorageDrivers, ArrayList::new));
      this.loadBalancerIps = CommonUtils.notNullOrDefaultValue(loadBalancerIps, ArrayList::new);
    }

    @Deprecated
    protected MarathonServiceProperties() {
      loadBalancerIps = new ArrayList<>();
    }

  }
}
