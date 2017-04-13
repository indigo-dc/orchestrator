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

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "_id", "_rev", "type", "data" })
public abstract class CmdbDataWrapper<U, T> implements Serializable {

  private static final long serialVersionUID = -7442528095759086603L;

  @JsonProperty("_id")
  private String id;
  @JsonProperty("_rev")
  private String rev;
  @JsonProperty("type")
  private String type;
  @JsonProperty("data")
  private T data;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<String, Object>();

  @JsonProperty("_id")
  public String getId() {
    return id;
  }

  @JsonProperty("_id")
  public void setId(String id) {
    this.id = id;
  }

  @SuppressWarnings("unchecked")
  public U withId(String id) {
    this.id = id;
    return (U) this;
  }

  @JsonProperty("_rev")
  public String getRev() {
    return rev;
  }

  @JsonProperty("_rev")
  public void setRev(String rev) {
    this.rev = rev;
  }

  @SuppressWarnings("unchecked")
  public U withRev(String rev) {
    this.rev = rev;
    return (U) this;
  }

  @JsonProperty("type")
  public String getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(String type) {
    this.type = type;
  }

  @SuppressWarnings("unchecked")
  public U withType(String type) {
    this.type = type;
    return (U) this;
  }

  @JsonProperty("data")
  public T getData() {
    return data;
  }

  @JsonProperty("data")
  public void setData(T data) {
    this.data = data;
  }

  @SuppressWarnings("unchecked")
  public U withData(T data) {
    this.data = data;
    return (U) this;
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

  @SuppressWarnings("unchecked")
  public U withAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
    return (U) this;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(id).append(rev).append(type).append(data)
        .append(additionalProperties).toHashCode();
  }

  @Override
  public boolean equals(Object other) {

    if (other == this) {
      return true;
    }

    if (other == null) {
      return false;
    }

    if ((other instanceof CmdbDataWrapper) == false) {
      return false;
    }

    CmdbDataWrapper<?, ?> rhs = ((CmdbDataWrapper<?, ?>) other);

    return new EqualsBuilder().append(id, rhs.id).append(rev, rhs.rev).append(type, rhs.type)
        .append(data, rhs.data).append(additionalProperties, rhs.additionalProperties).isEquals();
  }

}