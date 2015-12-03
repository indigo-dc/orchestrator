package it.reply.orchestrator.dto.response;

import it.reply.orchestrator.dto.common.Deployment;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Deployments {

  @JsonProperty("deployments")
  private List<Deployment> deployments = new ArrayList<Deployment>();

  /**
   * 
   * @return The deployments
   */
  @JsonProperty("deployments")
  public List<Deployment> getDeployments() {
    return deployments;
  }

  /**
   * 
   * @param deployments
   *          The deployments
   */
  @JsonProperty("deployments")
  public void setDeployments(List<Deployment> deployments) {
    this.deployments = deployments;
  }

  public Deployments withDeployments(List<Deployment> deployments) {
    this.deployments = deployments;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(deployments).toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if ((other instanceof Deployments) == false) {
      return false;
    }
    Deployments rhs = ((Deployments) other);
    return new EqualsBuilder().append(deployments, rhs.deployments).isEquals();
  }

}