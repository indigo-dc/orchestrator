/*
 * Copyright © 2015-2020 Santer Reply S.p.A.
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

import org.apache.ignite.Ignite;
import org.flowable.engine.impl.asyncexecutor.message.AbstractMessageBasedJobManager;
import org.flowable.job.api.HistoryJob;
import org.flowable.job.api.JobInfo;
import org.springframework.stereotype.Component;

@Component
public class MessageBasedJobManager extends AbstractMessageBasedJobManager {

  static final String JOBS_TOPIC = "JobsTopic";
  static final String HISTORY_JOBS_TOPIC = "HistoryJobsTopic";

  private Ignite ignite;

  public MessageBasedJobManager(Ignite ignite) {
    this.ignite = ignite;
  }

  @Override
  protected void sendMessage(final JobInfo job) {
    String topic = (job instanceof HistoryJob) ? HISTORY_JOBS_TOPIC : JOBS_TOPIC;
    ignite.message().sendOrdered(topic, job.getId(), 0);
  }

}
