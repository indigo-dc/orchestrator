package it.reply.orchestrator.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Map;

@JsonPropertyOrder({ "template", "parameters" })
public class DeploymentRequest {

  @JsonProperty("template")
  private String template;
  @JsonProperty("parameters")
  private Map<String, Object> parameters;

  /**
   * A string containing a TOSCA YAML-formatted template.
   * 
   * @return The template
   */
  @JsonProperty("template")
  public String getTemplate() {
    return template;
  }

  /**
   * 
   * @param template
   *          The template.
   */
  @JsonProperty("template")
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
  @JsonProperty("parameters")
  public Map<String, Object> getParameters() {
    return parameters;
  }

  /**
   * 
   * @param parameters
   *          The parameters.
   */
  @JsonProperty("parameters")
  public void setParameters(Map<String, Object> parameters) {
    this.parameters = parameters;
  }

  public DeploymentRequest withParameters(Map<String, Object> parameters) {
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