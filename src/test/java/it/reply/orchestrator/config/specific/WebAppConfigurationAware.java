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

package it.reply.orchestrator.config.specific;

import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.github.springtestdbunit.DbUnitTestExecutionListener;

import it.reply.orchestrator.annotation.SpringTestProfile;
import it.reply.orchestrator.config.ApplicationConfigTest;
import it.reply.orchestrator.config.WorkflowConfigProducerBean;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import javax.inject.Inject;

@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles(SpringTestProfile.PROFILE_QUALIFIER)
@WebAppConfiguration
@ContextHierarchy({
    @ContextConfiguration(name = "baseContext", classes = ApplicationConfigTest.class) })
@TestPropertySource(locations = { "classpath:application.properties", "classpath:application-test.properties" })
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
    TransactionalTestExecutionListener.class, DbUnitTestExecutionListener.class })
@Transactional
public abstract class WebAppConfigurationAware {

  private static final Logger LOG = LoggerFactory.getLogger(WebAppConfigurationAware.class);

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
