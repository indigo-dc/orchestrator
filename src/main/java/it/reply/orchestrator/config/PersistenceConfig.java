package it.reply.orchestrator.config;

import it.reply.workflowmanager.spring.orchestrator.annotations.WorkflowPersistenceUnit;
import it.reply.workflowmanager.utils.Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;
import org.springframework.orm.hibernate4.HibernateExceptionTranslator;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;

import java.util.Properties;

import javax.annotation.Resource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "it.reply.orchestrator.dal")
@PropertySource(value = { "classpath:application.properties" })
public class PersistenceConfig {

  private static final Logger LOG = LoggerFactory.getLogger(PersistenceConfig.class);

  private static final String ENTITY_MANAGER_PACKAGE_TO_SCAN = "entitymanager.packages.to.scan";
  private static final String HIBERNATE_HBM2DDL_AUTO = "hibernate.hbm2ddl.auto";
  private static final String HIBERNATE_SHOW_SQL = "hibernate.show_sql";
  private static final String HIBERNATE_DIALECT = "hibernate.dialect";
  private static final String HIBERNATE_TRANSACTION_JTA_PLATFORM =
      "hibernate.transaction.jta.platform";

  @Resource
  private Environment env;

  @Bean
  public DataSource dataSource() throws NamingException {
    Context ctx = new InitialContext();
    return (DataSource) ctx.lookup("java:jboss/datasources/orchestrator");
  }

  @Bean
  public PlatformTransactionManager transactionManager() {
    JtaTransactionManager tm = new JtaTransactionManager();
    return tm;
  }

  @Bean
  public UserTransaction userTransaction() {
    return ((JtaTransactionManager) transactionManager()).getUserTransaction();
  }

  /**
   * Creates an {@link EntityManagerFactory}.
   */
  @Bean
  public LocalContainerEntityManagerFactoryBean entityManagerFactory() throws NamingException {
    LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();

    HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
    vendorAdapter.setGenerateDdl(Boolean.TRUE);
    vendorAdapter.setShowSql(Boolean.valueOf(this.env.getProperty(HIBERNATE_SHOW_SQL)));

    factory.setJtaDataSource(dataSource());
    factory.setJpaVendorAdapter(vendorAdapter);
    factory.setPackagesToScan(this.env.getProperty(ENTITY_MANAGER_PACKAGE_TO_SCAN));

    LOG.debug(env.getProperty(ENTITY_MANAGER_PACKAGE_TO_SCAN));
    LOG.debug(env.getProperty(HIBERNATE_HBM2DDL_AUTO));
    LOG.debug(env.getProperty(HIBERNATE_DIALECT));
    LOG.debug(env.getProperty(HIBERNATE_TRANSACTION_JTA_PLATFORM));

    Properties jpaProperties = new Properties();
    jpaProperties.put(HIBERNATE_HBM2DDL_AUTO, env.getProperty(HIBERNATE_HBM2DDL_AUTO));
    jpaProperties.put(HIBERNATE_DIALECT, env.getProperty(HIBERNATE_DIALECT));
    jpaProperties.put(HIBERNATE_TRANSACTION_JTA_PLATFORM,
        env.getProperty(HIBERNATE_TRANSACTION_JTA_PLATFORM));
    factory.setJpaProperties(jpaProperties);

    factory.afterPropertiesSet();
    factory.setLoadTimeWeaver(new InstrumentationLoadTimeWeaver());
    return factory;
  }

  @Bean
  public HibernateExceptionTranslator hibernateExceptionTranslator() {
    return new HibernateExceptionTranslator();
  }

  @Bean
  public DataSource workflowDataSource() throws NamingException {
    Context ctx = new InitialContext();
    return (DataSource) ctx.lookup("java:jboss/datasources/WorkflowManager/JBPM-DS");
  }

  /**
   * Create a {@Link LocalContainerEntityManagerFactoryBean} for the jbpm workflow.
   */
  @Bean
  @WorkflowPersistenceUnit
  public LocalContainerEntityManagerFactoryBean workflowEntityManagerFactory()
      throws NamingException {
    LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
    factory.setPersistenceUnitName(Constants.PERSISTENCE_UNIT_NAME);
    factory.setPersistenceXmlLocation("classpath:/META-INF/persistence.xml");
    factory.setJtaDataSource(workflowDataSource());
    return factory;
  }
}
