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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({ "service_type", "priority" })
public class PreferenceCustomer {

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
