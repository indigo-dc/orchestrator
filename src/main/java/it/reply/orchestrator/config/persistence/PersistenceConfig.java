package it.reply.orchestrator.config.persistence;

import it.reply.orchestrator.annotation.SpringDefaultProfile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.hibernate4.HibernateExceptionTranslator;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.transaction.UserTransaction;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "it.reply.orchestrator.dal")
public class PersistenceConfig {

  private static final Logger LOG = LoggerFactory.getLogger(PersistenceConfig.class);

  @Bean
  @SpringDefaultProfile
  public UserTransaction userTransaction(JtaTransactionManager transactionManager) {
    return transactionManager.getUserTransaction();
  }

  @Bean
  public HibernateExceptionTranslator hibernateExceptionTranslator() {
    return new HibernateExceptionTranslator();
  }

}
