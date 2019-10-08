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
import junitparams.JUnitParamsRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.net.URI;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(JUnitParamsRunner.class)
public class ConfigurationServiceTest {

  private static final URI CPR_URL = URI.create("https://cpr.test.it");
  private static final URI SLAM_URL = URI.create("https://slam.test.it");
  private static final URI CMDB_URL = URI.create("https://cmdb.test.it");
  private static final URI IM_URL = URI.create("https://im.test.it");
  private static final URI MONITORING_URL = URI.create("https://monitoring.test.it");
  private static final URI VAULT_URL = URI.create("https://vault.test.it:8200");

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @InjectMocks
  private ConfigurationServiceImpl configurationService;

  @Mock
  private CmdbProperties cmdbProperties;

  @Mock
  private CprProperties cprProperties;

  @Mock
  private ImProperties imProperties;

  @Mock
  private MonitoringProperties monitoringProperties;

  @Mock
  private SlamProperties slamProperties;

  @Mock
  private VaultProperties vaultProperties;

  @Before
  public void setup() {
    when(cmdbProperties.getUrl()).thenReturn(CMDB_URL);
    when(cprProperties.getUrl()).thenReturn(CPR_URL);
    when(imProperties.getUrl()).thenReturn(IM_URL);
    when(monitoringProperties.getUrl()).thenReturn(MONITORING_URL);
    when(slamProperties.getUrl()).thenReturn(SLAM_URL);
    when(vaultProperties.getUri()).thenReturn(VAULT_URL);
  }

  @Test
  public void getConfiguration() {

    SystemEndpoints endpoints = createTestEndpoint();

    assertThat(configurationService.getConfiguration())
        .isEqualTo(endpoints);
  }

  private SystemEndpoints createTestEndpoint() {
    return SystemEndpoints
      .builder()
      .cprUrl(CPR_URL)
      .slamUrl(SLAM_URL)
      .cmdbUrl(CMDB_URL)
      .imUrl(IM_URL)
      .monitoringUrl(MONITORING_URL)
      .vaultUrl(VAULT_URL)
      .build();
  }

}
