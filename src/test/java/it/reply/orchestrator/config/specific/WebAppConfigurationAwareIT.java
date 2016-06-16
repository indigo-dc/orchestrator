package it.reply.orchestrator.config.specific;

import it.reply.orchestrator.IntegrationTest;

import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
// @ContextHierarchy({ @ContextConfiguration(name = "workflowContext",
// classes = { WebAppConfigurationAwareIT.Config.class }, inheritLocations = false) })
public abstract class WebAppConfigurationAwareIT extends WebAppConfigurationAware {

  /**
   * The override is not working - enable in the future.
   */
  // private static final Logger LOG = LogManager.getLogger(WebAppConfigurationAware.class);
  //
  // @Configuration
  // static class Config {
  // @Bean
  // @Primary
  // public WorkflowConfigProducerBean produceWorkflowConfigProducerBean() {
  // return new WorkflowConfigProducerBean() {
  //
  // @Override
  // public int getExecutorServiceThreadPoolSize() {
  // // Enable jBPM Executor Service during Integration Tests
  // LOG.warn("Enable jBPM Executor Service during Integration Tests");
  // return 2;
  // }
  //
  // };
  // }
  // }
}
