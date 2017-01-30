package it.reply.orchestrator.config.persistence;

import it.reply.orchestrator.annotation.OrchestratorPersistenceUnit;
import it.reply.orchestrator.annotation.SpringDefaultProfile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import java.util.Properties;

import javax.annotation.Resource;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@Configuration
@SpringDefaultProfile
public class OrchestratorPersistenceConfig {

  private static final Logger LOG = LoggerFactory.getLogger(OrchestratorPersistenceConfig.class);

  private static final String ENTITY_MANAGER_PACKAGE_TO_SCAN = "entitymanager.packages.to.scan";
  private static final String HIBERNATE_HBM2DDL_AUTO = "hibernate.hbm2ddl.auto";
  private static final String HIBERNATE_SHOW_SQL = "hibernate.show_sql";
  private static final String HIBERNATE_DIALECT = "hibernate.dialect";
  private static final String HIBERNATE_TRANSACTION_JTA_PLATFORM =
      "hibernate.transaction.jta.platform";

  @Resource
  private Environment env;

  @Bean
  @OrchestratorPersistenceUnit
  @Primary
  public DataSource orchestratorDataSource() {
    JndiDataSourceLookup dataSourceLookup = new JndiDataSourceLookup();
    return dataSourceLookup.getDataSource("java:jboss/datasources/orchestrator");
  }

  /**
   * Creates an {@link EntityManagerFactory}.
   */
  @Bean
  @OrchestratorPersistenceUnit
  public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
    LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();

    HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
    vendorAdapter.setGenerateDdl(Boolean.TRUE);
    vendorAdapter.setShowSql(Boolean.valueOf(this.env.getProperty(HIBERNATE_SHOW_SQL)));

    factory.setJtaDataSource(orchestratorDataSource());
    factory.setJpaVendorAdapter(vendorAdapter);
    factory.setPackagesToScan(this.env.getProperty(ENTITY_MANAGER_PACKAGE_TO_SCAN));
    factory.setPersistenceUnitName("orchestrator");

    LOG.debug(env.getProperty(ENTITY_MANAGER_PACKAGE_TO_SCAN));
    LOG.debug(env.getProperty(HIBERNATE_HBM2DDL_AUTO));
    LOG.debug(env.getProperty(HIBERNATE_DIALECT));
    LOG.debug(env.getProperty(HIBERNATE_TRANSACTION_JTA_PLATFORM));

    Properties jpaProperties = new Properties();
    jpaProperties.put(HIBERNATE_HBM2DDL_AUTO, env.getProperty(HIBERNATE_HBM2DDL_AUTO));
    jpaProperties.put(HIBERNATE_DIALECT, env.getProperty(HIBERNATE_DIALECT));
    jpaProperties.put(HIBERNATE_TRANSACTION_JTA_PLATFORM,
        env.getProperty(HIBERNATE_TRANSACTION_JTA_PLATFORM));
    jpaProperties.put("hibernate.transaction.manager_lookup_class",
        "org.hibernate.transaction.JBossTransactionManagerLookup");
    factory.setJpaProperties(jpaProperties);

    return factory;
  }
}
