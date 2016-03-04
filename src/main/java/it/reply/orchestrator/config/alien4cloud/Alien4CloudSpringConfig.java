package it.reply.orchestrator.config.alien4cloud;

import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import alien4cloud.security.ResourceRoleService;
import alien4cloud.utils.AlienYamlPropertiesFactoryBeanFactory;

@Configuration
@ComponentScan(basePackages = { "alien4cloud", "org.elasticsearch.mapping" }, excludeFilters = {
    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "alien4cloud.security.*"),
    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "alien4cloud.audit.*"),
    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "alien4cloud.ldap.*") })
public class Alien4CloudSpringConfig {

  @Bean
  public ResourceRoleService getDummyRoleResourceService() {
    return new ResourceRoleService();
  }

  // A4C code returns the YamlPropertiesFactoryBean, but that causes warnings at startup
  @Bean(name = { "alienconfig", "elasticsearchConfig" })
  public static Properties alienConfig(BeanFactory beans, ResourceLoader resourceLoader)
      throws IOException {
    return AlienYamlPropertiesFactoryBeanFactory.get(resourceLoader).getObject();
  }

  // @Autowired
  // @Bean
  // public PropertySource properties(ConfigurableListableBeanFactory beanFactory,
  // ResourceLoader resourceLoader) {
  // PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new
  // PropertySourcesPlaceholderConfigurer();
  // YamlPropertiesFactoryBean yaml = AlienYamlPropertiesFactoryBeanFactory.get(resourceLoader);
  //
  // propertySourcesPlaceholderConfigurer.setProperties(yaml.getObject());
  // propertySourcesPlaceholderConfigurer.setIgnoreUnresolvablePlaceholders(true);
  // // properties need to be processed by beanfactory to be accessible after
  // propertySourcesPlaceholderConfigurer.postProcessBeanFactory(beanFactory);
  // return propertySourcesPlaceholderConfigurer.getAppliedPropertySources()
  // .get(PropertySourcesPlaceholderConfigurer.LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME);
  // }

  @Bean
  public PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer(
      ResourceLoader resourceLoader) {
    PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
    propertyPlaceholderConfigurer
        .setProperties(AlienYamlPropertiesFactoryBeanFactory.get(resourceLoader).getObject());
    propertyPlaceholderConfigurer.setIgnoreUnresolvablePlaceholders(true);
    return propertyPlaceholderConfigurer;
  }
}
