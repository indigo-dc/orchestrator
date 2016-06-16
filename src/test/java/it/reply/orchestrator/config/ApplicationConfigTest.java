package it.reply.orchestrator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@ComponentScan(basePackages = { "it.reply.orchestrator", "it.reply.workflowmanager" },
    excludeFilters = { @ComponentScan.Filter(type = FilterType.REGEX,
        pattern = "it.reply.orchestrator.config.specific.*") })
public class ApplicationConfigTest {

  @Bean
  public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
    return new PropertySourcesPlaceholderConfigurer();
  }

}