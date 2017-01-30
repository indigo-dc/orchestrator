package it.reply.orchestrator.config;

import com.github.springtestdbunit.annotation.DatabaseSetup;

import it.reply.orchestrator.annotation.OrchestratorPersistenceUnit;
import it.reply.orchestrator.annotation.SpringTestProfile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

import java.util.Properties;

import javax.annotation.Resource;
import javax.naming.NamingException;
import javax.sql.DataSource;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;

@Configuration
@DatabaseSetup("database-init.xml")
@SpringTestProfile
public class PersistenceConfigTest {

  private static final Logger LOG = LoggerFactory.getLogger(PersistenceConfigTest.class);

  private static final String ENTITY_MANAGER_PACKAGE_TO_SCAN = "entitymanager.packages.to.scan";
  private static final String HIBERNATE_HBM2DDL_AUTO = "hibernate.hbm2ddl.auto";
  private static final String HIBERNATE_DIALECT = "hibernate.dialect";
  private static final String HIBERNATE_TRANSACTION_JTA_PLATFORM =
      "hibernate.transaction.jta.platform";

  @Resource
  private Environment env;

  @Bean(destroyMethod = "shutdown")
  @Primary
  @OrchestratorPersistenceUnit
  public DataSource dataSource() {
    EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
    EmbeddedDatabase dataSource = builder.setType(EmbeddedDatabaseType.H2).build();
    return dataSource;
  }

  @Bean
  @OrchestratorPersistenceUnit
  public LocalContainerEntityManagerFactoryBean entityManagerFactory() throws NamingException {
    LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();

    HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
    vendorAdapter.setGenerateDdl(Boolean.TRUE);
    vendorAdapter.setShowSql(Boolean.TRUE);

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

    return factory;
  }

  @Bean
  public bitronix.tm.Configuration btmConfig() {
    return TransactionManagerServices.getConfiguration();
  }

  @Bean(destroyMethod = "shutdown")
  public BitronixTransactionManager bitronixTransactionManager() {
    btmConfig();
    return TransactionManagerServices.getTransactionManager();
  }

  @Bean
  public PlatformTransactionManager transactionManager() {
    return new JtaTransactionManager(bitronixTransactionManager(), bitronixTransactionManager());
  }
}
