package it.reply.orchestrator.dto.cmdb;

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
@JsonPropertyOrder({ "_id", "_rev", "type", "data" })
public class CmdbImage {

  @JsonProperty("_id")
  private String id;
  @JsonProperty("_rev")
  private String rev;
  @JsonProperty("type")
  private String type;
  @JsonProperty("data")
  private Image data;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<String, Object>();

  /**
   *
   * @return The id
   */
  @JsonProperty("_id")
  public String getId() {
    return id;
  }

  /**
   *
   * @param id
   *          The _id
   */
  @JsonProperty("_id")
  public void setId(String id) {
    this.id = id;
  }

  public CmdbImage withId(String id) {
    this.id = id;
    return this;
  }

  /**
   *
   * @return The rev
   */
  @JsonProperty("_rev")
  public String getRev() {
    return rev;
  }

  /**
   *
   * @param rev
   *          The _rev
   */
  @JsonProperty("_rev")
  public void setRev(String rev) {
    this.rev = rev;
  }

  public CmdbImage withRev(String rev) {
    this.rev = rev;
    return this;
  }

  /**
   *
   * @return The type
   */
  @JsonProperty("type")
  public String getType() {
    return type;
  }

  /**
   *
   * @param type
   *          The type
   */
  @JsonProperty("type")
  public void setType(String type) {
    this.type = type;
  }

  public CmdbImage withType(String type) {
    this.type = type;
    return this;
  }

  /**
   *
   * @return The data
   */
  @JsonProperty("data")
  public Image getData() {
    return data;
  }

  /**
   *
   * @param data
   *          The data
   */
  @JsonProperty("data")
  public void setData(Image data) {
    this.data = data;
  }

  public CmdbImage withData(Image data) {
    this.data = data;
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

  public CmdbImage withAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
    return this;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(id).append(rev).append(type).append(data)
        .append(additionalProperties).toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if ((other instanceof CmdbImage) == false) {
      return false;
    }
    CmdbImage rhs = ((CmdbImage) other);
    return new EqualsBuilder().append(id, rhs.id).append(rev, rhs.rev).append(type, rhs.type)
        .append(data, rhs.data).append(additionalProperties, rhs.additionalProperties).isEquals();
  }

}