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

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

@RunWith(JUnitParamsRunner.class)
@RestClientTest(ConfigurationService.class)
@ContextConfiguration(classes = {ConfigurationServiceImpl.class, CprProperties.class,
    ImProperties.class, MonitoringProperties.class, CmdbProperties.class,
    SlamProperties.class, VaultProperties.class})
public class ConfigurationServiceTest {

  private static final String CPRURL = "https://cpr.test.it";
  private static final String SLAMURL = "https://slam.test.it";
  private static final String CMBDURL = "https://cmdb.test.it";
  private static final String IMURL = "https://im.test.it";
  private static final String MONITORINGURL = "https://monitoring.test.it";
  private static final String VAULTURL = "https://vault.test.it:8200";

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Autowired
  private ConfigurationService configurationService;

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

  @Before
  public void setup() throws URISyntaxException {
    cmdbProperties.setUrl(new URI(CMBDURL));
    cprProperties.setUrl(new URI(CPRURL));
    imProperties.setUrl(new URI(IMURL));
    monitoringProperties.setUrl(new URI(MONITORINGURL));
    slamProperties.setUrl(new URI(SLAMURL));
    vaultProperties.setUrl(new URI(VAULTURL));
  }

  @Test
  public void getConfiguration() throws URISyntaxException {

    SystemEndpoints endpoint = createTestEndpoint();

    Assert.assertEquals(configurationService.getConfiguration(), endpoint);
  }

  private SystemEndpoints createTestEndpoint() throws URISyntaxException {
    return new SystemEndpoints(
        new URI(CPRURL),
        new URI(SLAMURL),
        new URI(CMBDURL),
        new URI(IMURL),
        new URI(MONITORINGURL),
        new URI(VAULTURL));
  }

}
