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
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StorageService extends CloudService {

  @JsonProperty("rucio_rse")
  @Nullable
  private String rucioRse;

  /**
   * Generate a new StorageService.
   *
   * @param id
   *     the id
   * @param serviceType
   *     the serviceType
   * @param endpoint
   *     the endpoint
   * @param providerId
   *     the providerId
   * @param type
   *     the type
   * @param publicService
   *     the publicService
   * @param region
   *     the region
   * @param hostname
   *     the hostname
   * @param parentServiceId
   *     the parent service Id
   * @param rucioRse
   *     the optional Rucio RSE name
   */
  @Builder(builderMethodName = "storageBuilder")
  public StorageService(
      @NonNull String id,
      @NonNull String serviceType,
      @NonNull String endpoint,
      @NonNull String providerId,
      @NonNull CloudServiceType type,
      boolean publicService,
      @Nullable String tenant,
      @Nullable String region,
      @NonNull String hostname,
      @Nullable String parentServiceId,
      @Nullable String rucioRse,
      boolean iamEnabled,
      @Nullable String idpProtocol,
      boolean publicIpAssignable,
      @NonNull List<SupportedIdp> supportedIdps) {
    super(id, serviceType, endpoint, providerId, type, publicService, region, tenant, hostname,
            parentServiceId, iamEnabled, idpProtocol, publicIpAssignable, supportedIdps);
    this.rucioRse = rucioRse;
  }

}
