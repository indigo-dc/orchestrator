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

import it.reply.orchestrator.config.filters.CustomRequestLoggingFilter;
import it.reply.orchestrator.config.properties.OrchestratorProperties;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(OrchestratorProperties.class)
public class WebAppInitializer {

  /**
   * Register the CustomRequestLoggingFilter.
   * 
   * @return the FilterRegistrationBean
   */
  @Bean
  public FilterRegistrationBean customRequestLoggingFilterRegistration() {
    FilterRegistrationBean registration = new FilterRegistrationBean();
    registration.setFilter(customRequestLoggingFilter());
    registration.addUrlPatterns("/*");
    registration.setName("customWebRequestLoggingFilter");
    registration.setOrder(FilterRegistrationBean.REQUEST_WRAPPER_FILTER_MAX_ORDER - 104);
    return registration;
  }

  @Bean
  public CustomRequestLoggingFilter customRequestLoggingFilter() {
    return new CustomRequestLoggingFilter();
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
    String resolvedPath = applicationContext
        .getEnvironment()
        .resolvePlaceholders("${conf-file-path.oidc}");
    Resource resource = applicationContext.getResource(resolvedPath);
    YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
    factory.setResources(resource);
    return factory;
  }

  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
    return restTemplateBuilder.build();
  }

}
