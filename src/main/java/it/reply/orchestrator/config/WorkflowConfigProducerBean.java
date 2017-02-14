package it.reply.orchestrator.config;

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

  public static final String BASE_PATH = "workflows/";

  public static final WorkflowResource DEPLOY;
  public static final WorkflowResource UNDEPLOY;
  public static final WorkflowResource UPDATE;
  public static final WorkflowResource RANK_CLOUD_PROVIDERS;

  static {
    try {
      DEPLOY = new WorkflowResource(BASE_PATH + "Deploy.bpmn2");
      UNDEPLOY = new WorkflowResource(BASE_PATH + "Undeploy.bpmn2");
      UPDATE = new WorkflowResource(BASE_PATH + "Update.bpmn2");
      RANK_CLOUD_PROVIDERS = new WorkflowResource(BASE_PATH + "RankCloudProviders.bpmn2");

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
    resources.add(RANK_CLOUD_PROVIDERS);
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
    return 2;
  }

}
