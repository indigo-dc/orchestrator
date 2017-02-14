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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SlamPreferences implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  @JsonProperty("preferences")
  private List<Preference> preferences = new ArrayList<Preference>();
  @JsonProperty("sla")
  private List<Sla> sla = new ArrayList<Sla>();
  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<String, Object>();

  @JsonProperty("preferences")
  public List<Preference> getPreferences() {
    return preferences;
  }

  @JsonProperty("preferences")
  public void setPreferences(List<Preference> preferences) {
    this.preferences = preferences;
  }

  public SlamPreferences withPreferences(List<Preference> preferences) {
    this.preferences = preferences;
    return this;
  }

  @JsonProperty("sla")
  public List<Sla> getSla() {
    return sla;
  }

  @JsonProperty("sla")
  public void setSla(List<Sla> sla) {
    this.sla = sla;
  }

  public SlamPreferences withSla(List<Sla> sla) {
    this.sla = sla;
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

  public SlamPreferences withAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
    return this;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(preferences).append(sla).append(additionalProperties)
        .toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if ((other instanceof SlamPreferences) == false) {
      return false;
    }
    SlamPreferences rhs = ((SlamPreferences) other);
    return new EqualsBuilder().append(preferences, rhs.preferences).append(sla, rhs.sla)
        .append(additionalProperties, rhs.additionalProperties).isEquals();
  }

}
