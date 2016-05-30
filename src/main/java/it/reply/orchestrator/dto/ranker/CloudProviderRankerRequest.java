package it.reply.orchestrator.dto.ranker;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import it.reply.orchestrator.dto.slam.PreferenceCustomer;
import it.reply.orchestrator.dto.slam.Sla;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({ "preferences", "sla", "monitoring" })
public class CloudProviderRankerRequest {

  @JsonProperty("preferences")
  private List<PreferenceCustomer> preferences = new ArrayList<PreferenceCustomer>();
  @JsonProperty("sla")
  private List<Sla> sla = new ArrayList<Sla>();
  @JsonProperty("monitoring")
  private List<Monitoring> monitoring = new ArrayList<Monitoring>();

  /**
   * 
   * @return The preferences
   */
  @JsonProperty("preferences")
  public List<PreferenceCustomer> getPreferences() {
    return preferences;
  }

  /**
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
   * 
   * @return The sla
   */
  @JsonProperty("sla")
  public List<Sla> getSla() {
    return sla;
  }

  /**
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
   * 
   * @return The monitoring
   */
  @JsonProperty("monitoring")
  public List<Monitoring> getMonitoring() {
    return monitoring;
  }

  /**
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