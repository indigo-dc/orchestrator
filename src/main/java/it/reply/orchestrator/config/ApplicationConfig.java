package it.reply.orchestrator.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.Properties;

import alien4cloud.utils.AlienYamlPropertiesFactoryBeanFactory;

@Configuration
@EnableAsync
public class ApplicationConfig {

  // @Bean
  // public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer(
  // ResourceLoader resourceLoader) {
  // PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer = new
  // PropertySourcesPlaceholderConfigurer();
  // propertyPlaceholderConfigurer.setIgnoreUnresolvablePlaceholders(true);
  // return new PropertySourcesPlaceholderConfigurer();
  // }

}