/*
 * Copyright © 2015-2020 Santer Reply S.p.A.
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

import it.reply.orchestrator.annotation.ServiceVersion;
import it.reply.orchestrator.config.properties.CmdbProperties;
import it.reply.orchestrator.config.properties.SlamProperties;
import it.reply.orchestrator.service.CmdbService;
import it.reply.orchestrator.service.SlamService;
import java.util.Arrays;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    SlamProperties.class,
    CmdbProperties.class
})
public class DependenciesConfig {

  @Autowired
  private ApplicationContext applicationContext;

  private <R> R findVersion(Class<R> clazz, String version) {
    return applicationContext
        .getBeansOfType(clazz)
        .values()
        .stream()
        .filter(bean -> Arrays
            .stream(bean.getClass().getAnnotations())
            .filter(ServiceVersion.class::isInstance)
            .map(ServiceVersion.class::cast)
            .anyMatch(annotation -> version.equals(annotation.value()))
        ).findFirst()
        .orElseThrow(() -> new BeanCreationException("Unable to find bean of class "
            + clazz.getName() + " with service version " + version));
  }

  @Bean
  public SlamService slamService(SlamProperties slamProperties) {
    return this.findVersion(SlamService.class, slamProperties.getServiceVersion());
  }

  @Bean
  public CmdbService cmdbService(CmdbProperties cmdbProperties) {
    return this.findVersion(CmdbService.class, cmdbProperties.getServiceVersion());
  }

}
