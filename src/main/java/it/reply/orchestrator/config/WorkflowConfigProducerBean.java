package it.reply.orchestrator.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.kie.api.io.Resource;
import org.springframework.stereotype.Component;

import it.reply.workflowManager.orchestrator.config.ConfigProducer;

@Component
public class WorkflowConfigProducerBean implements ConfigProducer {

  public final static String BASE_PATH = "workflows";

  public final static WorkflowResource DEPLOY;
  public final static WorkflowResource UNDEPLOY;

  static {
    try {
      DEPLOY = new WorkflowResource(BASE_PATH + "/" + "Deploy.bpmn2");
      UNDEPLOY = new WorkflowResource(BASE_PATH + "/" + "Undeploy.bpmn2");
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
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
