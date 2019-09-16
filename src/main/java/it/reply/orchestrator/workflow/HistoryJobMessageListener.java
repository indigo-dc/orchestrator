/*
 * Copyright © 2015-2019 Santer Reply S.p.A.
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

package it.reply.orchestrator.workflow;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

import org.apache.ignite.Ignite;
import org.apache.ignite.lang.IgniteBiPredicate;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.common.impl.interceptor.EngineConfigurationConstants;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.asyncexecutor.AsyncRunnableExecutionExceptionHandler;
import org.flowable.job.service.impl.asyncexecutor.ExecuteAsyncRunnable;
import org.flowable.job.service.impl.asyncexecutor.UnacquireAsyncHistoryJobExceptionHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HistoryJobMessageListener implements IgniteBiPredicate<UUID, String> {

  private static final long serialVersionUID = 1L;

  private transient JobServiceConfiguration jobServiceConfiguration;
  private transient AsyncRunnableExecutionExceptionHandler exceptionHandler;

  /**
   * Creates a new HistoryJobMessageListener.
   *
   * @param processEngineConfiguration
   *          the ProcessEngineConfiguration to use
   * @param ignite
   *          the Ignite instance to use
   */
  public HistoryJobMessageListener(ProcessEngineConfiguration processEngineConfiguration,
      Ignite ignite) {
    this.exceptionHandler = new UnacquireAsyncHistoryJobExceptionHandler();
    this.jobServiceConfiguration = (JobServiceConfiguration) processEngineConfiguration
        .getServiceConfigurations()
        .get(EngineConfigurationConstants.KEY_JOB_SERVICE_CONFIG);
    ignite.message().localListen(MessageBasedJobManager.HISTORY_JOBS_TOPIC, this);
  }

  @Override
  public boolean apply(UUID nodeId, String jobId) {
    try {
      new ExecuteAsyncRunnable(jobId, jobServiceConfiguration,
          jobServiceConfiguration.getHistoryJobEntityManager(), exceptionHandler).run();
    } catch (RuntimeException e) {
      LOG.error("Exception when handling message from job queue", e);
    }
    return true;
  }

}
