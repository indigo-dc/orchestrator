/*
 * Copyright © 2015-2020 Santer Reply S.p.A.
 * Copyright © 2020-2021 I.N.F.N.
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

package it.reply.orchestrator.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import it.reply.orchestrator.dto.cmdb.CloudService.SupportedIdp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class CloudProviderEndpoint {

  public enum IaaSType {
    OPENSTACK,
    OPENNEBULA,
    OCCI,
    AWS,
    OTC,
    AZURE,
    CHRONOS,
    MARATHON,
    QCG,
    KUBERNETES;
  }

  @Nullable
  private String imEndpoint;

  @NonNull
  @NotNull
  private String cpEndpoint;

  @NonNull
  @NotNull
  private String cpComputeServiceId;

  @NonNull
  @NotNull
  private IaaSType iaasType;

  @Nullable
  @JsonProperty
  private String tenant;

  @Nullable
  @JsonProperty
  private String region;

  @Nullable
  @JsonProperty
  private String iaasHeaderId;

  @Nullable
  private String vaultEndpoint;

  @NonNull
  @NotNull
  @Builder.Default
  private Map<String, CloudProviderEndpoint> hybridCloudProviderEndpoints = new HashMap<>();

  @Builder.Default
  @JsonProperty
  private boolean iamEnabled = true;

  @Nullable
  @JsonProperty
  private List<SupportedIdp> supportedIdps;

  @Builder.Default
  @JsonProperty
  @Getter(AccessLevel.NONE)
  private String idpProtocol = "oidc";

  @SuppressWarnings("null")
  @Deprecated
  protected CloudProviderEndpoint() {
    hybridCloudProviderEndpoints = new HashMap<>();
  }

  @JsonIgnore
  public Optional<String> getIaasHeaderId() {
    return Optional.ofNullable(iaasHeaderId);
  }

  @JsonIgnore
  public Optional<List<SupportedIdp>> getIdpName() {
    return Optional.ofNullable(supportedIdps);
  }


  /**
   * getIdpProtocol.
   * @return the name of Idp Protocol
   */
  @JsonIgnore
  public String getIdpProtocol() {
    if (idpProtocol == null) {
      return "oidc";
    }
    return idpProtocol;
  }

  @JsonIgnore
  public Optional<String> getTenant() {
    return Optional.ofNullable(tenant);
  }

  @JsonIgnore
  public Optional<String> getRegion() {
    return Optional.ofNullable(region);
  }

  /**
   * Generates a list with all the CloudProviderEndpoint of the deployments.
   *
   * @return the list
   */
  @JsonIgnore
  public List<CloudProviderEndpoint> getAllCloudProviderEndpoint() {
    List<CloudProviderEndpoint> returnList = new ArrayList<>();
    returnList.add(this);
    returnList.addAll(hybridCloudProviderEndpoints.values());
    return returnList;
  }

}
