/*
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

import it.reply.orchestrator.dto.cmdb.CloudService.CloudServiceBuilder;
import it.reply.orchestrator.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Data
@EqualsAndHashCode(callSuper = true)
public class ComputeService extends CloudService {

  @JsonProperty("images")
  @NonNull
  @NotNull
  //@Builder.Default
  List<Image> images = new ArrayList<>();

  @JsonProperty("flavors")
  @NonNull
  @NotNull
  //@Builder.Default
  List<Flavor> flavors = new ArrayList<>();

  @JsonProperty("public_network_name")
  @Nullable
  private String publicNetworkName;

  @JsonProperty("private_network_cidr")
  @Nullable
  private String privateNetworkCidr;

  @JsonProperty("private_network_name")
  @Nullable
  private String privateNetworkName;

  /**
   * Generate a new ComputeService.
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
   * @param images
   *     the images
   * @param flavors
   *     the flavors
   * @param iamEnabled
   *     the iamEnabled flag
   * @param publicIpAssignable
   *     the publicIpAssignable flag
   * @param publicNetworkName
   *     the public network name
   * @param privateNetworkCidr
   *     the private network CIDR
   * @param privateNetworkName
   *     the private network name
   */
  @Builder(builderMethodName = "computeBuilder")
  public ComputeService(
      @NonNull String id,
      @NonNull String serviceType,
      @NonNull String endpoint,
      @NonNull String providerId,
      @NonNull CloudServiceType type,
      boolean publicService,
      @Nullable String region,
      @NonNull String hostname,
      @Nullable String parentServiceId,
      @NonNull List<Image> images,
      @NonNull List<Flavor> flavors,
      boolean iamEnabled,
      @Nullable boolean publicIpAssignable,
      @Nullable String publicNetworkName,
      @Nullable String privateNetworkCidr,
      @Nullable String privateNetworkName) {
    super(id, serviceType, endpoint, providerId, type, publicService, region, hostname,
            parentServiceId, iamEnabled, publicIpAssignable);
    this.images = CommonUtils.notNullOrDefaultValue(images, ArrayList::new);
    this.flavors = CommonUtils.notNullOrDefaultValue(flavors, ArrayList::new);
    this.publicNetworkName = publicNetworkName;
    this.privateNetworkName = privateNetworkName;
    this.privateNetworkCidr = privateNetworkCidr;
  }

  @Deprecated
  private ComputeService() {
    images = new ArrayList<>();
    flavors = new ArrayList<>();
  }
}
