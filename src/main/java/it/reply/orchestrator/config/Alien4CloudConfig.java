/*
 * Copyright © 2015-2019 Santer Reply S.p.A.
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

package it.reply.orchestrator.config;

import alien4cloud.security.ResourceRoleService;
import alien4cloud.utils.AlienYamlPropertiesFactoryBeanFactory;

import it.reply.orchestrator.utils.CommonUtils;

import java.util.Properties;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.MapPropertySource;

@Configuration
@ComponentScan(basePackages = { "alien4cloud", "org.elasticsearch.mapping" },
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "alien4cloud.security.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "alien4cloud.audit.*") })
@Slf4j
public class Alien4CloudConfig {

  public static final String ELASTICSEARCH_CONFIG_BEAN_NAME = "elasticsearchConfig";
  public static final String ALIEN_CONFIG_BEAN_NAME = "alienconfig";

  @Bean
  public ResourceRoleService getDummyRoleResourceService() {
    return new ResourceRoleService();
  }

  @Configuration
  public static class AlienBeanPostProcessor implements BeanPostProcessor {

    private static final String PREFIX = "alien4cloud.";

    @Autowired
    private AbstractEnvironment environment;

    protected static void postProcess(Properties properties,
        AbstractEnvironment environment) {
      if (!properties.isEmpty()) {
        LOG.warn("Alien4 cloud Properties specified by {} will be overridden",
            AlienYamlPropertiesFactoryBeanFactory.ALIEN_CONFIGURATION_YAML);
      }
      CommonUtils
          .spliteratorToStream(environment.getPropertySources().spliterator())
          .filter(MapPropertySource.class::isInstance)
          .flatMap(propertySource -> ((MapPropertySource) propertySource)
              .getSource()
              .entrySet()
              .stream())
          .filter(entry -> entry.getKey().startsWith(PREFIX))
          .collect(Collectors.toMap(entry -> entry.getKey().substring(PREFIX.length()),
              entry -> entry.getValue(), (k1, k2) -> k1))
          .forEach((key, value) -> properties.put(key, value));
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName)
        throws BeansException {
      return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName)
        throws BeansException {
      if (ALIEN_CONFIG_BEAN_NAME.equals(beanName)) {
        postProcess((Properties) bean, environment);
      }
      return bean;
    }

  }

  @Bean(name = { ALIEN_CONFIG_BEAN_NAME, ELASTICSEARCH_CONFIG_BEAN_NAME })
  public YamlPropertiesFactoryBean alienConfig(ApplicationContext applicationContext) {
    return AlienYamlPropertiesFactoryBeanFactory.get(applicationContext);
  }

  /**
   * Creates a PropertySourcesPlaceholderConfigurer from the Alien4Cloud Properties.
   * 
   * @param alienProperties
   *          the Alien4Cloud properties
   * @param environment
   *          The Spring injected Environment
   * @return the PropertySourcesPlaceholderConfigurer
   */
  @Bean
  public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer(
      @Qualifier(ALIEN_CONFIG_BEAN_NAME) Properties alienProperties,
      AbstractEnvironment environment) {

    AlienBeanPostProcessor.postProcess(alienProperties, environment);

    PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer =
        new PropertySourcesPlaceholderConfigurer();
    propertyPlaceholderConfigurer.setPropertiesArray(alienProperties);
    return propertyPlaceholderConfigurer;
  }
}
