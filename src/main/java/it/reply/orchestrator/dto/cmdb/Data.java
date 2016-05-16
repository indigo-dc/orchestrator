package it.reply.orchestrator.dto.cmdb;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Data {

  @JsonProperty("service_type")
  private String serviceType;
  @JsonProperty("endpoint")
  private String endpoint;
  @JsonProperty("provider_id")
  private String providerId;
  @JsonProperty("type")
  private Type type;

  @JsonProperty("service_type")
  public String getServiceType() {
    return serviceType;
  }

  @JsonProperty("service_type")
  public void setServiceType(String serviceType) {
    this.serviceType = serviceType;
  }

  public Data withServiceType(String serviceType) {
    this.serviceType = serviceType;
    return this;
  }

  @JsonProperty("endpoint")
  public String getEndpoint() {
    return endpoint;
  }

  @JsonProperty("endpoint")
  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public Data withEndpoint(String endpoint) {
    this.endpoint = endpoint;
    return this;
  }

  @JsonProperty("provider_id")
  public String getProviderId() {
    return providerId;
  }

  @JsonProperty("provider_id")
  public void setProviderId(String providerId) {
    this.providerId = providerId;
  }

  public Data withProviderId(String providerId) {
    this.providerId = providerId;
    return this;
  }

  @JsonProperty("type")
  public Type getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(Type type) {
    this.type = type;
  }

  public Data withType(Type type) {
    this.type = type;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(serviceType).append(endpoint).append(providerId)
        .append(type).toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if ((other instanceof Data) == false) {
      return false;
    }
    Data rhs = ((Data) other);
    return new EqualsBuilder().append(serviceType, rhs.serviceType).append(endpoint, rhs.endpoint)
        .append(providerId, rhs.providerId).append(type, rhs.type).isEquals();
  }

}
