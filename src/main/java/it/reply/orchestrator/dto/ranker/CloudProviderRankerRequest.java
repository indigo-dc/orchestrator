package it.reply.orchestrator.dto.ranker;

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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import it.reply.orchestrator.dto.slam.PreferenceCustomer;
import it.reply.orchestrator.dto.slam.Sla;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({ "preferences", "sla", "monitoring" })
public class CloudProviderRankerRequest implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  @JsonProperty("preferences")
  private List<PreferenceCustomer> preferences = new ArrayList<PreferenceCustomer>();
  @JsonProperty("sla")
  private List<Sla> sla = new ArrayList<Sla>();
  @JsonProperty("monitoring")
  private List<Monitoring> monitoring = new ArrayList<Monitoring>();

  /**
   * Get the preferences for the customer.
   * 
   * @return The preferences
   */
  @JsonProperty("preferences")
  public List<PreferenceCustomer> getPreferences() {
    return preferences;
  }

  /**
   * Set the preferences for the customer.
   * 
   * @param preferences
   *          The preferences
   */
  @JsonProperty("preferences")
  public void setPreferences(List<PreferenceCustomer> preferences) {
    this.preferences = preferences;
  }

  public CloudProviderRankerRequest withPreferences(List<PreferenceCustomer> preferences) {
    this.preferences = preferences;
    return this;
  }

  /**
   * Get the sla for the customer.
   * 
   * @return The sla
   */
  @JsonProperty("sla")
  public List<Sla> getSla() {
    return sla;
  }

  /**
   * Set the sla for the customer.
   * 
   * @param sla
   *          The sla
   */
  @JsonProperty("sla")
  public void setSla(List<Sla> sla) {
    this.sla = sla;
  }

  public CloudProviderRankerRequest withSla(List<Sla> sla) {
    this.sla = sla;
    return this;
  }

  /**
   * Get the monitoring data for the customer.
   * 
   * @return The monitoring
   */
  @JsonProperty("monitoring")
  public List<Monitoring> getMonitoring() {
    return monitoring;
  }

  /**
   * Set the monitoring data for the customer.
   * 
   * @param monitoring
   *          The monitoring
   */
  @JsonProperty("monitoring")
  public void setMonitoring(List<Monitoring> monitoring) {
    this.monitoring = monitoring;
  }

  public CloudProviderRankerRequest withMonitoring(List<Monitoring> monitoring) {
    this.monitoring = monitoring;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(preferences).append(sla).append(monitoring).toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if ((other instanceof CloudProviderRankerRequest) == false) {
      return false;
    }
    CloudProviderRankerRequest rhs = ((CloudProviderRankerRequest) other);
    return new EqualsBuilder().append(preferences, rhs.preferences).append(sla, rhs.sla)
        .append(monitoring, rhs.monitoring).isEquals();
  }

}