/*
 * Copyright © 2019 I.N.F.N.
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

package it.reply.orchestrator.service;

import it.reply.orchestrator.config.properties.CmdbProperties;
import it.reply.orchestrator.config.properties.CprProperties;
import it.reply.orchestrator.config.properties.ImProperties;
import it.reply.orchestrator.config.properties.MonitoringProperties;
import it.reply.orchestrator.config.properties.SlamProperties;
import it.reply.orchestrator.config.properties.VaultProperties;
import it.reply.orchestrator.dto.SystemEndpoints;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationServiceImpl implements ConfigurationService {

  @Autowired
  private CmdbProperties cmdbProperties;

  @Autowired
  private CprProperties cprProperties;

  @Autowired
  private ImProperties imProperties;

  @Autowired
  private MonitoringProperties monitoringProperties;

  @Autowired
  private SlamProperties slamProperties;

  @Autowired
  private VaultProperties vaultProperties;

  /**
   * Return the system configuration endpoints.
   * @return the configuration endpoints
   */
  public SystemEndpoints getConfiguration() {
    return SystemEndpoints
        .builder()
        .cprUrl(cprProperties.getUrl())
        .slamUrl(slamProperties.getUrl())
        .cmdbUrl(cmdbProperties.getUrl())
        .imUrl(imProperties.getUrl())
        .monitoringUrl(monitoringProperties.getUrl())
        .vaultUrl(vaultProperties.getUrl())
        .build();
  }

}
