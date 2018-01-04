/*
 * Copyright © 2015-2018 Santer Reply S.p.A.
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

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

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
  public YamlPropertiesFactoryBean alienConfig(ResourceLoader resourceLoader) {
    return AlienYamlPropertiesFactoryBeanFactory.get(resourceLoader);
  }

  /**
   * Resolves Alien4Cloud ${...} placeholders within bean definition property values and @Value
   * annotations.
   *
   */
  @Bean
  public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer(
      List<YamlPropertiesFactoryBean> factories) throws IOException {

    PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer =
        new PropertySourcesPlaceholderConfigurer();
    propertyPlaceholderConfigurer.setPropertiesArray(factories
        .stream()
        .map(YamlPropertiesFactoryBean::getObject)
        .collect(Collectors.toList())
        .toArray(new Properties[0]));
    return propertyPlaceholderConfigurer;
  }
}
