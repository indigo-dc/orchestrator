
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
import org.hibernate.validator.constraints.NotEmpty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "customer", "provider", "start_date", "end_date", "services", "id" })
public class Sla implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  @JsonProperty("customer")
  @NotNull
  private String customer;

  @JsonProperty("provider")
  @NotNull
  private String cloudProviderId;

  @JsonProperty("start_date")
  @NotNull
  private String startDate;

  @NotNull
  @JsonProperty("end_date")
  private String endDate;

  @JsonProperty("services")
  @NotNull
  @NotEmpty
  private List<Service> services = new ArrayList<>();

  @JsonProperty("id")
  @NotNull
  private String id;

  @JsonIgnore
  @NotNull
  private transient Map<String, Object> additionalProperties = new HashMap<>();

  @JsonProperty("customer")
  public String getCustomer() {
    return customer;
  }

  @JsonProperty("customer")
  public void setCustomer(String customer) {
    this.customer = customer;
  }

  public Sla withCustomer(String customer) {
    this.customer = customer;
    return this;
  }

  @JsonProperty("provider")
  public String getCloudProviderId() {
    return cloudProviderId;
  }

  @JsonProperty("provider")
  public void setCloudProviderId(String cloudProviderId) {
    this.cloudProviderId = cloudProviderId;
  }

  public Sla withCloudProviderId(String cloudProviderId) {
    this.cloudProviderId = cloudProviderId;
    return this;
  }

  @JsonProperty("start_date")
  public String getStartDate() {
    return startDate;
  }

  @JsonProperty("start_date")
  public void setStartDate(String startDate) {
    this.startDate = startDate;
  }

  public Sla withStartDate(String startDate) {
    this.startDate = startDate;
    return this;
  }

  @JsonProperty("end_date")
  public String getEndDate() {
    return endDate;
  }

  @JsonProperty("end_date")
  public void setEndDate(String endDate) {
    this.endDate = endDate;
  }

  public Sla withEndDate(String endDate) {
    this.endDate = endDate;
    return this;
  }

  @JsonProperty("services")
  public List<Service> getServices() {
    return services;
  }

  @Deprecated
  public Service getService() {
    return services.get(0);
  }

  @JsonProperty("services")
  public void setServices(List<Service> services) {
    this.services = services;
  }

  public Sla withServices(List<Service> services) {
    this.services = services;
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

  public Sla withId(String id) {
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

  public Sla withAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
    return this;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(customer).append(cloudProviderId).append(startDate)
        .append(endDate).append(services).append(id).append(additionalProperties).toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (!(other instanceof Sla)) {
      return false;
    }
    Sla rhs = (Sla) other;
    return new EqualsBuilder().append(customer, rhs.customer)
        .append(cloudProviderId, rhs.cloudProviderId).append(startDate, rhs.startDate)
        .append(endDate, rhs.endDate).append(services, rhs.services).append(id, rhs.id)
        .append(additionalProperties, rhs.additionalProperties).isEquals();
  }

}
