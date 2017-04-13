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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloudServiceData implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  @JsonProperty("service_type")
  private String serviceType;
  @JsonProperty("endpoint")
  private String endpoint;
  @JsonProperty("provider_id")
  private String providerId;
  @JsonProperty("type")
  private Type type;
  @JsonProperty("is_public_service")
  private boolean publicService;

  @JsonProperty("service_type")
  public String getServiceType() {
    return serviceType;
  }

  @JsonProperty("service_type")
  public void setServiceType(String serviceType) {
    this.serviceType = serviceType;
  }

  public CloudServiceData withServiceType(String serviceType) {
    this.serviceType = serviceType;
    return this;
  }

  @JsonProperty("endpoint")
  public String getEndpoint() {
    return endpoint;
  }

  @JsonProperty("endpoint")
  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public CloudServiceData withEndpoint(String endpoint) {
    this.endpoint = endpoint;
    return this;
  }

  @JsonProperty("provider_id")
  public String getProviderId() {
    return providerId;
  }

  @JsonProperty("provider_id")
  public void setProviderId(String providerId) {
    this.providerId = providerId;
  }

  public CloudServiceData withProviderId(String providerId) {
    this.providerId = providerId;
    return this;
  }

  @JsonProperty("type")
  public Type getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(Type type) {
    this.type = type;
  }

  public CloudServiceData withType(Type type) {
    this.type = type;
    return this;
  }

  @JsonProperty("is_public_service")
  public boolean isPublicService() {
    return publicService;
  }

  @JsonProperty("is_public_service")
  public void setPublicService(boolean publicService) {
    this.publicService = publicService;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(serviceType).append(endpoint).append(providerId)
        .append(type).toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }

    if (other == null) {
      return false;
    }

    if ((other instanceof CloudServiceData) == false) {
      return false;
    }
    CloudServiceData rhs = ((CloudServiceData) other);
    return new EqualsBuilder().append(serviceType, rhs.serviceType).append(endpoint, rhs.endpoint)
        .append(providerId, rhs.providerId).append(type, rhs.type).isEquals();
  }

}
