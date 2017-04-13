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

package it.reply.orchestrator.config;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.Resource;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@SpringBootApplication(scanBasePackages = { "it.reply.orchestrator", "it.reply.workflowmanager" })
@PropertySource(value = { "${conf-file-path.im}", "${conf-file-path.marathon}" })
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
      List<YamlPropertiesFactoryBean> factories) throws IOException {

    PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer =
        new PropertySourcesPlaceholderConfigurer();
    propertyPlaceholderConfigurer.setPropertiesArray(factories.stream()
        .map(YamlPropertiesFactoryBean::getObject)
        .collect(Collectors.toList())
        .toArray(new Properties[0]));
    return propertyPlaceholderConfigurer;
  }

  /**
   * Create a YamlPropertiesFactoryBean for OIDC configuration.
   * 
   * @param applicationContext
   *          the application context
   * @return the factory
   */
  @Bean
  public YamlPropertiesFactoryBean oidcYamlFactoryBean(ApplicationContext applicationContext) {
    String resolvedPath =
        applicationContext.getEnvironment().resolvePlaceholders("${conf-file-path.oidc}");
    Resource resource = applicationContext.getResource(resolvedPath);
    YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
    factory.setResources(new Resource[] { resource });
    return factory;
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
