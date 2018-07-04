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

import com.google.common.base.Strings;

import java.util.Optional;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import liquibase.integration.spring.SpringLiquibase;

import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties.Xa;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.autoconfigure.transaction.PlatformTransactionManagerCustomizer;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.jta.XADataSourceWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Component;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

@Configuration
@EnableJpaRepositories("it.reply.orchestrator.dal.repository")
@EntityScan("it.reply.orchestrator.dal.entity")
public class DatasourceConfig implements BeanClassLoaderAware {

  private ClassLoader classLoader;

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Bean
  @Primary
  @ConfigurationProperties("datasource.orchestrator")
  public DataSourceProperties orchestratorDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean
  @ConfigurationProperties("datasource.workflow")
  public DataSourceProperties workflowDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean
  @Primary
  @ConfigurationProperties("datasource.orchestrator")
  public DataSource dataSource(XADataSourceWrapper wrapper) throws Exception {
    XADataSource xaDataSource = createXaDataSource(orchestratorDataSourceProperties());
    return wrapper.wrapDataSource(xaDataSource);
  }

  @Bean
  @ConfigurationProperties("datasource.workflow")
  public DataSource workflowDataSource(XADataSourceWrapper wrapper) throws Exception {
    XADataSource xaDataSource = createXaDataSource(workflowDataSourceProperties());
    return wrapper.wrapDataSource(xaDataSource);
  }

  @Bean
  @Primary
  @ConfigurationProperties("datasource.orchestrator.liquibase")
  public LiquibaseProperties orchestratorLiquibaseProperties() {
    return new LiquibaseProperties();
  }

  @Bean
  @ConfigurationProperties("datasource.workflow.liquibase")
  public LiquibaseProperties workflowLiquibaseProperties() {
    return new LiquibaseProperties();
  }

  @Bean
  @Primary
  @ConfigurationProperties("datasource.orchestrator.liquibase")
  public SpringLiquibase liquibase(XADataSourceWrapper wrapper) throws Exception {
    return springLiquibase(dataSource(wrapper), orchestratorLiquibaseProperties());
  }

  @Bean
  @ConfigurationProperties("datasource.workflow.liquibase")
  public SpringLiquibase workflowLiquibase(XADataSourceWrapper wrapper) throws Exception {
    return springLiquibase(workflowDataSource(wrapper), workflowLiquibaseProperties());
  }

  private static SpringLiquibase springLiquibase(DataSource dataSource,
      LiquibaseProperties properties) {
    SpringLiquibase liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setChangeLog(properties.getChangeLog());
    liquibase.setContexts(properties.getContexts());
    liquibase.setDefaultSchema(properties.getDefaultSchema());
    liquibase.setDropFirst(properties.isDropFirst());
    liquibase.setShouldRun(properties.isEnabled());
    liquibase.setLabels(properties.getLabels());
    liquibase.setChangeLogParameters(properties.getParameters());
    liquibase.setRollbackFile(properties.getRollbackFile());
    return liquibase;
  }

  @Component
  public static class DatasourceProxyBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName)
        throws BeansException {
      return bean;
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName)
        throws BeansException {
      if (bean instanceof DataSource) {
        DataSource dataSourceBean = (DataSource) bean;
        return ProxyDataSourceBuilder
            .create(dataSourceBean)
            .logQueryBySlf4j(SLF4JLogLevel.TRACE, "it.reply.orchestrator.datasources." + beanName)
            .build();
      }
      return bean;
    }
  }

  private XADataSource createXaDataSource(DataSourceProperties properties)
      throws ClassNotFoundException, LinkageError {
    String className = Optional
        .ofNullable(properties.getXa())
        .map(Xa::getDataSourceClassName)
        .map(String::trim)
        .map(Strings::emptyToNull)
        .orElseGet(() -> DatabaseDriver
            .fromJdbcUrl(properties.determineUrl())
            .getXaDataSourceClassName());
    Assert.state(StringUtils.hasLength(className),
        "No XA DataSource class name specified or inferred");
    XADataSource dataSource = createXaDataSourceInstance(className);
    bindXaProperties(dataSource, properties);
    return dataSource;
  }

  private XADataSource createXaDataSourceInstance(String className)
      throws ClassNotFoundException, LinkageError {
    Class<?> dataSourceClass = ClassUtils.forName(className, this.classLoader);
    Object instance = BeanUtils.instantiate(dataSourceClass);
    Assert.isInstanceOf(XADataSource.class, instance);
    return (XADataSource) instance;
  }

  private void bindXaProperties(XADataSource target, DataSourceProperties properties) {
    MutablePropertyValues values = new MutablePropertyValues()
        .add("user", properties.determineUsername())
        .add("password", properties.determinePassword())
        .add("url", properties.determineUrl());
    Optional
        .ofNullable(properties.getXa())
        .ifPresent(xa -> values.addPropertyValues(xa.getProperties()));
    new RelaxedDataBinder(target).withAlias("user", "username").bind(values);
  }

  @Bean
  public PlatformTransactionManagerCustomizer<JtaTransactionManager>
      jtaTransactionManagerCustomizer() {
    return transactionManager -> transactionManager.setAllowCustomIsolationLevels(true);
  }

}
