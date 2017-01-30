package it.reply.orchestrator.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@SpringBootApplication(scanBasePackages = { "it.reply.orchestrator", "it.reply.workflowmanager" })
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(applicationClass, args);
  }

  public static Class<Application> applicationClass = Application.class;

  /**
   * Resolves Alien4Cloud ${...} placeholders within bean definition property values and @Value
   * annotations.
   *
   */
  @Bean
  public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer(
      ResourceLoader resourceLoader) throws IOException {

    PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer =
        new PropertySourcesPlaceholderConfigurer();
    propertyPlaceholderConfigurer
        .setProperties(Alien4CloudConfig.alienConfig(resourceLoader).getObject());
    return propertyPlaceholderConfigurer;
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  // @Bean
  // public ConversionService conversionService() {
  // return new DefaultConversionService();
  // }
}