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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({ "type", "unit", "restrictions" })
public class Target implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  @JsonProperty("type")
  private String type;
  @JsonProperty("unit")
  private String unit;
  @JsonProperty("restrictions")
  private Restrictions restrictions;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<String, Object>();

  @JsonProperty("type")
  public String getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(String type) {
    this.type = type;
  }

  public Target withType(String type) {
    this.type = type;
    return this;
  }

  @JsonProperty("unit")
  public String getUnit() {
    return unit;
  }

  @JsonProperty("unit")
  public void setUnit(String unit) {
    this.unit = unit;
  }

  public Target withUnit(String unit) {
    this.unit = unit;
    return this;
  }

  @JsonProperty("restrictions")
  public Restrictions getRestrictions() {
    return restrictions;
  }

  @JsonProperty("restrictions")
  public void setRestrictions(Restrictions restrictions) {
    this.restrictions = restrictions;
  }

  public Target withRestrictions(Restrictions restrictions) {
    this.restrictions = restrictions;
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

  public Target withAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
    return this;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(type).append(unit).append(restrictions)
        .append(additionalProperties).toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if ((other instanceof Target) == false) {
      return false;
    }
    Target rhs = ((Target) other);
    return new EqualsBuilder().append(type, rhs.type).append(unit, rhs.unit)
        .append(restrictions, rhs.restrictions)
        .append(additionalProperties, rhs.additionalProperties).isEquals();
  }

}
