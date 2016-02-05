package it.reply.orchestrator.config;

import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.github.springtestdbunit.DbUnitTestExecutionListener;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import javax.inject.Inject;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { ApplicationConfig.class,
    WebAppInitializer.class, WebMvcConfig.class, PersistenceConfigTest.class,
    WorklfowPersistenceConfigTest.class })
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
    TransactionalTestExecutionListener.class, DbUnitTestExecutionListener.class })
public abstract class WebAppConfigurationAware {

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

}
