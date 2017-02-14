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
@JsonPropertyOrder({ "type", "service_id", "targets" })
public class Service implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  @JsonProperty("type")
  private String type;
  @JsonProperty("service_id")
  private String serviceId;
  @JsonProperty("targets")
  private List<Target> targets = new ArrayList<Target>();
  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<String, Object>();

  @JsonProperty("type")
  public String getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(String type) {
    this.type = type;
  }

  public Service withType(String type) {
    this.type = type;
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

  public Service withServiceId(String serviceId) {
    this.serviceId = serviceId;
    return this;
  }

  @JsonProperty("targets")
  public List<Target> getTargets() {
    return targets;
  }

  @JsonProperty("targets")
  public void setTargets(List<Target> targets) {
    this.targets = targets;
  }

  public Service withTargets(List<Target> targets) {
    this.targets = targets;
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

  public Service withAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
    return this;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(type).append(serviceId).append(targets)
        .append(additionalProperties).toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if ((other instanceof Service) == false) {
      return false;
    }
    Service rhs = ((Service) other);
    return new EqualsBuilder().append(type, rhs.type).append(serviceId, rhs.serviceId)
        .append(targets, rhs.targets).append(additionalProperties, rhs.additionalProperties)
        .isEquals();
  }

}
