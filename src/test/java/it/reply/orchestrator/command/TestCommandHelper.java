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

package it.reply.orchestrator.command;

import com.google.common.collect.Maps;

import it.reply.workflowmanager.orchestrator.bpm.WIHs.EJBWorkItemHelper;

import org.drools.core.process.instance.impl.WorkItemImpl;
import org.kie.api.executor.CommandContext;

import java.util.Map;

public class TestCommandHelper {

  public static CommandContextBuilder buildCommandContext() {
    return new CommandContextBuilder();
  }

  public static class CommandContextBuilder {
    Map<String, Object> params = Maps.newHashMap();

    public CommandContextBuilder withParam(String key, Object value) {
      params.put(key, value);
      return this;
    }

    public CommandContext get() {
      WorkItemImpl workItem = new WorkItemImpl();
      workItem.setDeploymentId("deploymentId");
      workItem.setProcessInstanceId(0);
      CommandContext ctx = EJBWorkItemHelper.buildCommandContext(workItem, null);
      if (params != null) {
        params.entrySet().stream().forEach(e -> workItem.setParameter(e.getKey(), e.getValue()));
      }
      return ctx;
    }
  }

}
