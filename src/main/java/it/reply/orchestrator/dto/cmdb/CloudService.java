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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;

import javax.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "service_type",
    visible = true,
    include = As.EXISTING_PROPERTY
)
@JsonTypeIdResolver(CloudServiceResolver.class)
public class CloudService implements CmdbIdentifiable {

  @JsonProperty("id")
  @NonNull
  @NotNull
  private String id;

  @JsonProperty("service_type")
  @NonNull
  @NotNull
  private String serviceType;

  @JsonProperty("endpoint")
  @NonNull
  @NotNull
  private String endpoint;

  @JsonProperty("provider_id")
  @NonNull
  @NotNull
  private String providerId;

  @JsonProperty("type")
  @NonNull
  @NotNull
  private CloudServiceType type;

  @JsonProperty("is_public_service")
  private boolean publicService;

  @JsonProperty("region")
  @Nullable
  private String region;

  @JsonProperty("hostname")
  @NonNull
  @NotNull
  private String hostname;

  @JsonProperty("service_parent_id")
  @Nullable
  private String parentServiceId;

  @JsonProperty("iam_enabled")
  private boolean iamEnabled = true;

  @JsonProperty("public_ip_assignable")
  private boolean publicIpAssignable;

  private static final String INDIGO_SERVICE_PREFIX = "eu.indigo-datacloud";
  private static final String EGI_SERVICE_PREFIX = "eu.egi.cloud";

  private static final String COMPUTE_SERVICE_PREFIX = EGI_SERVICE_PREFIX + ".vm-management";
  private static final String STORAGE_SERVICE_PREFIX = EGI_SERVICE_PREFIX + ".storage-management";

  public static final String OPENSTACK_COMPUTE_SERVICE = "org.openstack.nova";
  public static final String OPENNEBULA_COMPUTE_SERVICE = COMPUTE_SERVICE_PREFIX + ".opennebula";
  public static final String OCCI_COMPUTE_SERVICE = COMPUTE_SERVICE_PREFIX + ".occi";
  public static final String AWS_COMPUTE_SERVICE = "com.amazonaws.ec2";
  public static final String AZURE_COMPUTE_SERVICE = "com.microsoft.azure";
  public static final String OTC_COMPUTE_SERVICE = "eu.otc.compute";

  public static final String CDMI_STORAGE_SERVICE = STORAGE_SERVICE_PREFIX + ".cdmi";
  public static final String ONEPROVIDER_STORAGE_SERVICE = STORAGE_SERVICE_PREFIX + ".oneprovider";

  public static final String OPENNEBULA_TOSCA_SERVICE =
      INDIGO_SERVICE_PREFIX + ".im-tosca.opennebula";

  public static final String MARATHON_COMPUTE_SERVICE = INDIGO_SERVICE_PREFIX + ".marathon";
  public static final String CHRONOS_COMPUTE_SERVICE = INDIGO_SERVICE_PREFIX + ".chronos";
  public static final String QCG_COMPUTE_SERVICE = "eu.deep.qcg";

  /**
   * Get if the the service is a OpenStack compute service.
   *
   * @return true if the service is a OpenStack compute service
   */
  @JsonIgnore
  public boolean isOpenStackComputeProviderService() {
    return OPENSTACK_COMPUTE_SERVICE.equals(this.serviceType);
  }

  /**
   * Get if the the service is a OTC compute service.
   *
   * @return true if the service is a OTC compute service
   */
  @JsonIgnore
  public boolean isOtcComputeProviderService() {
    return OTC_COMPUTE_SERVICE.equals(this.serviceType);
  }

  /**
   * Get if the the service is a OpenNebula compute service.
   *
   * @return true if the service is a OpenNebula compute service
   */
  @JsonIgnore
  public boolean isOpenNebulaComputeProviderService() {
    return OPENNEBULA_COMPUTE_SERVICE.equals(this.serviceType);
  }

  /**
   * Get if the the service is a OCCI compute service.
   *
   * @return true if the service is a OCCI compute service
   */
  @JsonIgnore
  public boolean isOcciComputeProviderService() {
    return OCCI_COMPUTE_SERVICE.equals(this.serviceType);
  }

  /**
   * Get if the the service is a AWS compute service.
   *
   * @return true if the service is a AWS compute service
   */
  @JsonIgnore
  public boolean isAwsComputeProviderService() {
    return AWS_COMPUTE_SERVICE.equals(this.serviceType);
  }

  /**
   * Get if the the service is a OneProvider storage service.
   *
   * @return true if the service is a OneProvider storage service
   */
  @JsonIgnore
  public boolean isOneProviderStorageService() {
    return ONEPROVIDER_STORAGE_SERVICE.equals(this.serviceType);
  }

  /**
   * Get if the the service is a CDMI storage service.
   *
   * @return true if the service is a CDMI storage service
   */
  @JsonIgnore
  public boolean isCdmiStorageProviderService() {
    return CDMI_STORAGE_SERVICE.equals(this.serviceType);
  }

  /**
   * Get if the the service is a OpenNebula TOSCA service.
   *
   * @return true if the service is a OpenNebula TOSCA service
   */
  @JsonIgnore
  public boolean isOpenNebulaToscaProviderService() {
    return OPENNEBULA_TOSCA_SERVICE.equals(this.serviceType);
  }

  /**
   * Get if the the service is a Azure compute service.
   *
   * @return true if the service is a Azure compute service
   */
  @JsonIgnore
  public boolean isAzureComputeProviderService() {
    return AZURE_COMPUTE_SERVICE.equals(this.serviceType);
  }

  /**
   * Get if the the service is a Marathon compute service.
   *
   * @return true if the service is a Marathon compute service
   */
  @JsonIgnore
  public boolean isMarathonComputeProviderService() {
    return MARATHON_COMPUTE_SERVICE.equals(this.serviceType);
  }

  /**
   * Get if the the service is a Chronos compute service.
   *
   * @return true if the service is a Chronos compute service
   */
  @JsonIgnore
  public boolean isChronosComputeProviderService() {
    return CHRONOS_COMPUTE_SERVICE.equals(this.serviceType);
  }

  /**
   * Get if the the service is a Qcg compute service.
   *
   * @return true if the service is a Qcg compute service
   */
  @JsonIgnore
  public boolean isQcgComputeProviderService() {
    return QCG_COMPUTE_SERVICE.equals(this.serviceType);
  }

  @JsonIgnore
  public boolean isCredentialsRequired() {
    return isAwsComputeProviderService() || isAzureComputeProviderService()
        || isOtcComputeProviderService();
  }
}
