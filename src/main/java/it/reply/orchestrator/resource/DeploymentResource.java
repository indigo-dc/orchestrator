package it.reply.orchestrator.resource;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.hateoas.Link;

import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Tasks;
import it.reply.orchestrator.resource.common.AbstractResource;

public class DeploymentResource extends AbstractResource {

  private Map<String, Object> outputs;
  private Tasks task;
  private List<BaseResource> resources;

  public DeploymentResource() {
    super();
  }

  public DeploymentResource(String uuid, List<Link> links, Map<String, Object> outputs,
      Date creationTime, Status status, Tasks task, List<BaseResource> resources) {
    super(uuid, creationTime, status);
    this.outputs = outputs;
    this.task = task;
    this.resources = resources;
  }

  public Map<String, Object> getOutputs() {
    return outputs;
  }

  public void setOutputs(Map<String, Object> outputs) {
    this.outputs = outputs;
  }

  public Tasks getTask() {
    return task;
  }

  public void setTask(Tasks task) {
    this.task = task;
  }

  public List<BaseResource> getResources() {
    return resources;
  }

  public void setResources(List<BaseResource> resources) {
    this.resources = resources;
  }

}
