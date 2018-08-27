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

package it.reply.orchestrator.dto.cmdb;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.checkerframework.checker.nullness.qual.NonNull;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CloudService extends CmdbDataWrapper<CloudService, CloudServiceData> {

  private static final String COMPUTE_SERVICE_PREFIX = "eu.egi.cloud.vm-management";
  private static final String STORAGE_SERVICE_PREFIX = "eu.egi.cloud.storage-management";

  public static final String OPENSTACK_COMPUTE_SERVICE = COMPUTE_SERVICE_PREFIX + ".openstack";
  public static final String OPENNEBULA_COMPUTE_SERVICE = COMPUTE_SERVICE_PREFIX + ".opennebula";
  public static final String OCCI_COMPUTE_SERVICE = COMPUTE_SERVICE_PREFIX + ".occi";
  public static final String AWS_COMPUTE_SERVICE = "com.amazonaws.ec2";
  public static final String AZURE_COMPUTE_SERVICE = "com.microsoft.azure";
  public static final String OTC_COMPUTE_SERVICE = "eu.otc.compute";

  public static final String CDMI_STORAGE_SERVICE = STORAGE_SERVICE_PREFIX + ".cdmi";
  public static final String ONEPROVIDER_STORAGE_SERVICE = STORAGE_SERVICE_PREFIX + ".oneprovider";

  public static final String OPENNEBULA_TOSCA_SERVICE = "eu.indigo-datacloud.im-tosca.opennebula";

  @Builder
  protected CloudService(@NonNull String id, @NonNull CloudServiceData data) {
    super(id, data);
  }

  /**
   * Get if the the service type is the requested one.
   * 
   * @param type
   *          the requested type
   * @return true if the service type is the requested one, false otherwise
   */
  public boolean isServiceOfType(@NonNull String type) {
    return getData().getServiceType().equals(type);
  }

  /**
   * Get if the the service is a OpenStack compute service.
   * 
   * @return true if the service is a OpenStack compute service
   */
  @JsonIgnore
  public boolean isOpenStackComputeProviderService() {
    return isServiceOfType(OPENSTACK_COMPUTE_SERVICE);
  }

  /**
   * Get if the the service is a OTC compute service.
   * 
   * @return true if the service is a OTC compute service
   */
  @JsonIgnore
  public boolean isOtcComputeProviderService() {
    return isServiceOfType(OTC_COMPUTE_SERVICE);
  }

  /**
   * Get if the the service is a OpenNebula compute service.
   * 
   * @return true if the service is a OpenNebula compute service
   */
  @JsonIgnore
  public boolean isOpenNebulaComputeProviderService() {
    return isServiceOfType(OPENNEBULA_COMPUTE_SERVICE);
  }

  /**
   * Get if the the service is a OCCI compute service.
   * 
   * @return true if the service is a OCCI compute service
   */
  @JsonIgnore
  public boolean isOcciComputeProviderService() {
    return isServiceOfType(OCCI_COMPUTE_SERVICE);
  }

  /**
   * Get if the the service is a AWS compute service.
   * 
   * @return true if the service is a AWS compute service
   */
  @JsonIgnore
  public boolean isAwsComputeProviderService() {
    return isServiceOfType(AWS_COMPUTE_SERVICE);
  }

  /**
   * Get if the the service is a OneProvider storage service.
   * 
   * @return true if the service is a OneProvider storage service
   */
  @JsonIgnore
  public boolean isOneProviderStorageService() {
    return isServiceOfType(ONEPROVIDER_STORAGE_SERVICE);
  }

  /**
   * Get if the the service is a CDMI storage service.
   * 
   * @return true if the service is a CDMI storage service
   */
  @JsonIgnore
  public boolean isCdmiStorageProviderService() {
    return isServiceOfType(CDMI_STORAGE_SERVICE);
  }

  /**
   * Get if the the service is a OpenNebula TOSCA service.
   * 
   * @return true if the service is a OpenNebula TOSCA service
   */
  @JsonIgnore
  public boolean isOpenNebulaToscaProviderService() {
    return isServiceOfType(OPENNEBULA_TOSCA_SERVICE);
  }

  /**
   * Get if the the service is a Azure compute service.
   * 
   * @return true if the service is a Azure compute service
   */
  @JsonIgnore
  public boolean isAzureComputeProviderService() {
    return isServiceOfType(AZURE_COMPUTE_SERVICE);
  }

}
