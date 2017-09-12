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

import alien4cloud.tosca.parser.ParsingException;

import it.reply.orchestrator.config.properties.MarathonProperties;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.IndigoInputsPreProcessorService;
import it.reply.orchestrator.service.ToscaService;

import mesosphere.marathon.client.model.v2.ExternalVolume;
import mesosphere.marathon.client.model.v2.LocalVolume;
import mesosphere.marathon.client.model.v2.Volume;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class MarathonServiceTest {

  private MarathonServiceImpl marathonServiceImpl;

  @Mock
  private ToscaService toscaService;

  @Mock
  private MarathonProperties marathonProperties;

  @Mock
  private ResourceRepository resourceRepository;
  
  @Mock
  private IndigoInputsPreProcessorService indigoInputsPreProcessorService;

  @Before
  public void setup() throws ParsingException {
    MockitoAnnotations.initMocks(this);
    marathonServiceImpl = new MarathonServiceImpl(toscaService, marathonProperties,
        resourceRepository, indigoInputsPreProcessorService);
  }

  @Test
  public void testGenerateLocalVolumeSuccess() {
    Volume actualVolume = marathonServiceImpl.generateVolume("local/path:/var/lib/mysql:rw");
    LocalVolume expectedVolume = new LocalVolume();
    expectedVolume.setHostPath("local/path");
    expectedVolume.setContainerPath("/var/lib/mysql");
    expectedVolume.setMode("RW");
    Assertions.assertThat(actualVolume).isEqualToComparingFieldByFieldRecursively(expectedVolume);
  }

  @Test
  public void testGenerateExternalVolumeSuccess() {
    Volume actualVolume = marathonServiceImpl.generateVolume("mysql:/var/lib/mysql:rw:dvdi:rexray");
    ExternalVolume expectedVolume = new ExternalVolume();
    expectedVolume.setName("mysql");
    expectedVolume.setContainerPath("/var/lib/mysql");
    expectedVolume.setMode("RW");
    expectedVolume.setProvider("dvdi");
    expectedVolume.setDriver("rexray");
    Assertions.assertThat(actualVolume).isEqualToComparingFieldByFieldRecursively(expectedVolume);
  }

  @Test(expected = DeploymentException.class)
  public void testGenerateVolumeFail() {
    marathonServiceImpl.generateVolume("nota:valid:volume:mount::string");
  }

}
