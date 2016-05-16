package it.reply.orchestrator.dto.cmdb;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Provider {

  @JsonProperty("_id")
  private String id;
  @JsonProperty("_rev")
  private String rev;
  @JsonProperty("data")
  private ProviderData data;
  @JsonProperty("type")
  private String type;
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

  public Provider withId(String id) {
    this.id = id;
    return this;
  }

  @JsonProperty("_rev")
  public String getRev() {
    return rev;
  }

  @JsonProperty("_rev")
  public void setRev(String rev) {
    this.rev = rev;
  }

  public Provider withRev(String rev) {
    this.rev = rev;
    return this;
  }

  @JsonProperty("data")
  public ProviderData getData() {
    return data;
  }

  @JsonProperty("data")
  public void setData(ProviderData data) {
    this.data = data;
  }

  public Provider withData(ProviderData data) {
    this.data = data;
    return this;
  }

  @JsonProperty("type")
  public String getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(String type) {
    this.type = type;
  }

  public Provider withType(String type) {
    this.type = type;
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

  public Provider withAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
    return this;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(id).append(rev).append(data).append(type)
        .append(additionalProperties).toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if ((other instanceof Provider) == false) {
      return false;
    }
    Provider rhs = ((Provider) other);
    return new EqualsBuilder().append(id, rhs.id).append(rev, rhs.rev).append(data, rhs.data)
        .append(type, rhs.type).append(additionalProperties, rhs.additionalProperties).isEquals();
  }

}