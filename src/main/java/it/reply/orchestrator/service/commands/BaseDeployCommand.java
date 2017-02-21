package it.reply.orchestrator.service.commands;

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

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.service.WorkflowConstants;
import it.reply.orchestrator.service.deployment.providers.DeploymentStatusHelper;
import it.reply.workflowmanager.spring.orchestrator.bpm.ejbcommands.BaseCommand;

import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

/**
 * Base behavior for all Deploy WF tasks. <br/>
 * This checks input parameters and manages output and errors (specifically, in case of errors, it
 * also updates directly the deployment status on DB).
 * 
 * @author l.biava
 *
 */
public abstract class BaseDeployCommand extends BaseCommand {

  private static final Logger LOG = LoggerFactory.getLogger(BaseDeployCommand.class);

  @Autowired
  protected DeploymentStatusHelper deploymentStatusHelper;

  @Autowired
  protected DeploymentRepository deploymentRepository;

  protected abstract String getErrorMessagePrefix();

  /**
   * <b>This method SHOULD NOT be overridden! It cannot be final for INJECTION purpose!</b> <br/>
   * Use the {@link #customExecute(RankCloudProvidersMessage)} method to implement command logic.
   */
  @Override
  protected ExecutionResults customExecute(CommandContext ctx) throws Exception {
    DeploymentMessage deploymentMessage =
        getParameter(ctx, WorkflowConstants.WF_PARAM_DEPLOYMENT_MESSAGE);
    if (deploymentMessage == null) {
      throw new IllegalArgumentException(String.format("WF parameter <%s> cannot be null",
          WorkflowConstants.WF_PARAM_DEPLOYMENT_MESSAGE));
    }
    ExecutionResults exResults = new ExecutionResults();
    try {
      // Load the DB Deployment from ID (this way we avoid jBPM JPA serialization issues)
      deploymentMessage
          .setDeployment(deploymentRepository.findOne(deploymentMessage.getDeploymentId()));

      exResults.getData().putAll(customExecute(ctx, deploymentMessage).getData());
      exResults.setData(WorkflowConstants.WF_PARAM_DEPLOYMENT_MESSAGE, deploymentMessage);
    } catch (Exception ex) {
      LOG.error(String.format("Error executing %s", this.getClass().getSimpleName()), ex);
      exResults.getData().putAll(resultOccurred(false).getData());

      // Update deployment with error
      // TODO: what if this fails??
      deploymentStatusHelper.updateOnError(deploymentMessage.getDeploymentId(),
          generateErrorMessagePrefix(deploymentMessage), ex);
    }

    // Save and then remove entities (again for jBPM JPA serialization issues)
    if (deploymentMessage.getDeployment() != null) {
      deploymentRepository.save(deploymentMessage.getDeployment());
      deploymentMessage.setDeployment(null);
    }
    return exResults;
  }

  protected abstract ExecutionResults customExecute(CommandContext ctx,
      DeploymentMessage deploymentMessage);

  private String generateErrorMessagePrefix(DeploymentMessage deploymentMessage) {
    String deploymentProviderMessagePrefix = Optional.ofNullable(deploymentMessage.getDeployment())
        .map(Deployment::getDeploymentProvider)
        .map(deploymentProvider -> " with deployment provider " + deploymentProvider.toString())
        .orElse("");
    return String.format("%s%s", getErrorMessagePrefix(), deploymentProviderMessagePrefix);
  }

}
