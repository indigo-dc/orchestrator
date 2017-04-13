/*
 * Copyright © 2015-2017 Santer Reply S.p.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.reply.orchestrator.dto.request;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

public class DeploymentRequest {

  @NotNull
  private String template;

  @NotNull
  private Map<String, Object> parameters = new HashMap<>();

  @MonotonicNonNull
  private String callback;

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

  /**
   * The template parameters.
   * 
   * @return The parameters
   */
  public Map<String, Object> getParameters() {
    return parameters;
  }

  /**
   * 
   * @param parameters
   *          The parameters.
   */
  public void setParameters(Map<String, Object> parameters) {
    this.parameters = parameters;
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
  public void setCallback(String callback) {
    this.callback = callback;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(template).append(parameters).append(callback).toHashCode();
  }

  @Override
  public boolean equals(Object other) {
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