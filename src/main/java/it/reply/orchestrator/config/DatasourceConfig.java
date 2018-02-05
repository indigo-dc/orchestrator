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

import org.springframework.beans.BeanUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties.Xa;
import org.springframework.boot.autoconfigure.transaction.PlatformTransactionManagerCustomizer;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.jta.XADataSourceWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.Optional;

import javax.sql.DataSource;
import javax.sql.XADataSource;

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
  public DataSource dataSource(XADataSourceWrapper wrapper) throws Exception {
    XADataSource xaDataSource = createXaDataSource(orchestratorDataSourceProperties());
    return wrapper.wrapDataSource(xaDataSource);
  }

  @Bean
  public DataSource workflowDataSource(XADataSourceWrapper wrapper) throws Exception {
    XADataSource xaDataSource = createXaDataSource(workflowDataSourceProperties());
    return wrapper.wrapDataSource(xaDataSource);
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
