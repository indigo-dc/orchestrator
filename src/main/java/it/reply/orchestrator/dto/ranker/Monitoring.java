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

package it.reply.orchestrator.dto.ranker;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import it.reply.monitoringpillar.domain.dsl.monitoring.pillar.wrapper.paas.PaaSMetric;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({ "provider", "metrics" })
public class Monitoring implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  @JsonProperty("provider")
  private String provider;
  @JsonProperty("metrics")
  private List<PaaSMetric> metrics = new ArrayList<PaaSMetric>();

  /**
   * Creates a new monitoring object.
   * 
   * @param provider
   *          the provider
   * @param metrics
   *          the metrics
   */
  public Monitoring(String provider, List<PaaSMetric> metrics) {
    super();
    this.provider = provider;
    this.metrics = metrics;
  }

  /**
   * Get the provider.
   * 
   * @return The provider
   */
  @JsonProperty("provider")
  public String getProvider() {
    return provider;
  }

  /**
   * Set the provider.
   * 
   * @param provider
   *          The provider
   */
  @JsonProperty("provider")
  public void setProvider(String provider) {
    this.provider = provider;
  }

  public Monitoring withProvider(String provider) {
    this.provider = provider;
    return this;
  }

  /**
   * Get the monitoring metrics.
   * 
   * @return The metrics
   */
  @JsonProperty("metrics")
  public List<PaaSMetric> getMetrics() {
    return metrics;
  }

  /**
   * Set the monitoring metrics.
   * 
   * @param metrics
   *          The metrics
   */
  @JsonProperty("metrics")
  public void setMetrics(List<PaaSMetric> metrics) {
    this.metrics = metrics;
  }

  public Monitoring withMetrics(List<PaaSMetric> metrics) {
    this.metrics = metrics;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(provider).append(metrics).toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if ((other instanceof Monitoring) == false) {
      return false;
    }
    Monitoring rhs = ((Monitoring) other);
    return new EqualsBuilder().append(provider, rhs.provider).append(metrics, rhs.metrics)
        .isEquals();
  }

}
