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

package it.reply.orchestrator.service.deployment.providers.factory;

import it.reply.orchestrator.config.properties.MesosFrameworkProperties;
import it.reply.orchestrator.config.properties.MesosProperties;
import it.reply.orchestrator.config.properties.MesosProperties.MesosInstanceProperties;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.exception.service.DeploymentException;

import lombok.AllArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public abstract class MesosFrameworkClientFactory<V extends MesosFrameworkProperties, T> {

  private MesosProperties mesosProperties;

  protected MesosInstanceProperties getInstanceProperties(Deployment deployment) {
    String cloudProviderName = deployment.getCloudProviderName();
    return mesosProperties
        .getInstance(cloudProviderName)
        .orElseThrow(() -> new DeploymentException(String
            .format("No %s instance available for cloud provider %s", getFrameworkName(),
                cloudProviderName)));
  }

  public abstract V getFrameworkProperties(Deployment deployment);

  protected abstract String getFrameworkName();

  public abstract T build(V frameworkProperties);

  public abstract T build(Deployment deployment);

}
