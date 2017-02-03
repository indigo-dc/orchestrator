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

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "image_id", "image_name", "architecture", "type", "distribution", "version" })
public class ImageData implements Serializable {

  private static final long serialVersionUID = 6029586902515892534L;

  @JsonProperty("image_id")
  private String imageId;
  @JsonProperty("image_name")
  private String imageName;
  @JsonProperty("image_description")
  private String imageDescription;
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
  @JsonProperty("user_name")
  private String userName;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<String, Object>();

  @JsonProperty("image_id")
  public String getImageId() {
    return imageId;
  }

  @JsonProperty("image_id")
  public void setImageId(String imageId) {
    this.imageId = imageId;
  }

  public ImageData withImageId(String imageId) {
    this.imageId = imageId;
    return this;
  }

  @JsonProperty("image_name")
  public String getImageName() {
    return imageName;
  }

  @JsonProperty("image_name")
  public void setImageName(String imageName) {
    this.imageName = imageName;
  }

  public ImageData withImageName(String imageName) {
    this.imageName = imageName;
    return this;
  }

  @JsonProperty("architecture")
  public String getArchitecture() {
    return architecture;
  }

  @JsonProperty("architecture")
  public void setArchitecture(String architecture) {
    this.architecture = architecture;
  }

  public ImageData withArchitecture(String architecture) {
    this.architecture = architecture;
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

  public ImageData withType(String type) {
    this.type = type;
    return this;
  }

  @JsonProperty("distribution")
  public String getDistribution() {
    return distribution;
  }

  @JsonProperty("distribution")
  public void setDistribution(String distribution) {
    this.distribution = distribution;
  }

  public ImageData withDistribution(String distribution) {
    this.distribution = distribution;
    return this;
  }

  @JsonProperty("version")
  public String getVersion() {
    return version;
  }

  @JsonProperty("version")
  public void setVersion(String version) {
    this.version = version;
  }

  public ImageData withVersion(String version) {
    this.version = version;
    return this;
  }

  @JsonProperty("service")
  public String getService() {
    return service;
  }

  @JsonProperty("service")
  public void setService(String service) {
    this.service = service;
  }

  public ImageData withService(String service) {
    this.service = service;
    return this;
  }

  public String getImageDescription() {
    return imageDescription;
  }

  public String getUserName() {
    return userName;
  }

  public void setImageDescription(String imageDescription) {
    this.imageDescription = imageDescription;
  }

  public ImageData withImageDescription(String imageDescription) {
    this.imageDescription = imageDescription;
    return this;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public ImageData withUserName(String userName) {
    this.userName = userName;
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

  public ImageData withAdditionalProperty(String name, Object value) {
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
    if (other == null) {
      return false;
    }
    if ((other instanceof ImageData) == false) {
      return false;
    }
    ImageData rhs = ((ImageData) other);
    return new EqualsBuilder().append(imageId, rhs.imageId).append(imageName, rhs.imageName)
        .append(architecture, rhs.architecture).append(type, rhs.type)
        .append(distribution, rhs.distribution).append(version, rhs.version)
        .append(service, rhs.service).append(additionalProperties, rhs.additionalProperties)
        .isEquals();
  }

}