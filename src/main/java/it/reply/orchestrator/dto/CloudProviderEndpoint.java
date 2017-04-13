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

package it.reply.orchestrator.dto;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.io.Serializable;

/**
 * This class holds information to connect (and authenticate) to a CloudProvider.
 * 
 * @author l.biava
 *
 */
public class CloudProviderEndpoint implements Serializable {

  private static final long serialVersionUID = -2585914648218602033L;

  public enum IaaSType {
    // @formatter:off
    OPENSTACK, OPENNEBULA, OCCI, AWS
    // @formatter:on
  }

  private String imEndpoint;
  private String cpEndpoint;
  private String cpComputeServiceId;
  private IaaSType iaasType;

  private String username;
  private String password;

  public String getImEndpoint() {
    return imEndpoint;
  }

  public void setImEndpoint(String imEndpoint) {
    this.imEndpoint = imEndpoint;
  }

  public String getCpEndpoint() {
    return cpEndpoint;
  }

  public void setCpEndpoint(String cpEndpoint) {
    this.cpEndpoint = cpEndpoint;
  }

  public String getCpComputeServiceId() {
    return cpComputeServiceId;
  }

  public void setCpComputeServiceId(String cpComputeServiceId) {
    this.cpComputeServiceId = cpComputeServiceId;
  }

  public IaaSType getIaasType() {
    return iaasType;
  }

  public void setIaasType(IaaSType iaasType) {
    this.iaasType = iaasType;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public String toString() {
    return "CloudProviderEndpoint [imEndpoint=" + imEndpoint + ", cpEndpoint=" + cpEndpoint
        + ", cpComputeServiceId=" + cpComputeServiceId + ", iaasType=" + iaasType + "]";
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public boolean equals(Object other) {
    return EqualsBuilder.reflectionEquals(this, other);
  }
  
}
