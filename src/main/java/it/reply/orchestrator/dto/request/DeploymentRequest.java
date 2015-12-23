package it.reply.orchestrator.dto.request;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Map;

public class DeploymentRequest {

  private String template;
  private Map<String, String> parameters;

  /**
   * A string containing a TOSCA YAML-formatted template.
   * 
   * @return The template
   */
  public String getTemplate() {
    return template;
  }

  /**
   * 
   * @param template
   *          The TOSCA YAML-formatted template.
   */
  public void setTemplate(String template) {
    this.template = template;
  }

  public DeploymentRequest withTemplate(String template) {
    this.template = template;
    return this;
  }

  /**
   * The template parameters.
   * 
   * @return The parameters
   */
  public Map<String, String> getParameters() {
    return parameters;
  }

  /**
   * 
   * @param parameters
   *          The parameters.
   */
  public void setParameters(Map<String, String> parameters) {
    this.parameters = parameters;
  }

  public DeploymentRequest withParameters(Map<String, String> parameters) {
    this.parameters = parameters;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(template).append(parameters).toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if ((other instanceof DeploymentRequest) == false) {
      return false;
    }
    DeploymentRequest rhs = ((DeploymentRequest) other);
    return new EqualsBuilder().append(template, rhs.template).append(parameters, rhs.parameters)
        .isEquals();
  }

}