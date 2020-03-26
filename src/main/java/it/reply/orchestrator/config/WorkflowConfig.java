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

package it.reply.orchestrator.config;

import it.reply.orchestrator.dal.util.StrongSequentialUuidGenerator;
import it.reply.orchestrator.workflow.CustomFailedJobCommandFactory;

import java.io.IOException;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.spring.AutoDeploymentStrategy;
import org.flowable.engine.ProcessEngine;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.FlowableAutoDeploymentProperties;
import org.flowable.spring.boot.FlowableHttpProperties;
import org.flowable.spring.boot.FlowableMailProperties;
import org.flowable.spring.boot.FlowableProperties;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.app.FlowableAppProperties;
import org.flowable.spring.boot.eventregistry.FlowableEventRegistryProperties;
import org.flowable.spring.boot.idm.FlowableIdmProperties;
import org.flowable.spring.boot.process.FlowableProcessProperties;
import org.flowable.spring.boot.process.Process;
import org.flowable.spring.boot.process.ProcessAsync;
import org.flowable.spring.boot.process.ProcessAsyncHistory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class WorkflowConfig extends ProcessEngineAutoConfiguration {

  public WorkflowConfig(FlowableProperties flowableProperties,
      FlowableProcessProperties processProperties, FlowableAppProperties appProperties,
      FlowableIdmProperties idmProperties, FlowableEventRegistryProperties eventProperties,
      FlowableMailProperties mailProperties, FlowableHttpProperties httpProperties,
      FlowableAutoDeploymentProperties autoDeploymentProperties) {
    super(flowableProperties, processProperties, appProperties, idmProperties, eventProperties,
        mailProperties, httpProperties, autoDeploymentProperties);
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
  @DependsOn("workflowLiquibase")
  public SpringProcessEngineConfiguration springProcessEngineConfiguration(
      @Qualifier("workflowDataSource") DataSource dataSource,
      PlatformTransactionManager platformTransactionManager,
      @Process ObjectProvider<IdGenerator> processIdGenerator,
      ObjectProvider<IdGenerator> globalIdGenerator,
      @ProcessAsync ObjectProvider<AsyncExecutor> asyncExecutorProvider,
      @ProcessAsyncHistory ObjectProvider<AsyncExecutor> asyncHistoryExecutorProvider,
      ObjectProvider<List<AutoDeploymentStrategy<ProcessEngine>>> processEngineAutoDeploymentStrategies,
      EntityManagerFactory entityManagerFactory) throws IOException {
    SpringProcessEngineConfiguration configuration =
        super.springProcessEngineConfiguration(dataSource, platformTransactionManager,
            processIdGenerator, globalIdGenerator, asyncExecutorProvider,
            asyncHistoryExecutorProvider, processEngineAutoDeploymentStrategies);

    configuration.setJpaEntityManagerFactory(entityManagerFactory);
    configuration.setJpaHandleTransaction(false);
    configuration.setJpaCloseEntityManager(false);
    configuration.setFailedJobCommandFactory(new CustomFailedJobCommandFactory());
    configuration.setAsyncExecutorMessageQueueMode(true);
    configuration.setAsyncHistoryExecutorMessageQueueMode(true);
    configuration.setIdGenerator(new StrongSequentialUuidGenerator());

    return configuration;
  }

}
