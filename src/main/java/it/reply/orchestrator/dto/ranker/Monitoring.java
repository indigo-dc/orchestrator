package it.reply.orchestrator.dto.ranker;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import it.reply.monitoringpillar.domain.dsl.monitoring.pillar.wrapper.paas.PaaSMetric;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({ "provider", "metrics" })
public class Monitoring {

  @JsonProperty("provider")
  private String provider;
  @JsonProperty("metrics")
  private List<PaaSMetric> metrics = new ArrayList<PaaSMetric>();

  public Monitoring(String provider, List<PaaSMetric> metrics) {
    super();
    this.provider = provider;
    this.metrics = metrics;
  }

  /**
   * 
   * @return The provider
   */
  @JsonProperty("provider")
  public String getProvider() {
    return provider;
  }

  /**
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
   * 
   * @return The metrics
   */
  @JsonProperty("metrics")
  public List<PaaSMetric> getMetrics() {
    return metrics;
  }

  /**
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
