package it.reply.orchestrator.config;

import java.util.Properties;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;

import com.github.springtestdbunit.annotation.DatabaseSetup;

@Configuration
@PropertySource(value = { "classpath:application-test.properties" })
@DatabaseSetup("database-init.xml")
public class PersistenceConfigTest {

  @Resource
  private Environment environment;

  @Bean
  public DataSource dataSource() {
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName(environment.getRequiredProperty("db.driver"));
    dataSource.setUrl(environment.getRequiredProperty("db.url"));
    dataSource.setUsername(environment.getRequiredProperty("db.username"));
    dataSource.setPassword(environment.getRequiredProperty("db.password"));
    return dataSource;
  }

  @Bean
  public LocalSessionFactoryBean sessionFactory() {
    LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
    sessionFactory.setDataSource(dataSource());
    sessionFactory
        .setPackagesToScan(environment.getRequiredProperty("entitymanager.packages.to.scan"));
    sessionFactory.setHibernateProperties(hibernateProperties());
    return sessionFactory;
  }

  private Properties hibernateProperties() {
    Properties properties = new Properties();
    properties.put("hibernate.dialect", environment.getRequiredProperty("hibernate.dialect"));
    properties.put("hibernate.show_sql", environment.getRequiredProperty("hibernate.show_sql"));
    properties.put("hibernate.hbm2ddl.auto",
        environment.getRequiredProperty("hibernate.hbm2ddl.auto"));
    return properties;
  }

  @Bean
  @Autowired
  public HibernateTransactionManager transactionManager(SessionFactory s) {
    HibernateTransactionManager txManager = new HibernateTransactionManager();
    txManager.setSessionFactory(s);
    return txManager;

  }
}
