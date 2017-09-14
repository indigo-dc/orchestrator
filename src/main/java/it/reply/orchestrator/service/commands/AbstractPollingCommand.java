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

import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.function.SerializableBiPredicate;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderService;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderServiceRegistry;
import it.reply.orchestrator.utils.CommonUtils;
import it.reply.orchestrator.utils.WorkflowConstants;
import it.reply.utils.misc.polling.AbstractPollingBehaviour;
import it.reply.utils.misc.polling.ExternallyControlledPoller;
import it.reply.utils.misc.polling.PollingBehaviour;
import it.reply.utils.misc.polling.PollingException;
import it.reply.utils.misc.polling.RetriesExceededException;
import it.reply.workflowmanager.spring.orchestrator.bpm.OrchestratorContextBean;

import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public abstract class AbstractPollingCommand<T extends AbstractPollingCommand<T>>
    extends BaseDeployCommand<T> {

  @Override
  public ExecutionResults customExecute(CommandContext ctx,
      DeploymentMessage deploymentMessage) throws Exception {

    ExecutionResults exResults = new ExecutionResults();

    long timeoutTime =
        CommonUtils.notNullOrDefaultValue(deploymentMessage.getTimeoutTime(), Long.MAX_VALUE);

    ExternallyControlledPoller<DeploymentMessage, Boolean> pollingStatus =
        AbstractPollingCommand
            .<ExternallyControlledPoller<DeploymentMessage, Boolean>>getOptionalParameter(ctx,
                WorkflowConstants.WF_PARAM_POLLING_STATUS)
            .or(() -> getPoller(timeoutTime));

    exResults.setData(WorkflowConstants.WF_PARAM_POLLING_STATUS, pollingStatus);

    try {
      boolean result = getFacade().executePollingEvent(pollingStatus, deploymentMessage);
      deploymentMessage.setPollComplete(result);
      return resultOccurred(true, exResults);
    } catch (PollingException ex) {
      throw handlePollingException(ex, pollingStatus.getLastException());
    }
  }

  private Exception handlePollingException(PollingException ex, Exception lastException)
      throws Exception {
    Throwable cause = ex.getCause();
    if (cause == null || cause == ex) {
      throw ex;
    } else if (cause instanceof RetriesExceededException && lastException != null) {
      if (lastException instanceof PollingException) {
        throw handlePollingException((PollingException) lastException, null);
      } else {
        throw lastException;
      }
    } else {
      if (cause instanceof Exception) {
        throw (Exception) cause;
      } else if (cause instanceof Error) {
        throw (Error) cause;
      } else {
        throw new DeploymentException(cause);
      }
    }
  }

  @Transactional
  public boolean executePollingEvent(
      ExternallyControlledPoller<DeploymentMessage, Boolean> pollingStatus,
      DeploymentMessage deploymentMessage) {
    return Optional.ofNullable(pollingStatus.doPollEvent(deploymentMessage)).orElse(false);
  }

  protected abstract SerializableBiPredicate<DeploymentMessage, DeploymentProviderService>
      getPollingFunction();

  protected ExternallyControlledPoller<DeploymentMessage, Boolean> getPoller(
      long timeoutTime) {

    PollingBehaviour<DeploymentMessage, Boolean> pollBehavior =
        new PollingBehaviourImpl(getPollingFunction(), timeoutTime);

    // Only 1 try until the exceptions on which retry will be configurable
    return new ExternallyControlledPoller<>(pollBehavior, 1);
  }

  public static class PollingBehaviourImpl
      extends AbstractPollingBehaviour<DeploymentMessage, Boolean> {

    private static final long serialVersionUID = -5994059867039967783L;
    private SerializableBiPredicate<DeploymentMessage, DeploymentProviderService> pollingFunction;

    public PollingBehaviourImpl(
        SerializableBiPredicate<DeploymentMessage, DeploymentProviderService> pollingFunction,
        long timeoutTime) {
      super(timeoutTime);
      this.pollingFunction = pollingFunction;
    }

    @Override
    public Boolean doPolling(DeploymentMessage deploymentMessage) throws PollingException {
      try {
        DeploymentProviderServiceRegistry registry =
            OrchestratorContextBean.getBean(DeploymentProviderServiceRegistry.class);
        DeploymentProviderService deploymentProviderService =
            registry.getDeploymentProviderService(deploymentMessage.getDeploymentId());
        return pollingFunction.test(deploymentMessage, deploymentProviderService);
      } catch (InterruptedException e) {
        // it will hardly happen, we've already waited for the orchestrator context in the
        // dispatch phase
        Thread.currentThread().interrupt();
        return false;
      }
    }

    @Override
    public boolean pollExit(Boolean pollResult) {
      return Boolean.TRUE.equals(pollResult);
    }

    @Override
    public boolean pollSuccessful(DeploymentMessage params, Boolean pollResult) {
      return pollExit(pollResult);
    }

  }
}
