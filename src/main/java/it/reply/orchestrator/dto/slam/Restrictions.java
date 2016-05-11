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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({ "total_guaranteed" })
public class Restrictions {

  @JsonProperty("total_guaranteed")
  private Integer totalGuaranteed;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<String, Object>();

  @JsonProperty("total_guaranteed")
  public Integer getTotalGuaranteed() {
    return totalGuaranteed;
  }

  @JsonProperty("total_guaranteed")
  public void setTotalGuaranteed(Integer totalGuaranteed) {
    this.totalGuaranteed = totalGuaranteed;
  }

  public Restrictions withTotalGuaranteed(Integer totalGuaranteed) {
    this.totalGuaranteed = totalGuaranteed;
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

  public Restrictions withAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
    return this;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(totalGuaranteed).append(additionalProperties).toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if ((other instanceof Restrictions) == false) {
      return false;
    }
    Restrictions rhs = ((Restrictions) other);
    return new EqualsBuilder().append(totalGuaranteed, rhs.totalGuaranteed)
        .append(additionalProperties, rhs.additionalProperties).isEquals();
  }

}
