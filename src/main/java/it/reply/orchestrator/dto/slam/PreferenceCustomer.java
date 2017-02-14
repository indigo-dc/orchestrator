package it.reply.orchestrator.dto.slam;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({ "service_type", "priority" })
public class PreferenceCustomer implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  @JsonProperty("service_type")
  private String serviceType;
  @JsonProperty("priority")
  private List<Priority> priority = new ArrayList<Priority>();
  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<String, Object>();

  @JsonProperty("service_type")
  public String getServiceType() {
    return serviceType;
  }

  @JsonProperty("service_type")
  public void setServiceType(String serviceType) {
    this.serviceType = serviceType;
  }

  public PreferenceCustomer withServiceType(String serviceType) {
    this.serviceType = serviceType;
    return this;
  }

  @JsonProperty("priority")
  public List<Priority> getPriority() {
    return priority;
  }

  @JsonProperty("priority")
  public void setPriority(List<Priority> priority) {
    this.priority = priority;
  }

  public PreferenceCustomer withPriority(List<Priority> priority) {
    this.priority = priority;
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

  public PreferenceCustomer withAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
    return this;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(serviceType).append(priority).append(additionalProperties)
        .toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if ((other instanceof PreferenceCustomer) == false) {
      return false;
    }
    PreferenceCustomer rhs = ((PreferenceCustomer) other);
    return new EqualsBuilder().append(serviceType, rhs.serviceType).append(priority, rhs.priority)
        .append(additionalProperties, rhs.additionalProperties).isEquals();
  }

}
