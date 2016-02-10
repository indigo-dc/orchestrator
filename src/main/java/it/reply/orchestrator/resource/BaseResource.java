package it.reply.orchestrator.resource;

import it.reply.orchestrator.resource.common.AbstractResource;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.List;

public class BaseResource extends AbstractResource {

  private String toscaNodeType;
  private String resourceType;
  private List<String> requiredBy = new ArrayList<String>();

  public String getToscaNodeType() {
    return this.toscaNodeType;
  }

  public void setToscaNodeType(String toscaNodeType) {
    this.toscaNodeType = toscaNodeType;
  }

  public BaseResource withToscaNodeType(String toscaNodeType) {
    this.toscaNodeType = toscaNodeType;
    return this;
  }

  public void setResourceType(String resourceType) {
    this.resourceType = resourceType;
  }

  public BaseResource withResourceType(String resourceType) {
    this.resourceType = resourceType;
    return this;
  }

  public List<String> getRequiredBy() {
    return requiredBy;
  }

  public void setRequiredBy(List<String> requiredBy) {
    this.requiredBy = requiredBy;
  }

  public BaseResource withRequiredBy(List<String> requiredBy) {
    this.requiredBy = requiredBy;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(resourceType).append(requiredBy).toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if ((other instanceof BaseResource) == false) {
      return false;
    }
    BaseResource rhs = ((BaseResource) other);
    return new EqualsBuilder().append(resourceType, rhs.resourceType)
        .append(requiredBy, rhs.requiredBy).isEquals();
  }

}