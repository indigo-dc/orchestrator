package it.reply.orchestrator.resource;

import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.resource.common.AbstractResource;

import org.springframework.hateoas.Link;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class DeploymentResource extends AbstractResource {

  private Map<String, Object> outputs;
  private Task task;
  private String callback;
  private List<BaseResource> resources;

  public DeploymentResource() {
    super();
  }

  public DeploymentResource(String uuid, List<Link> links, Map<String, Object> outputs,
      Date creationTime, Status status, Task task, String callback, List<BaseResource> resources) {
    super(uuid, creationTime, status);
    this.outputs = outputs;
    this.task = task;
    this.callback = callback;
    this.resources = resources;
  }

  public Map<String, Object> getOutputs() {
    return outputs;
  }

  public void setOutputs(Map<String, Object> outputs) {
    this.outputs = outputs;
  }

  public Task getTask() {
    return task;
  }

  public void setTask(Task task) {
    this.task = task;
  }

  public String getCallback() {
    return callback;
  }

  public void setCallback(String callback) {
    this.callback = callback;
  }

  public List<BaseResource> getResources() {
    return resources;
  }

  public void setResources(List<BaseResource> resources) {
    this.resources = resources;
  }

}
