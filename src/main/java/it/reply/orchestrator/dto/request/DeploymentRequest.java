package it.reply.orchestrator.dto.request;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DeploymentRequest {

  private String template = "";
  private Map<String, Object> parameters = new HashMap<>();

  private String callback;

  /**
   * A string containing a TOSCA YAML-formatted template.
   * 
   * @return The template
   */
  @Nonnull
  public String getTemplate() {
    return template;
  }

  /**
   * 
   * @param template
   *          The TOSCA YAML-formatted template.
   */
  public void setTemplate(@Nonnull String template) {
    this.template = template;
  }

  @Nonnull
  public DeploymentRequest withTemplate(@Nonnull String template) {
    this.template = template;
    return this;
  }

  /**
   * The template parameters.
   * 
   * @return The parameters
   */
  @Nonnull
  public Map<String, Object> getParameters() {
    return parameters;
  }

  /**
   * 
   * @param parameters
   *          The parameters.
   */
  public void setParameters(@Nonnull Map<String, Object> parameters) {
    this.parameters = parameters;
  }

  @Nonnull
  public DeploymentRequest withParameters(@Nonnull Map<String, Object> parameters) {
    this.parameters = parameters;
    return this;
  }

  /**
   * A string containing the endpoint used by the orchestrator to notify the progress of the
   * deployment process.
   * 
   * @return The endpoint
   */
  @Nullable
  public String getCallback() {
    return callback;
  }

  /**
   * 
   * @param callback
   *          the endpoint.
   */
  public void setCallback(@Nullable String callback) {
    this.callback = callback;
  }

  @Nonnull
  public DeploymentRequest withCallback(@Nullable String callback) {
    this.callback = callback;
    return this;
  }

  @Override
  @Nonnull
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(template).append(parameters).append(callback).toHashCode();
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (other == null) {
      return false;
    }
    if (other == this) {
      return true;
    }
    if ((other instanceof DeploymentRequest) == false) {
      return false;
    }
    DeploymentRequest rhs = ((DeploymentRequest) other);
    return new EqualsBuilder().append(template, rhs.template).append(parameters, rhs.parameters)
        .append(callback, rhs.callback).isEquals();
  }

}