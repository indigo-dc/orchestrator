/*
 * Copyright © 2015-2018 Santer Reply S.p.A.
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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This class holds information to connect (and authenticate) to a CloudProvider.
 * 
 * @author l.biava
 *
 */
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "password", callSuper = false)
public class CloudProviderEndpoint extends AdditionalPropertiesAwareDto implements Serializable {

  private static final long serialVersionUID = -2585914648218602033L;

  public enum IaaSType {
    OPENSTACK,
    OPENNEBULA,
    OCCI,
    AWS,
    OTC,
    AZURE;
  }

  private String imEndpoint;
  private String cpEndpoint;
  private String cpComputeServiceId;
  private IaaSType iaasType;

  @Nullable
  @JsonProperty
  private String region;

  @Nullable
  private String username;

  @Nullable
  private String password;

  @Nullable
  private String tenant;

  @JsonProperty
  private String iaasHeaderId;

  @NonNull
  private Map<String, CloudProviderEndpoint> hybridCloudProviderEndpoints = new HashMap<>();

  @JsonIgnore
  public Optional<String> getIaasHeaderId() {
    return Optional.ofNullable(iaasHeaderId);
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
