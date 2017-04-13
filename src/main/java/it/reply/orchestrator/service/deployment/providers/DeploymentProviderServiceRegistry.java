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

package it.reply.orchestrator.service.deployment.providers;

import it.reply.orchestrator.annotation.DeploymentProviderQualifier;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.exception.OrchestratorException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.EnumMap;
import java.util.Optional;

@Service
public class DeploymentProviderServiceRegistry {

  private EnumMap<DeploymentProvider, DeploymentProviderService> providers =
      new EnumMap<>(DeploymentProvider.class);

  /**
   * Creates the DeploymentProviderServiceRegistry from all the DeploymentProviderServices
   * registered in the Spring {@link ApplicationContext}.
   * 
   * @param services
   *          the registered DeploymentProviderServices
   */
  @Autowired
  public DeploymentProviderServiceRegistry(DeploymentProviderService[] services) {
    for (DeploymentProviderService service : services) {

      DeploymentProviderQualifier annotation =
          service.getClass().getAnnotation(DeploymentProviderQualifier.class);
      Assert.notNull(annotation,
          String.format("DeploymentProviderService %s not annotated with annotation @%s",
              service.getClass(), DeploymentProviderQualifier.class));

      DeploymentProvider deploymentProvider = annotation.value();
      Assert.notNull(deploymentProvider);

      providers.put(deploymentProvider, service);
    }
  }

  public DeploymentProviderService getDeploymentProviderService(Deployment deployment) {
    return getDeploymentProviderService(deployment.getDeploymentProvider());
  }

  /**
   * Returns the {@link DeploymentProviderService} corresponding to the {@link DeploymentProvider}.
   * 
   * @param deploymentProvider
   *          the DeploymentProvider
   * @return the DeploymentProviderService
   */
  public DeploymentProviderService getDeploymentProviderService(
      DeploymentProvider deploymentProvider) {
    return Optional.ofNullable(providers.get(deploymentProvider))
        .orElseThrow(() -> new OrchestratorException(
            "No DeploymentProviderService found for deployment provider: " + deploymentProvider));
  }
}
