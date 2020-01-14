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

package it.reply.orchestrator.service.deployment.providers;

import com.google.common.collect.Lists;

import it.reply.orchestrator.config.specific.ToscaParserAwareTest;
import it.reply.orchestrator.controller.ControllerTestUtils;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.CloudProviderEndpoint.IaaSType;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.MarathonService;
import it.reply.orchestrator.dto.cmdb.MarathonService.MarathonServiceProperties;
import it.reply.orchestrator.dto.cmdb.CloudServiceType;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.workflow.CloudServicesOrderedIterator;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.exception.service.BusinessWorkflowException;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.function.ThrowingFunction;
import it.reply.orchestrator.service.ToscaService;
import it.reply.orchestrator.service.ToscaServiceTest;
import it.reply.orchestrator.service.VaultService;
import it.reply.orchestrator.service.deployment.providers.factory.MarathonClientFactory;
import it.reply.orchestrator.util.TestUtil;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import mesosphere.client.common.ModelUtils;
import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.MarathonException;
import mesosphere.marathon.client.model.v2.ExternalVolume;
import mesosphere.marathon.client.model.v2.GetAppResponse;
import mesosphere.marathon.client.model.v2.Group;
import mesosphere.marathon.client.model.v2.HealthCheck;
import mesosphere.marathon.client.model.v2.LocalVolume;
import mesosphere.marathon.client.model.v2.TaskFailure;
import mesosphere.marathon.client.model.v2.VersionedApp;
import mesosphere.marathon.client.model.v2.Volume;

import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class MarathonServiceTest extends ToscaParserAwareTest {

  @InjectMocks
  private MarathonServiceImpl marathonServiceImpl;

  @SpyBean
  @Autowired
  protected ToscaService toscaService;

  @MockBean
  private ResourceRepository resourceRepository;

  @MockBean
  private DeploymentRepository deploymentRepository;

  @MockBean
  private MarathonClientFactory marathonClientFactory;

  @MockBean
  private VaultService vaultService;

  @MockBean
  private Marathon marathonClient;

  private static final String defaultVaultEndpoint = "https://default.vault.com:8200";

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    Mockito
        .when(oauth2tokenService.executeWithClientForResult(
            Mockito.any(), Mockito.any(), Mockito.any()))
        .thenAnswer(y -> ((ThrowingFunction) y.getArguments()[1]).apply("token"));
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

  @Test(expected = UnsupportedOperationException.class)
  public void testdoUpdateFail() {
    marathonServiceImpl.doUpdate(null, null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testCleanFailedUpdateFail() {
    marathonServiceImpl.cleanFailedUpdate(null);
  }

  private Deployment generateDeployment() throws IOException {
    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.setCloudProviderEndpoint(CloudProviderEndpoint.builder()
        .cpComputeServiceId(UUID.randomUUID().toString())
        .cpEndpoint("example.com")
        .iaasType(IaaSType.MARATHON)
        .build());
    deployment.setTemplate(
        TestUtil.getFileContentAsString(ToscaServiceTest.TEMPLATES_BASE_DIR + "marathon_app.yaml"));

    Resource runtime = new Resource();
    runtime.setDeployment(deployment);
    runtime.setId("1");
    runtime.setState(NodeStates.INITIAL);
    runtime.setToscaNodeName("Docker");
    runtime.setToscaNodeType("tosca.nodes.indigo.Container.Runtime.Docker");
    deployment.getResources().add(runtime);

    Resource app = new Resource();
    app.setDeployment(deployment);
    app.setId("2");
    app.setState(NodeStates.INITIAL);
    app.setToscaNodeName("marathon");
    app.setToscaNodeType("tosca.nodes.indigo.Container.Application.Docker.Marathon");
    app.addRequiredResource(runtime);
    deployment.getResources().add(app);

    Mockito
        .when(resourceRepository.findByToscaNodeNameAndDeployment_id("Docker",
            deployment.getId()))
        .thenReturn(Lists.newArrayList(runtime));

    Mockito
        .when(resourceRepository.findByToscaNodeNameAndDeployment_id("marathon",
            deployment.getId()))
        .thenReturn(Lists.newArrayList(app));
    return deployment;
  }

  @Test
  public void testCreateGroup() throws IOException {
    Deployment deployment = generateDeployment();
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);
    Mockito
        .when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);
    Assertions
        .assertThat(marathonServiceImpl.createGroup(dm, null))
        .isEqualToComparingFieldByFieldRecursively(
            ModelUtils.GSON.fromJson(TestUtil.getFileContentAsString(
                ToscaServiceTest.TEMPLATES_BASE_DIR + "marathon_app.json"), Group.class));
  }

  private MarathonService buildService() {
    MarathonService cs = MarathonService
        .marathonBuilder()
        .endpoint("example.com/marathon")
        .serviceType(CloudService.MARATHON_COMPUTE_SERVICE)
        .hostname("example.com")
        .providerId("provider-1")
        .id("provider-1-service-1")
        .type(CloudServiceType.COMPUTE)
        .properties(MarathonServiceProperties
            .builder()
            .localVolumesHostBasePath("/tmp/")
            .build())
        .build();
    return cs;
  }

  @Test
  public void testDoUndeploy() throws IOException, URISyntaxException {
    Deployment deployment = generateDeployment();
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    MarathonService cs = buildService();

    CloudServicesOrderedIterator csi = new CloudServicesOrderedIterator(Lists.newArrayList(cs));
    csi.next();
    dm.setCloudServicesOrderedIterator(csi);

    Mockito
        .when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);
    Mockito
        .when(marathonClientFactory.build(deployment.getCloudProviderEndpoint(), "token"))
        .thenReturn(marathonClient);
    Mockito
        .when(vaultService.getServiceUri())
        .thenReturn(Optional.of(new URI(defaultVaultEndpoint)));
    Assertions
    .assertThat(marathonServiceImpl.doUndeploy(dm))
    .isTrue();    
  }

  @Test
  public void testDoDeploy() throws IOException, URISyntaxException {
    Deployment deployment = generateDeployment();
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    MarathonService cs = buildService();

    CloudServicesOrderedIterator csi = new CloudServicesOrderedIterator(Lists.newArrayList(cs));
    csi.next();
    dm.setCloudServicesOrderedIterator(csi);

    Mockito
        .when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);
    Mockito
        .when(marathonClientFactory.build(deployment.getCloudProviderEndpoint(), "token"))
        .thenReturn(marathonClient);
    Mockito
        .when(vaultService.getServiceUri())
        .thenReturn(Optional.of(new URI(defaultVaultEndpoint)));
    Assertions
        .assertThat(marathonServiceImpl.doDeploy(dm))
        .isTrue();
  }

  @Test
  @Parameters({"true","false"})
  public void testIsUndeployed(boolean expected) {
    Deployment deployment = new Deployment();
    deployment.setId(UUID.randomUUID().toString());
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);
    Mockito
        .when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);
    Mockito
        .when(marathonClientFactory.build(deployment.getCloudProviderEndpoint(), "token"))
        .thenReturn(marathonClient);
    VersionedApp app = new VersionedApp();
    app.setId("appId");
    Group group = new Group();
    group.setId(deployment.getId());
    group.setApps(Lists.newArrayList(app));

    if (!expected) {
      Mockito
          .when(marathonClient.getGroup(deployment.getId()))
          .thenReturn(group);
    } else {
      Mockito
          .when(marathonClient.getGroup(deployment.getId()))
          .thenThrow(new MarathonException(404, "Deployment not found"));
    }
    Assertions
        .assertThat(marathonServiceImpl.isUndeployed(dm))
        .isEqualTo(expected);
  }

  @Test
  @Parameters({ "1,0,0,0,false",
      "0,1,0,0,true",
      "1,1,0,0,false",
      "0,1,1,0,false",
      "0,1,1,1,true",
      "1,1,1,1,false" })
  public void testIsDeployed(int deployments, int running, int healtChecks,
      int healty, boolean expected) {
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
        .when(marathonClientFactory.build(deployment.getCloudProviderEndpoint(), "token"))
        .thenReturn(marathonClient);
    Assertions
        .assertThat(marathonServiceImpl.isDeployed(dm))
        .isEqualTo(expected);
  }

  @Test
  @Parameters({"true", "false"})
  public void doProviderTimeoutSuccessful(boolean hasTaskFailure) throws IOException {
    Deployment deployment = generateDeployment();
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    VersionedApp app = new VersionedApp();
    app.setId("appId");
    if (hasTaskFailure) {
      TaskFailure tf = new TaskFailure();
      tf.setMessage("Task Failure Message");
      app.setLastTaskFailure(tf);
    }

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
        .when(marathonClientFactory.build(deployment.getCloudProviderEndpoint(), "token"))
        .thenReturn(marathonClient);

    AbstractThrowableAssert<?, ? extends Throwable> assertion = assertThatCode(
        () -> marathonServiceImpl.doProviderTimeout(dm));
    if (!hasTaskFailure) {
      assertion.isInstanceOf(BusinessWorkflowException.class)
          .hasCauseExactlyInstanceOf(DeploymentException.class)
          .hasMessage("Error executing request to Marathon service;"
              + " nested exception is it.reply.orchestrator.exception.service."
              + "DeploymentException: Deployment timeout");
    } else {
      assertion.isInstanceOf(BusinessWorkflowException.class)
      .hasCauseExactlyInstanceOf(DeploymentException.class)
      .hasMessage("Error executing request to Marathon service;"
          + " nested exception is it.reply.orchestrator.exception.service."
          + "DeploymentException: Deployment timeout: Task Failure Message\n");
    }
  }

}
