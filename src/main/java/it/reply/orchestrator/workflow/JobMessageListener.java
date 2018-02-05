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

package it.reply.orchestrator.workflow;

import lombok.extern.slf4j.Slf4j;

import org.apache.ignite.Ignite;
import org.apache.ignite.lang.IgniteBiPredicate;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.common.impl.interceptor.EngineConfigurationConstants;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.asyncexecutor.ExecuteAsyncRunnable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class JobMessageListener implements IgniteBiPredicate<UUID, String> {

  private static final long serialVersionUID = -4399331241269448854L;

  private transient JobServiceConfiguration jobServiceConfiguration;

  public JobMessageListener(ProcessEngineConfiguration processEngineConfiguration, Ignite ignite) {
    this.jobServiceConfiguration = (JobServiceConfiguration) processEngineConfiguration
        .getServiceConfigurations()
        .get(EngineConfigurationConstants.KEY_JOB_SERVICE_CONFIG);
    ignite.message().localListen(MessageBasedJobManager.JOBS_TOPIC, this);
  }

  @Override
  public boolean apply(UUID nodeId, String jobId) {
    try {
      new ExecuteAsyncRunnable(jobId, jobServiceConfiguration,
          jobServiceConfiguration.getJobEntityManager(), null).run();
    } catch (RuntimeException e) {
      LOG.error("Exception when handling message from job queue", e);
    }
    return true;
  }

}
