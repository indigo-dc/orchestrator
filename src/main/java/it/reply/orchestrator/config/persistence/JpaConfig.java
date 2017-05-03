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

package it.reply.orchestrator.config.persistence;

import it.reply.orchestrator.annotation.OrchestratorPersistenceUnit;
import it.reply.workflowmanager.spring.orchestrator.annotations.WorkflowPersistenceUnit;
import it.reply.workflowmanager.utils.Constants;

import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories("it.reply.orchestrator.dal.repository")
public class JpaConfig {

  /**
   * Create a {@Link LocalContainerEntityManagerFactoryBean} for the orchestrator datasource.
   */
  @Bean
  @Primary
  @OrchestratorPersistenceUnit
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(
      EntityManagerFactoryBuilder builder,
      @OrchestratorPersistenceUnit DataSource datasource) {

    return builder
        .dataSource(datasource)
        .persistenceUnit("orchestrator")
        .packages("it.reply.orchestrator.dal.entity")
        .jta(true)
        .build();
  }

  /**
   * Create a {@Link LocalContainerEntityManagerFactoryBean} for the workflow datasource.
   */
  @Bean
  @WorkflowPersistenceUnit
  public LocalContainerEntityManagerFactoryBean workflowEntityManagerFactory(
      EntityManagerFactoryBuilder builder,
      @WorkflowPersistenceUnit DataSource workflowDatasource) {

    LocalContainerEntityManagerFactoryBean factoryBean = builder
        .dataSource(workflowDatasource)
        .packages((String[]) null)
        .persistenceUnit(Constants.PERSISTENCE_UNIT_NAME)
        .jta(true)
        .build();

    // JBPM needs them
    factoryBean.getJpaPropertyMap().put("hibernate.max_fetch_depth", "3");
    factoryBean.getJpaPropertyMap().put("hibernate.id.new_generator_mappings", "false");

    return factoryBean;
  }

}
