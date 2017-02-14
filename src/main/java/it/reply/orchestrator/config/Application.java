package it.reply.orchestrator.config;

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