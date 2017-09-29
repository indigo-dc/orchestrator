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

package it.reply.orchestrator.service.deployment.providers.factory;

import it.infn.ba.indigo.chronos.client.Chronos;
import it.infn.ba.indigo.chronos.client.ChronosClient;
import it.reply.orchestrator.config.properties.ChronosProperties;
import it.reply.orchestrator.config.properties.MesosProperties;
import it.reply.orchestrator.dal.entity.Deployment;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ChronosClientFactory extends MesosFrameworkClientFactory<ChronosProperties, Chronos> {

  public ChronosClientFactory(MesosProperties mesosProperties) {
    super(mesosProperties);
  }

  @Override
  public Chronos build(ChronosProperties chronosProperties) {
    LOG.info("Generating Chronos client with parameters: {}", chronosProperties);
    return ChronosClient.getInstanceWithBasicAuth(chronosProperties.getUrl().toString(),
        chronosProperties.getUsername(), chronosProperties.getPassword());
  }

  @Override
  public Chronos build(Deployment deployment) {
    ChronosProperties chronosProperties = getFrameworkProperties(deployment);
    return build(chronosProperties);
  }

  @Override
  public ChronosProperties getFrameworkProperties(Deployment deployment) {
    return getInstanceProperties(deployment).getChronos();
  }

  @Override
  protected String getFrameworkName() {
    return "Chronos";
  }

}
