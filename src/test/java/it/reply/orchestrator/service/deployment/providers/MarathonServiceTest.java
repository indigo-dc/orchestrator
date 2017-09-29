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

import com.google.common.collect.Lists;

import alien4cloud.tosca.parser.ParsingException;

import it.reply.orchestrator.config.properties.MarathonProperties;
import it.reply.orchestrator.config.specific.ToscaParserAwareTest;
import it.reply.orchestrator.controller.ControllerTestUtils;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.IndigoInputsPreProcessorService;
import it.reply.orchestrator.service.ToscaServiceImpl;
import it.reply.orchestrator.service.ToscaServiceTest;
import it.reply.orchestrator.service.deployment.providers.factory.MarathonClientFactory;
import it.reply.orchestrator.util.TestUtil;

import mesosphere.client.common.ModelUtils;
import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.model.v2.ExternalVolume;
import mesosphere.marathon.client.model.v2.GetAppResponse;
import mesosphere.marathon.client.model.v2.Group;
import mesosphere.marathon.client.model.v2.HealthCheck;
import mesosphere.marathon.client.model.v2.LocalVolume;
import mesosphere.marathon.client.model.v2.VersionedApp;
import mesosphere.marathon.client.model.v2.Volume;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class MarathonServiceTest extends ToscaParserAwareTest {

  @InjectMocks
  private MarathonServiceImpl marathonServiceImpl;

  @Spy
  @InjectMocks
  private ToscaServiceImpl toscaService;

  @Spy
  private IndigoInputsPreProcessorService indigoInputsPreProcessorService;

  @Spy
  private MarathonProperties marathonProperties;

  @Mock
  private ResourceRepository resourceRepository;

  @Mock
  private DeploymentRepository deploymentRepository;

  @Mock
  private MarathonClientFactory marathonClientFactory;

  @Mock
  private Marathon marathonClient;

  @Before
  public void setup() throws ParsingException {
    MockitoAnnotations.initMocks(this);
    marathonProperties.setLocalVolumesHostBasePath("/tmp");
    marathonProperties.afterPropertiesSet();
  }

  @Test
  public void testGenerateLocalVolumeSuccess() {
    Volume actualVolume = marathonServiceImpl.generateVolume("/var/lib/mysql:rw");
    LocalVolume expectedVolume = new LocalVolume();
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
    marathonServiceImpl.generateVolume("local/path:/var/lib/mysql:rw");
  }

  private Deployment generateDeployment() throws IOException {
    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.setTemplate(
        TestUtil.getFileContentAsString(ToscaServiceTest.TEMPLATES_BASE_DIR + "marathon_app.yaml"));

    Resource runtime = new Resource();
    runtime.setDeployment(deployment);
    runtime.setId("1");
    runtime.setState(NodeStates.INITIAL);
    runtime.setToscaNodeName("docker_runtime");
    runtime.setToscaNodeType("tosca.nodes.indigo.Container.Runtime.Docker");
    deployment.getResources().add(runtime);

    Resource app = new Resource();
    app.setDeployment(deployment);
    app.setId("2");
    app.setState(NodeStates.INITIAL);
    app.setToscaNodeName("marathon-app");
    app.setToscaNodeType("tosca.nodes.indigo.Container.Application.Docker.Marathon");
    app.addRequiredResource(runtime);
    deployment.getResources().add(app);

    Mockito
        .when(resourceRepository.findByToscaNodeNameAndDeployment_id("docker_runtime",
            deployment.getId()))
        .thenReturn(Lists.newArrayList(runtime));

    Mockito
        .when(resourceRepository.findByToscaNodeNameAndDeployment_id("marathon-app",
            deployment.getId()))
        .thenReturn(Lists.newArrayList(app));
    return deployment;
  }

  @Test
  public void testCreateGroup() throws IOException {
    Deployment deployment = generateDeployment();

    Assertions
        .assertThat(marathonServiceImpl.createGroup(deployment))
        .isEqualToComparingFieldByFieldRecursively(
            ModelUtils.GSON.fromJson(TestUtil.getFileContentAsString(
                ToscaServiceTest.TEMPLATES_BASE_DIR + "marathon_app.json"), Group.class));
  }

  @Test
  public void testDoDeploy() throws IOException {
    Deployment deployment = generateDeployment();
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    Mockito
        .when(marathonClientFactory.getFrameworkProperties(deployment))
        .thenReturn(marathonProperties);
    Mockito
        .when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);
    Mockito
        .when(marathonClientFactory.build(deployment))
        .thenReturn(marathonClient);

    Assertions
        .assertThat(marathonServiceImpl.doDeploy(dm))
        .isTrue();
  }

  @Test
  @Parameters({ "1,0,0,0,false",
      "0,1,0,0,true",
      "1,1,0,0,false",
      "0,1,1,0,false",
      "0,1,1,1,true",
      "1,1,1,1,false" })
  public void testIsDeployed(int deployments, int running, int healtChecks,
      int healty, boolean expected) throws IOException {
    Deployment deployment = new Deployment();
    deployment.setId(UUID.randomUUID().toString());
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);
    VersionedApp app = new VersionedApp();
    app.setId("appId");

    app.setDeployments(IntStream
        .range(0, deployments)
        .mapToObj(i -> new mesosphere.marathon.client.model.v2.App.Deployment())
        .collect(Collectors.toList()));

    app.setInstances(1);

    app.setHealthChecks(IntStream
        .range(0, healtChecks)
        .mapToObj(i -> new HealthCheck())
        .collect(Collectors.toList()));

    app.setTasksRunning(running);
    app.setTasksHealthy(healty);

    Group group = new Group();
    group.setId(deployment.getId());
    group.setApps(Lists.newArrayList(app));

    Mockito
        .when(marathonClient.getGroup(deployment.getId()))
        .thenReturn(group);

    GetAppResponse appResponse = new GetAppResponse();
    appResponse.setApp(app);

    Mockito
        .when(marathonClient.getApp("appId"))
        .thenReturn(appResponse);
    Mockito
        .when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);
    Mockito
        .when(marathonClientFactory.build(deployment))
        .thenReturn(marathonClient);
    Assertions
        .assertThat(marathonServiceImpl.isDeployed(dm))
        .isEqualTo(expected);
  }

  @Override
  protected ToscaServiceImpl getToscaService() {
    return toscaService;
  }

}
