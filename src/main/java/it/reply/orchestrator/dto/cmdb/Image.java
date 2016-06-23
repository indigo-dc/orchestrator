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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({ "image_id", "image_name", "architecture", "type", "distribution", "version" })
public class Image implements Serializable {

  private static final long serialVersionUID = 6029586902515892534L;

  @JsonProperty("image_id")
  private String imageId;
  @JsonProperty("image_name")
  private String imageName;
  @JsonProperty("architecture")
  private String architecture;
  @JsonProperty("type")
  private String type;
  @JsonProperty("distribution")
  private String distribution;
  @JsonProperty("version")
  private String version;
  @JsonProperty("service")
  private String service;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<String, Object>();

  /**
   * 
   * @return The imageId
   */
  @JsonProperty("image_id")
  public String getImageId() {
    return imageId;
  }

  /**
   * 
   * @param imageId
   *          The image_id
   */
  @JsonProperty("image_id")
  public void setImageId(String imageId) {
    this.imageId = imageId;
  }

  public Image withImageId(String imageId) {
    this.imageId = imageId;
    return this;
  }

  /**
   * 
   * @return The imageName
   */
  @JsonProperty("image_name")
  public String getImageName() {
    return imageName;
  }

  /**
   * 
   * @param imageName
   *          The image_name
   */
  @JsonProperty("image_name")
  public void setImageName(String imageName) {
    this.imageName = imageName;
  }

  public Image withImageName(String imageName) {
    this.imageName = imageName;
    return this;
  }

  /**
   * 
   * @return The architecture
   */
  @JsonProperty("architecture")
  public String getArchitecture() {
    return architecture;
  }

  /**
   * 
   * @param architecture
   *          The architecture
   */
  @JsonProperty("architecture")
  public void setArchitecture(String architecture) {
    this.architecture = architecture;
  }

  public Image withArchitecture(String architecture) {
    this.architecture = architecture;
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

  public Image withType(String type) {
    this.type = type;
    return this;
  }

  /**
   * 
   * @return The distribution
   */
  @JsonProperty("distribution")
  public String getDistribution() {
    return distribution;
  }

  /**
   * 
   * @param distribution
   *          The distribution
   */
  @JsonProperty("distribution")
  public void setDistribution(String distribution) {
    this.distribution = distribution;
  }

  public Image withDistribution(String distribution) {
    this.distribution = distribution;
    return this;
  }

  /**
   * 
   * @return The version
   */
  @JsonProperty("version")
  public String getVersion() {
    return version;
  }

  /**
   * 
   * @param version
   *          The version
   */
  @JsonProperty("version")
  public void setVersion(String version) {
    this.version = version;
  }

  public Image withVersion(String version) {
    this.version = version;
    return this;
  }

  /**
   * 
   * @return The service
   */
  @JsonProperty("service")
  public String getService() {
    return service;
  }

  /**
   * 
   * @param service
   *          The service
   */
  @JsonProperty("service")
  public void setService(String service) {
    this.service = service;
  }

  public Image withService(String service) {
    this.service = service;
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

  public Image withAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
    return this;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(imageId).append(imageName).append(architecture).append(type)
        .append(distribution).append(version).append(service).append(additionalProperties)
        .toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if ((other instanceof Image) == false) {
      return false;
    }
    Image rhs = ((Image) other);
    return new EqualsBuilder().append(imageId, rhs.imageId).append(imageName, rhs.imageName)
        .append(architecture, rhs.architecture).append(type, rhs.type)
        .append(distribution, rhs.distribution).append(version, rhs.version)
        .append(service, rhs.service).append(additionalProperties, rhs.additionalProperties)
        .isEquals();
  }

}