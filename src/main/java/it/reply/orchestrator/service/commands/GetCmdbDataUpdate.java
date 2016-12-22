package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.service.CmdbService;

import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GetCmdbDataUpdate extends BaseDeployCommand {

  private static final Logger LOG = LoggerFactory.getLogger(GetCmdbDataUpdate.class);

  @Autowired
  private CmdbService cmdbService;

  @Override
  protected ExecutionResults customExecute(CommandContext ctx,
      DeploymentMessage deploymentMessage) {
    Deployment deployment = deploymentMessage.getDeployment();
    CloudProvider cp = new CloudProvider(deployment.getCloudProviderName());
    cp.getCmdbProviderServices().put(deployment.getCloudProviderEndpoint().getCpComputeServiceId(),
        null);
    cmdbService.fillCloudProviderInfo(cp);
    deploymentMessage.setChosenCloudProvider(cp);
    return resultOccurred(cp);
  }

  @Override
  protected String getErrorMessagePrefix() {
    return "Error retrieving info from CMDB";
  }
}
