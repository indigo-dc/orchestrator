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

package it.reply.orchestrator.config;

import it.reply.orchestrator.workflow.CustomFailedJobCommandFactory;

import java.io.IOException;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.FlowableMailProperties;
import org.flowable.spring.boot.FlowableProperties;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.idm.FlowableIdmProperties;
import org.flowable.spring.boot.process.FlowableProcessProperties;
import org.flowable.spring.boot.process.Process;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class WorkflowConfig extends ProcessEngineAutoConfiguration {

  public WorkflowConfig(FlowableProperties flowableProperties,
      FlowableProcessProperties processProperties, FlowableIdmProperties idmProperties,
      FlowableMailProperties mailProperties) {
    super(flowableProperties, processProperties, idmProperties, mailProperties);
  }

  /**
   * Generates a SpringProcessEngineConfiguration.
   * 
   * @param dataSource
   *          the Datasource to use
   * @param platformTransactionManager
   *          the PlatformTransactionManager to use
   * @param asyncExecutorProvider
   *          the AsyncExecutor to use
   * @param entityManagerFactory
   *          the EntityManagerFactory to use
   * @return the generated SpringProcessEngineConfiguration
   * @throws IOException
   *           when I/O exception of some sort has occurred during initialization
   */
  @Bean
  public SpringProcessEngineConfiguration springProcessEngineConfiguration(
      @Qualifier("workflowDataSource") DataSource dataSource,
      PlatformTransactionManager platformTransactionManager,
      @Process ObjectProvider<AsyncExecutor> asyncExecutorProvider,
      EntityManagerFactory entityManagerFactory) throws IOException {
    SpringProcessEngineConfiguration configuration =
        super.springProcessEngineConfiguration(dataSource, platformTransactionManager,
            asyncExecutorProvider);

    configuration.setJpaEntityManagerFactory(entityManagerFactory);
    configuration.setJpaHandleTransaction(false);
    configuration.setJpaCloseEntityManager(false);
    configuration.setFailedJobCommandFactory(new CustomFailedJobCommandFactory());
    configuration.setAsyncExecutorMessageQueueMode(true);
    configuration.setAsyncHistoryExecutorMessageQueueMode(true);

    return configuration;
  }

}
