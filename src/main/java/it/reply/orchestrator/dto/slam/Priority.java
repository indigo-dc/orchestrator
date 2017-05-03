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

package it.reply.orchestrator.dto.slam;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({ "sla_id", "service_id" })
public class Priority implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  @JsonProperty("sla_id")
  private String slaId;
  @JsonProperty("service_id")
  private String serviceId;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<String, Object>();

  @JsonProperty("sla_id")
  public String getSlaId() {
    return slaId;
  }

  @JsonProperty("sla_id")
  public void setSlaId(String slaId) {
    this.slaId = slaId;
  }

  public Priority withSlaId(String slaId) {
    this.slaId = slaId;
    return this;
  }

  @JsonProperty("service_id")
  public String getServiceId() {
    return serviceId;
  }

  @JsonProperty("service_id")
  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  public Priority withServiceId(String serviceId) {
    this.serviceId = serviceId;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

  public Priority withAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
    return this;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(slaId).append(serviceId).append(additionalProperties)
        .toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if ((other instanceof Priority) == false) {
      return false;
    }
    Priority rhs = ((Priority) other);
    return new EqualsBuilder().append(slaId, rhs.slaId).append(serviceId, rhs.serviceId)
        .append(additionalProperties, rhs.additionalProperties).isEquals();
  }

}
