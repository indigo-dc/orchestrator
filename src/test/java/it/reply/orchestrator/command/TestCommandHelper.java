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
