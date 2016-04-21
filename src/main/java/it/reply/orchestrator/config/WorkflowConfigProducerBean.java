package it.reply.orchestrator.config;

import it.reply.workflowmanager.orchestrator.config.ConfigProducer;

import org.kie.api.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class WorkflowConfigProducerBean implements ConfigProducer {

  public static final String BASE_PATH = "workflows";

  public static final WorkflowResource DEPLOY;
  public static final WorkflowResource UNDEPLOY;
  public static final WorkflowResource UPDATE;

  static {
    try {
      DEPLOY = new WorkflowResource(BASE_PATH + "/" + "Deploy.bpmn2");
      UNDEPLOY = new WorkflowResource(BASE_PATH + "/" + "Undeploy.bpmn2");
      UPDATE = new WorkflowResource(BASE_PATH + "/" + "Update.bpmn2");

    } catch (IOException ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  private List<WorkflowResource> resources;

  public WorkflowConfigProducerBean() {
    initResourceList();
  }

  private void initResourceList() {
    resources = new ArrayList<>();
    resources.add(DEPLOY);
    resources.add(UNDEPLOY);
    resources.add(UPDATE);
    resources = Collections.unmodifiableList(resources);
  }

  @Override
  public List<Resource> getJbpmResources() {
    return resources.stream().map(r -> r.getResource()).collect(Collectors.toList());
  }

  @Override
  public List<WorkflowResource> getWorkflowResources() {
    return resources;
  }

  @Override
  public int getExecutorServiceThreadPoolSize() {
    return 10;
  }

  @Override
  public int getExecutorServiceInterval() {
    return 3;
  }

}
