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

import java.util.Collections;
import java.util.List;
import javax.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class MesosFrameworkService<T extends MesosFrameworkServiceProperties> extends
    CloudService {

  @NonNull
  @NotNull
  private T properties;

  /**
   * Create a new MesosFrameworkService.
   * @param id
   *     the id of the service
   * @param serviceType
   *     the serviceType of the service
   * @param endpoint
   *     the endpoint of the service
   * @param providerId
   *     the providerId of the service
   * @param type
   *     the type of the service
   * @param publicService
   *     the publicService of the service
   * @param region
   *     the region of the service
   * @param tenant
   *     the tenant
   * @param hostname
   *     the hostname of the service
   * @param parentServiceId
   *     the parentServiceId of the service
   * @param iamEnabled
   *     the iamEnabled flag
   * @param idpProtocol
   *     the IDP protocol name
   * @param publicIpAssignable
   *     the publicIpAssignable flag
   * @param supportedIdps
   *     the supportedIdps
   * @param properties
   *     the properties
   */
  public MesosFrameworkService(
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
      @NonNull T properties) {
    super(id, serviceType, endpoint, microversion, providerId, type, publicService,
          tenant, region, hostname, parentServiceId, iamEnabled, idpProtocol,
          publicIpAssignable, supportedIdps, Collections.<VolumeType>emptyList());
    this.properties = properties;
  }
}
