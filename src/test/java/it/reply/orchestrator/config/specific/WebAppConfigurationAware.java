package it.reply.orchestrator.config.specific;

import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.github.springtestdbunit.DbUnitTestExecutionListener;

import it.reply.orchestrator.config.ApplicationConfigTest;
import it.reply.orchestrator.config.PersistenceConfigTest;
import it.reply.orchestrator.config.WebAppInitializer;
import it.reply.orchestrator.config.WebMvcConfig;
import it.reply.orchestrator.config.WorkflowConfigProducerBean;
import it.reply.orchestrator.config.WorklfowPersistenceConfigTest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import javax.inject.Inject;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextHierarchy({
    @ContextConfiguration(name = "baseContext",
        classes = { ApplicationConfigTest.class, WebAppInitializer.class, WebMvcConfig.class,
            PersistenceConfigTest.class, WorklfowPersistenceConfigTest.class }),
    @ContextConfiguration(name = "workflowContext",
        classes = { WebAppConfigurationAware.Config.class }) })
@TestPropertySource(locations = { "classpath:application-test.properties" })
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
    TransactionalTestExecutionListener.class, DbUnitTestExecutionListener.class })
public abstract class WebAppConfigurationAware {

  private static final Logger LOG = LogManager.getLogger(WebAppConfigurationAware.class);

  @Inject
  protected WebApplicationContext wac;
  protected MockMvc mockMvc;

  @BeforeClass
  public static void generalSetup() {
    System.setProperty(javax.naming.Context.INITIAL_CONTEXT_FACTORY,
        "bitronix.tm.jndi.BitronixInitialContextFactory");
  }

  @AfterClass
  public static void generalCleanup() {
    System.clearProperty(javax.naming.Context.INITIAL_CONTEXT_FACTORY);
  }

  @Before
  public void before() {
    this.mockMvc = webAppContextSetup(this.wac).build();
  }

  @Configuration
  static class Config {
    @Bean
    @Primary
    public WorkflowConfigProducerBean produceWorkflowConfigProducerBean() {
      return new WorkflowConfigProducerBean() {

        @Override
        public int getExecutorServiceThreadPoolSize() {
          // Disable jBPM Executor Service during Unit Tests (to avoid transaction/concurrency
          // issues)
          LOG.warn(
              "Disable jBPM Executor Service during Unit Tests (to avoid transaction/concurrency issues)");
          return 0;
        }

      };
    }
  }
}
