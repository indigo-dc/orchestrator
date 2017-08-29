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

package it.reply.orchestrator.service.commands;

import it.reply.workflowmanager.orchestrator.bpm.WIHs.EJBWorkItemHelper;
import it.reply.workflowmanager.utils.Constants;

import org.assertj.core.api.Assertions;
import org.drools.core.process.instance.impl.WorkItemImpl;
import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;

import java.util.HashMap;
import java.util.Map;

public class TestCommandHelper {

  public static CommandContextBuilder buildCommandContext() {
    return new CommandContextBuilder();
  }

  public static class CommandContextBuilder {
    private Map<String, Object> params = new HashMap<>();

    public CommandContextBuilder withParam(String key, Object value) {
      params.put(key, value);
      return this;
    }

    public CommandContext get() {
      WorkItemImpl workItem = new WorkItemImpl();
      workItem.setDeploymentId("deploymentId");
      workItem.setProcessInstanceId(0);
      CommandContext ctx = EJBWorkItemHelper.buildCommandContext(workItem, null);

      params.forEach((key, value) -> workItem.setParameter(key, value));

      return ctx;
    }
  }

  public static void assertBaseResults(Object status, ExecutionResults actualResult) {
    Assertions
        .assertThat(actualResult.getData(Constants.RESULT_STATUS))
        .isEqualTo("OK");
    Assertions
        .assertThat(actualResult.getData(Constants.OK_RESULT))
        .isEqualTo(status);
  }
}
