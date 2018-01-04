/*
 * Copyright © 2015-2018 Santer Reply S.p.A.
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

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.deployment.BaseWorkflowMessage;
import it.reply.orchestrator.service.deployment.providers.DeploymentStatusHelper;
import it.reply.workflowmanager.spring.orchestrator.bpm.ejbcommands.BaseCommand;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.OptimisticLockException;

/**
 * Base behavior for all Deploy WF tasks. <br/>
 * This checks input parameters and manages output and errors (specifically, in case of errors, it
 * also updates directly the deployment status on DB).
 * 
 * @author l.biava
 *
 */
@Slf4j
public abstract class BaseWorkflowCommand<M extends BaseWorkflowMessage,
    T extends BaseWorkflowCommand<M, T>> extends BaseCommand<T> {

  @Autowired
  protected DeploymentStatusHelper deploymentStatusHelper;

  @Autowired
  private DeploymentRepository deploymentRepository;

  protected abstract String getErrorMessagePrefix();

  protected abstract String getMessageParameterName();

  /**
   * <b>This method SHOULD NOT be overridden! It cannot be final for INJECTION purpose!</b> <br/>
   * Use the {@link #customExecute(RankCloudProvidersMessage)} method to implement command logic.
   */
  @Override
  protected ExecutionResults customExecute(CommandContext ctx) {
    M message = getRequiredParameter(ctx, getMessageParameterName());
    ExecutionResults exResults;
    try {
      exResults = getFacade().customExecute(ctx, message);
      resultOccurred(true, exResults);
    } catch (Exception ex) {
      LOG.error(getErrorMessagePrefix(), ex);
      exResults = resultOccurred(false);
      if (ExceptionUtils.indexOfThrowable(ex, OptimisticLockException.class) == -1) {
        // not due to OptimisticLockException
        deploymentStatusHelper.updateOnError(message.getDeploymentId(), getErrorMessagePrefix(),
            ex);
      } else {
        // due to OptimisticLockException, some other thread will handle it
        LOG.debug(
            "Not setting deployment in error because exception was caused by optimistic lock");
      }
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
    }
    exResults.setData(getMessageParameterName(), message);
    return exResults;
  }

  protected abstract ExecutionResults customExecute(CommandContext ctx, M message) throws Exception;

  protected Deployment getDeployment(M message) {
    return deploymentRepository.findOne(message.getDeploymentId());
  }

}
