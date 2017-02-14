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
@JsonPropertyOrder({ "customer", "preferences", "id" })
public class Preference implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  @JsonProperty("customer")
  private String customer;
  @JsonProperty("preferences")
  private List<PreferenceCustomer> preferences = new ArrayList<PreferenceCustomer>();
  @JsonProperty("id")
  private String id;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<String, Object>();

  @JsonProperty("customer")
  public String getCustomer() {
    return customer;
  }

  @JsonProperty("customer")
  public void setCustomer(String customer) {
    this.customer = customer;
  }

  public Preference withCustomer(String customer) {
    this.customer = customer;
    return this;
  }

  @JsonProperty("preferences")
  public List<PreferenceCustomer> getPreferences() {
    return preferences;
  }

  @JsonProperty("preferences")
  public void setPreferences(List<PreferenceCustomer> preferences) {
    this.preferences = preferences;
  }

  public Preference withPreferences(List<PreferenceCustomer> preferences) {
    this.preferences = preferences;
    return this;
  }

  @JsonProperty("id")
  public String getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(String id) {
    this.id = id;
  }

  public Preference withId(String id) {
    this.id = id;
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

  public Preference withAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
    return this;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(customer).append(preferences).append(id)
        .append(additionalProperties).toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if ((other instanceof Preference) == false) {
      return false;
    }
    Preference rhs = ((Preference) other);
    return new EqualsBuilder().append(customer, rhs.customer).append(preferences, rhs.preferences)
        .append(id, rhs.id).append(additionalProperties, rhs.additionalProperties).isEquals();
  }

}
