/*
 * Copyright © 2015-2017 Santer Reply S.p.A.
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

import com.google.common.base.Preconditions;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serializable;
import java.util.Optional;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloudService extends CmdbDataWrapper<CloudService, CloudServiceData>
    implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  private static final String COMPUTE_SERVICE_PREFIX = "eu.egi.cloud.vm-management";
  private static final String STORAGE_SERVICE_PREFIX = "eu.egi.cloud.storage-management";

  public static final String OPENSTACK_COMPUTE_SERVICE = COMPUTE_SERVICE_PREFIX + ".openstack";
  public static final String OPENNEBULA_COMPUTE_SERVICE = COMPUTE_SERVICE_PREFIX + ".opennebula";
  public static final String OCCI_COMPUTE_SERVICE = COMPUTE_SERVICE_PREFIX + ".occi";
  public static final String AWS_COMPUTE_SERVICE = "com.amazonaws.ec2";

  public static final String CDMI_STORAGE_SERVICE = STORAGE_SERVICE_PREFIX + ".cdmi";
  public static final String ONEPROVIDER_STORAGE_SERVICE = STORAGE_SERVICE_PREFIX + ".oneprovider";

  public static final String OPENNEBULA_TOSCA_SERVICE = "eu.indigo-datacloud.im-tosca.opennebula";

  @Builder
  private CloudService(@Nullable String id, @Nullable String rev, @Nullable String type,
      @Nullable CloudServiceData data) {
    super(id, rev, type, data);
  }

  /**
   * Get if the the service type is the requested one.
   * 
   * @param type
   *          the requested type
   * @return true if the service type is the requested one, false otherwise
   */
  public boolean isServiceOfType(String type) {
    Preconditions.checkNotNull(type);
    return Optional.ofNullable(getData())
        .map(CloudServiceData::getServiceType)
        .map(type::equals)
        .orElse(false);
  }

  /**
   * Get if the the service is a OpenStack compute service.
   * 
   * @return true if the service is a OpenStack compute service
   */
  public boolean isOpenStackComputeProviderService() {
    return isServiceOfType(OPENSTACK_COMPUTE_SERVICE);
  }

  /**
   * Get if the the service is a OpenNebula compute service.
   * 
   * @return true if the service is a OpenNebula compute service
   */
  public boolean isOpenNebulaComputeProviderService() {
    return isServiceOfType(OPENNEBULA_COMPUTE_SERVICE);
  }

  /**
   * Get if the the service is a OCCI compute service.
   * 
   * @return true if the service is a OCCI compute service
   */
  public boolean isOcciComputeProviderService() {
    return isServiceOfType(OCCI_COMPUTE_SERVICE);
  }

  /**
   * Get if the the service is a AWS compute service.
   * 
   * @return true if the service is a AWS compute service
   */
  public boolean isAwsComputeProviderService() {
    return isServiceOfType(AWS_COMPUTE_SERVICE);
  }

  /**
   * Get if the the service is a OneProvider storage service.
   * 
   * @return true if the service is a OneProvider storage service
   */
  public boolean isOneProviderStorageService() {
    return isServiceOfType(ONEPROVIDER_STORAGE_SERVICE);
  }

  /**
   * Get if the the service is a CDMI storage service.
   * 
   * @return true if the service is a CDMI storage service
   */
  public boolean isCdmiStorageProviderService() {
    return isServiceOfType(CDMI_STORAGE_SERVICE);
  }

  /**
   * Get if the the service is a OpenNebula TOSCA service.
   * 
   * @return true if the service is a OpenNebula TOSCA service
   */
  public boolean isOpenNebulaToscaProviderService() {
    return isServiceOfType(OPENNEBULA_TOSCA_SERVICE);
  }

}
