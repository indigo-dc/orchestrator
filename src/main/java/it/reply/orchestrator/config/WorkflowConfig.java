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

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkflowConfig {

  @Autowired
  @Qualifier("workflowDataSource")
  DataSource dataSource;

  @Bean
  EngineConfigurationConfigurer<SpringProcessEngineConfiguration> workflowConfigurer(
    @Qualifier("workflowDataSource") DataSource dataSource,
    @Qualifier("workflowLiquibase") SpringLiquibase workflowLiquibase,
    EntityManagerFactory entityManagerFactory
  ) {
    return engineConfiguration -> {
      engineConfiguration.setDataSource(dataSource);
      engineConfiguration.setJpaEntityManagerFactory(entityManagerFactory);
      engineConfiguration.setJpaHandleTransaction(false);
      engineConfiguration.setJpaCloseEntityManager(false);
      engineConfiguration.setFailedJobCommandFactory(new CustomFailedJobCommandFactory());
      engineConfiguration.setIdGenerator(new StrongSequentialUuidGenerator());
    };
  }
}
