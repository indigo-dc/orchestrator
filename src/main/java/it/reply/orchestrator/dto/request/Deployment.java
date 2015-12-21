package it.reply.orchestrator.dto.request;

import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

//@JsonPropertyOrder({ "template", "parameters" })
public class Deployment {

  // @JsonProperty("template")
  private String template;
  // @JsonProperty("parameters")
  private Map<String, String> parameters;

  /**
   * A string containing a TOSCA YAML-formatted template.
   * 
   * @return The template
   */
  // @JsonProperty("template")
  public String getTemplate() {
    return template;
  }

  /**
   * 
   * @param template
   *          The TOSCA YAML-formatted template.
   */
  // @JsonProperty("template")
  public void setTemplate(String template) {
    this.template = template;
  }

  public Deployment withTemplate(String template) {
    this.template = template;
    return this;
  }

  /**
   * The template parameters.
   * 
   * @return The parameters
   */
  // @JsonProperty("parameters")
  public Map<String, String> getParameters() {
    return parameters;
  }

  /**
   * 
   * @param parameters
   *          The parameters.
   */
  // @JsonProperty("parameters")
  public void setParameters(Map<String, String> parameters) {
    this.parameters = parameters;
  }

  public Deployment withParameters(Map<String, String> parameters) {
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
    if ((other instanceof Deployment) == false) {
      return false;
    }
    Deployment rhs = ((Deployment) other);
    return new EqualsBuilder().append(template, rhs.template).append(parameters, rhs.parameters)
        .isEquals();
  }

}