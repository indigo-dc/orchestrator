package it.reply.orchestrator.dto.common;

import it.reply.orchestrator.enums.Status;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "id", "resourceType", "links", "creationTime", "status", "requiredBy" })
public class Resource {

  @JsonProperty("id")
  private String id;
  @JsonProperty("resourceType")
  private String resourceType;
  @JsonProperty("links")
  private List<Link> links;
  @JsonProperty("creationTime")
  private Date creationTime;
  @JsonProperty("status")
  private Status status;
  @JsonProperty("requiredBy")
  private List<String> requiredBy = new ArrayList<String>();

  /**
   * 
   * @return The id.
   */
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  /**
   * 
   * @param id
   *          The id.
   */
  @JsonProperty("id")
  public void setId(String id) {
    this.id = id;
  }

  public Resource withId(String id) {
    this.id = id;
    return this;
  }

  /**
   * 
   * @return The resourceType.
   */
  @JsonProperty("resourceType")
  public String getResourceType() {
    return resourceType;
  }

  /**
   * 
   * @param resourceType
   *          The resourceType.
   */
  @JsonProperty("resourceType")
  public void setResourceType(String resourceType) {
    this.resourceType = resourceType;
  }

  public Resource withResourceType(String resourceType) {
    this.resourceType = resourceType;
    return this;
  }

  /**
   * 
   * @return The links.
   */
  @JsonProperty("links")
  public List<Link> getLinks() {
    return links;
  }

  /**
   * 
   * @param links
   *          The links.
   */
  @JsonProperty("links")
  public void setLinks(List<Link> links) {
    this.links = links;
  }

  public Resource withLinks(List<Link> links) {
    this.links = links;
    return this;
  }

  /**
   * 
   * @return The creationTime.
   */
  @JsonProperty("creationTime")
  public Date getCreationTime() {
    return creationTime;
  }

  /**
   * 
   * @param creationTime
   *          The creationTime.
   */
  @JsonProperty("creationTime")
  public void setCreationTime(Date creationTime) {
    this.creationTime = creationTime;
  }

  public Resource withCreationTime(Date creationTime) {
    this.creationTime = creationTime;
    return this;
  }

  /**
   * 
   * @return The status.
   */
  @JsonProperty("status")
  public Status getStatus() {
    return status;
  }

  /**
   * 
   * @param status
   *          The status.
   */
  @JsonProperty("status")
  public void setStatus(Status status) {
    this.status = status;
  }

  public Resource withStatus(Status status) {
    this.status = status;
    return this;
  }

  /**
   * 
   * @return The requiredBy.
   */
  @JsonProperty("requiredBy")
  public List<String> getRequiredBy() {
    return requiredBy;
  }

  /**
   * 
   * @param requiredBy
   *          The requiredBy.
   */
  @JsonProperty("requiredBy")
  public void setRequiredBy(List<String> requiredBy) {
    this.requiredBy = requiredBy;
  }

  public Resource withRequiredBy(List<String> requiredBy) {
    this.requiredBy = requiredBy;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(id).append(resourceType).append(links).append(creationTime)
        .append(status).append(requiredBy).toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if ((other instanceof Resource) == false) {
      return false;
    }
    Resource rhs = ((Resource) other);
    return new EqualsBuilder().append(id, rhs.id).append(resourceType, rhs.resourceType)
        .append(links, rhs.links).append(creationTime, rhs.creationTime).append(status, rhs.status)
        .append(requiredBy, rhs.requiredBy).isEquals();
  }

}