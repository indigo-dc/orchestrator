package it.reply.orchestrator.config;

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

import it.reply.orchestrator.annotation.SpringTestProfile;
import it.reply.workflowmanager.spring.orchestrator.annotations.WorkflowPersistenceUnit;
import it.reply.workflowmanager.utils.Constants;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.annotation.Resource;
import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement(proxyTargetClass = true)
@SpringTestProfile
public class WorkflowPersistenceConfigTest {

  @Resource
  private Environment environment;

  @Bean(destroyMethod = "shutdown")
  public DataSource workflowDataSource() {
    EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
    EmbeddedDatabase dataSource =
        builder.setType(EmbeddedDatabaseType.H2).addScripts("h2-jbpm-schema.sql").build();
    return dataSource;
  }

  // @Bean(initMethod = "start", destroyMethod = "stop")
  // public Server h2WebServer() throws SQLException {
  // return Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082");
  // }

  @Bean
  @WorkflowPersistenceUnit
  public LocalContainerEntityManagerFactoryBean workflowEntityManagerFactory() {
    LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
    factory.setPersistenceUnitName(Constants.PERSISTENCE_UNIT_NAME);
    factory.setPersistenceXmlLocation("classpath:/META-INF/persistence-test.xml");
    factory.setJtaDataSource(workflowDataSource());
    return factory;
  }
}
