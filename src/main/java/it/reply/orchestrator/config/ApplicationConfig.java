package it.reply.orchestrator.config;

import it.reply.orchestrator.config.security.WebSecurityConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.EnableAsync;

import java.io.IOException;

@Configuration
@EnableAsync
@Import(WebSecurityConfig.class)
public class ApplicationConfig {

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
    propertyPlaceholderConfigurer.setProperties(Alien4CloudConfig.alienConfig(resourceLoader));
    return propertyPlaceholderConfigurer;
  }

  @Bean
  public ConversionService conversionService() {
    return new DefaultConversionService();
  }
}