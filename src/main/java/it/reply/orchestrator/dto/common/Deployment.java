package it.reply.orchestrator.dto.common;

import it.reply.orchestrator.enums.Status;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.hateoas.Identifiable;
import org.springframework.scheduling.config.Task;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
public class Deployment implements Identifiable<String> {

  @JsonProperty("id")
  private String id;
  @JsonProperty("links")
  private List<Link> links;
  @JsonProperty("outputs")
  private Map<String, Object> outputs;
  @JsonProperty("creationTime")
  private Date creationTime;
  @JsonProperty("status")
  private Status status;
  @JsonProperty("task")
  private Task task;
  @JsonProperty("resources")
  private List<Resource> resources;

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

  public Deployment withId(String id) {
    this.id = id;
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

  public Deployment withLinks(List<Link> links) {
    this.links = links;
    return this;
  }

  /**
   * 
   * @return The outputs.
   */
  @JsonProperty("outputs")
  public Map<String, Object> getOutputs() {
    return outputs;
  }

  /**
   * 
   * @param outputs
   *          The outputs.
   */
  @JsonProperty("outputs")
  public void setOutputs(Map<String, Object> outputs) {
    this.outputs = outputs;
  }

  public Deployment withOutputs(Map<String, Object> outputs) {
    this.outputs = outputs;
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

  public Deployment withCreationTime(Date creationTime) {
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

  public Deployment withStatus(Status status) {
    this.status = status;
    return this;
  }

  /**
   * 
   * @return The task.
   */
  @JsonProperty("task")
  public Task getTask() {
    return task;
  }

  /**
   * 
   * @param task
   *          The task.
   */
  @JsonProperty("task")
  public void setTask(Task task) {
    this.task = task;
  }

  public Deployment withTask(Task task) {
    this.task = task;
    return this;
  }

  /**
   * 
   * @return The resources.
   */
  @JsonProperty("resources")
  public List<Resource> getResources() {
    return resources;
  }

  /**
   * 
   * @param resources
   *          The resources.
   */
  @JsonProperty("resources")
  public void setResources(List<Resource> resources) {
    this.resources = resources;
  }

  public Deployment withResources(List<Resource> resources) {
    this.resources = resources;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(id).append(links).append(outputs).append(creationTime)
        .append(status).append(task).append(resources).toHashCode();
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
    return new EqualsBuilder().append(id, rhs.id).append(links, rhs.links)
        .append(outputs, rhs.outputs).append(creationTime, rhs.creationTime)
        .append(status, rhs.status).append(task, rhs.task).append(resources, rhs.resources)
        .isEquals();
  }

}