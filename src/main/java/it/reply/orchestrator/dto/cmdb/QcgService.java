/*
 * Copyright © 2019-2020 I.N.F.N.
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
public class QcgService extends  CloudService {

  /**
   * Create a new QcgService.
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
   * @param hostname
   *     the hostname of the service
   * @param parentServiceId
   *     the parentServiceId of the service
   * @param iamEnabled
   *     the iamEnabled flag
   * @param publicIpAssignable
   *     the publicIpAssignable flag
   */
  @Builder(builderMethodName = "qcgBuilder")
  public QcgService(
      @NonNull String id,
      @NonNull String serviceType,
      @NonNull String endpoint,
      @NonNull String providerId,
      @NonNull CloudServiceType type,
      boolean publicService,
      @Nullable String region,
      @NonNull String hostname,
      @Nullable String parentServiceId,
      boolean iamEnabled,
      boolean publicIpAssignable) {
    super(id, serviceType, endpoint, providerId, type, publicService, region, hostname,
            parentServiceId, iamEnabled, publicIpAssignable);
  }
}
