package it.reply.orchestrator.resource;

import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.resource.common.AbstractResource;

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
