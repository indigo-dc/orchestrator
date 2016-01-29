package it.reply.orchestrator.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.kie.api.io.Resource;
import org.kie.internal.io.ResourceFactory;
import org.springframework.stereotype.Component;

import it.reply.workflowManager.orchestrator.config.ConfigProducer;

@Component
public class WorkflowConfigProducerBean implements ConfigProducer {

  public final static String BASE_PATH = "workflows";

  public final static String TEST = "New Process.bpmn2";

  private List<Resource> resources;

  public WorkflowConfigProducerBean() {
    initResourceList();
  }

  private void initResourceList() {
    resources = new ArrayList<>();
    resources.add(ResourceFactory.newClassPathResource(BASE_PATH + "/" + TEST));
    resources = Collections.unmodifiableList(resources);
  }

  @Override
  public List<Resource> getResources() {
    return resources;
  }

}
