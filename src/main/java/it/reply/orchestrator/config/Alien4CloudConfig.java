package it.reply.orchestrator.config;

import alien4cloud.security.ResourceRoleService;
import alien4cloud.utils.AlienYamlPropertiesFactoryBeanFactory;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;

@Configuration
@ComponentScan(basePackages = { "alien4cloud", "org.elasticsearch.mapping" },
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "alien4cloud.security.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "alien4cloud.audit.*") })
public class Alien4CloudConfig {

  @Bean
  public ResourceRoleService getDummyRoleResourceService() {
    return new ResourceRoleService();
  }

  @Bean(name = { "alienconfig", "elasticsearchConfig" })
  public static YamlPropertiesFactoryBean alienConfig(ResourceLoader resourceLoader)
      throws IOException {
    return AlienYamlPropertiesFactoryBeanFactory.get(resourceLoader);
  }

}
