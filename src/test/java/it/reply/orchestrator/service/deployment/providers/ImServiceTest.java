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

import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import es.upv.i3m.grycap.im.InfrastructureManager;
import es.upv.i3m.grycap.im.States;
import es.upv.i3m.grycap.im.exceptions.ImClientErrorException;
import es.upv.i3m.grycap.im.exceptions.ImClientException;
import es.upv.i3m.grycap.im.exceptions.InfrastructureUuidNotFoundException;
import es.upv.i3m.grycap.im.pojo.InfOutputValues;
import es.upv.i3m.grycap.im.pojo.InfrastructureState;
import es.upv.i3m.grycap.im.pojo.InfrastructureUri;
import es.upv.i3m.grycap.im.pojo.InfrastructureUris;
import es.upv.i3m.grycap.im.pojo.Property;
import es.upv.i3m.grycap.im.pojo.ResponseError;
import es.upv.i3m.grycap.im.pojo.VirtualMachineInfo;
import es.upv.i3m.grycap.im.rest.client.BodyContentType;
import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.config.specific.ToscaParserAwareTest;
import it.reply.orchestrator.controller.ControllerTestUtils;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.cmdb.ComputeService;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.CloudServiceType;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.workflow.CloudServiceWf;
import it.reply.orchestrator.dto.workflow.CloudServicesOrderedIterator;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.exception.service.BusinessWorkflowException;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.exception.service.ToscaException;
import it.reply.orchestrator.function.ThrowingFunction;
import it.reply.orchestrator.service.ToscaServiceImpl;
import it.reply.orchestrator.service.deployment.providers.factory.ImClientFactory;
import it.reply.orchestrator.util.TestUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.assertj.core.api.AbstractThrowableAssert;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static it.reply.orchestrator.dto.cmdb.CloudService.OPENSTACK_COMPUTE_SERVICE;
import static it.reply.orchestrator.dto.cmdb.CloudService.ONEPROVIDER_STORAGE_SERVICE;
import static org.mockito.Mockito.*;

@RunWith(JUnitParamsRunner.class)
public class ImServiceTest extends ToscaParserAwareTest {

  @InjectMocks
  private ImServiceImpl imService;

  @MockBean
  private ImClientFactory imClientFactory;

  @MockBean
  private DeploymentRepository deploymentRepository;

  @SpyBean
  private DeploymentStatusHelperImpl deploymentStatusHelper;

  @MockBean
  private ResourceRepository resourceRepository;

  @MockBean
  private InfrastructureManager infrastructureManager;

  @MockBean
  private OidcProperties oidcProperties;

  @SpyBean
  @Autowired
  private ToscaServiceImpl toscaService;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    Mockito
        .when(oauth2tokenService.executeWithClientForResult(
            Mockito.any(), Mockito.any(), Mockito.any()))
        .thenAnswer(y -> ((ThrowingFunction) y.getArguments()[1]).apply("token"));
  }

  private InfrastructureState generateInfrastructureState(States state, int vmNum) {
    InfrastructureState infrastructureState =
        new InfrastructureState(state.getValue(), Maps.newHashMap());

    for (int i = 0; i < vmNum; ++i) {
      infrastructureState.getVmStates().put(String.valueOf(i), state.getValue());
    }
    return infrastructureState;
  }

  private List<VirtualMachineInfo> generateVirtualMachineInfo(int vmNum) {
    return IntStream.range(0, vmNum).mapToObj(i -> {
      Map<String, Object> properties = new HashMap<>();
      properties.put("class", "system");
      properties.put("id", "node_" + i);
      return new VirtualMachineInfo(Lists.newArrayList(properties));
    }).collect(Collectors.toList());
  }

  @Test
  public void testDoDeploySuccesful()
      throws ToscaException, ParsingException, IOException, ImClientException {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    ComputeService cs = ComputeService
        .computeBuilder()
        .endpoint("http://example.com")
        .providerId("cloud-provider-id-1")
        .id("cloud-service-id-1")
        .type(CloudServiceType.COMPUTE)
        .endpoint("http://example.com")
        .serviceType(OPENSTACK_COMPUTE_SERVICE)
        .hostname("example.com")
        .build();
    CloudServicesOrderedIterator csi = new CloudServicesOrderedIterator(Lists.newArrayList(new CloudServiceWf(cs)));
    csi.next();
    dm.setCloudServicesOrderedIterator(csi);

    String infrastructureId = UUID.randomUUID().toString();
    InfrastructureUri infrastructureUri =
        new InfrastructureUri("http://localhost:8080/infrastructures/" + infrastructureId);
    ArchiveRoot ar = new ArchiveRoot();

    Mockito.when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);

    Mockito.doReturn(ar).when(toscaService).parseAndValidateTemplate(deployment.getTemplate(),
        deployment.getParameters());
    Mockito.doNothing().when(indigoInputsPreProcessorService).processGetInputAttributes(eq(ar),
        eq(deployment.getParameters()), Mockito.any());
    Mockito.when(infrastructureManager.createInfrastructureAsync(Mockito.anyString(),
        Mockito.eq(BodyContentType.TOSCA))).thenReturn(infrastructureUri);
    Mockito.doReturn(infrastructureManager).when(imClientFactory)
        .build(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());

    boolean returnValue = imService.doDeploy(dm);

    assertThat(deployment.getTask()).isEqualTo(Task.DEPLOYER);
    assertThat(deployment.getStatus()).isEqualTo(Status.CREATE_IN_PROGRESS);
    assertThat(deployment.getDeploymentProvider()).isEqualTo(DeploymentProvider.IM);
    assertThat(deployment.getEndpoint()).isEqualTo(infrastructureId);
    assertThat(deployment.getResources()).hasSize(2);
    assertThat(deployment.getResources()).extracting(Resource::getState).allMatch(NodeStates.CREATING::equals);
    assertThat(returnValue).isTrue();
  }

  @Test
  public void testDoDeployNoId() throws ToscaException, ImClientException {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    ComputeService cs = ComputeService
        .computeBuilder()
        .endpoint("http://example.com")
        .providerId("cloud-provider-id-1")
        .id("cloud-service-id-1")
        .type(CloudServiceType.COMPUTE)
        .endpoint("http://example.com")
        .serviceType(OPENSTACK_COMPUTE_SERVICE)
        .hostname("example.com")
        .build();
    CloudServicesOrderedIterator csi = new CloudServicesOrderedIterator(Lists.newArrayList(new CloudServiceWf(cs)));
    csi.next();
    dm.setCloudServicesOrderedIterator(csi);

    InfrastructureUri infrastructureUri =
        new InfrastructureUri("http://localhost:8080/infrastructures/");
    ArchiveRoot ar = new ArchiveRoot();

    Mockito.when(deploymentRepository.save(deployment)).thenReturn(deployment);
    Mockito.when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);
    Mockito.doReturn(ar).when(toscaService).parseAndValidateTemplate(deployment.getTemplate(),
        deployment.getParameters());
    Mockito.doNothing().when(indigoInputsPreProcessorService).processGetInputAttributes(eq(ar),
        eq(deployment.getParameters()), Mockito.any());
    Mockito.when(infrastructureManager.createInfrastructureAsync(Mockito.anyString(),
        Mockito.eq(BodyContentType.TOSCA))).thenReturn(infrastructureUri);
    Mockito.doReturn(infrastructureManager).when(imClientFactory)
        .build(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());

    assertThatThrownBy(() -> imService.doDeploy(dm))
        .hasCauseExactlyInstanceOf(InfrastructureUuidNotFoundException.class);
  }

  @Parameters({
      "false|false|false|false",
      "false|false|true|false",
      "false|true|false|false",
      "false|true|true|false",
      "true|false|false|true",
      "true|false|true|true",
      "true|true|false|true",
      "true|true|true|false"
  })
  @Test
  public void testCleanFailedAttempt(
      boolean hasDeploymentEndpoint,
      boolean isLastProvider,
      boolean isKeepLastAttempt,
      boolean deleteExpectedToBeCalled) throws ImClientException {

    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    deployment.setEndpoint(hasDeploymentEndpoint ? "endpoint" : null);

    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);
    dm.setKeepLastAttempt(isKeepLastAttempt);

    CloudServicesOrderedIterator csi = mock(CloudServicesOrderedIterator.class);
    Mockito.when(csi.hasNext()).thenReturn(!isLastProvider);

    dm.setCloudServicesOrderedIterator(csi);

    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);

    Mockito.doReturn(infrastructureManager).when(imClientFactory)
        .build(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());

    imService.cleanFailedDeploy(dm);
    Mockito.verify(infrastructureManager, Mockito.times(deleteExpectedToBeCalled ? 1 : 0))
        .destroyInfrastructureAsync("endpoint");
  }

  @Test
  public void testDoDeployIMexception()
      throws ToscaException, ParsingException, IOException, ImClientException {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    ComputeService cs = ComputeService
        .computeBuilder()
        .endpoint("http://example.com")
        .providerId("cloud-provider-id-1")
        .id("cloud-service-id-1")
        .type(CloudServiceType.COMPUTE)
        .endpoint("http://example.com")
        .serviceType(OPENSTACK_COMPUTE_SERVICE)
        .hostname("example.com")
        .build();
    CloudServicesOrderedIterator csi = new CloudServicesOrderedIterator(Lists.newArrayList(new CloudServiceWf(cs)));
    csi.next();
    dm.setCloudServicesOrderedIterator(csi);

    ArchiveRoot ar = new ArchiveRoot();
    ImClientErrorException imException =
        new ImClientErrorException(new ResponseError("Error", 500));

    Mockito.when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);
    Mockito.doReturn(ar).when(toscaService).parseAndValidateTemplate(deployment.getTemplate(),
        deployment.getParameters());
    Mockito.doNothing().when(indigoInputsPreProcessorService).processGetInputAttributes(eq(ar),
        eq(deployment.getParameters()), Mockito.any());
    Mockito.doReturn(infrastructureManager).when(imClientFactory)
        .build(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());
    Mockito.when(infrastructureManager.createInfrastructureAsync(Mockito.anyString(),
        Mockito.eq(BodyContentType.TOSCA))).thenThrow(imException);

    assertThatThrownBy(() -> imService.doDeploy(dm))
        .hasCauseInstanceOf(ImClientException.class);
  }

  @Test
  public void testIsDeployedSuccesful() throws ImClientException, JsonProcessingException {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    deployment.setTask(Task.DEPLOYER);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    InfrastructureState infrastructureState = generateInfrastructureState(States.CONFIGURED, 2);

    List<VirtualMachineInfo> info= generateVirtualMachineInfo(2);
    List<Resource> resources = new ArrayList<>(deployment.getResources());
    ObjectMapper mapper = new ObjectMapper();
    Map<String,String> metadata1 = new HashMap<>();
    metadata1.put("VirtualMachineInfo",  mapper.writeValueAsString(info.get(0)));
    resources.get(0).setMetadata(metadata1);
    Map<String,String> metadata2 = new HashMap<>();
    metadata2.put("VirtualMachineInfo",  mapper.writeValueAsString(info.get(1)));
    resources.get(1).setMetadata(metadata2);
    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    Mockito.doReturn(infrastructureManager).when(imClientFactory)
        .build(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());
    Mockito.when(infrastructureManager.getInfrastructureState(deployment.getEndpoint()))
        .thenReturn(infrastructureState);
    Mockito
        .when(infrastructureManager.getVmInfo(Mockito.eq(deployment.getEndpoint()), Mockito.anyString()))
        .thenReturn(info.get(0), info.get(1));
    Mockito.when(resourceRepository
            .findByDeployment_id(deployment.getId())).thenReturn(resources);

    boolean returnValue = imService.isDeployed(dm);

    assertThat(deployment.getTask()).isEqualTo(Task.DEPLOYER);
    assertThat(deployment.getStatus()).isEqualTo(Status.CREATE_IN_PROGRESS);
    assertThat(deployment.getDeploymentProvider()).isEqualTo(DeploymentProvider.IM);
    assertThat(deployment.getEndpoint()).isEqualTo(deployment.getEndpoint());
    assertThat(deployment.getResources()).hasSize(2);
    assertThat(deployment.getResources())
        .extracting(Resource::getState)
        .allMatch(NodeStates.CREATING::equals);
    assertThat(returnValue).isTrue();
  }

  @Test
  public void testIsDeployedFail() throws ImClientException {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    Mockito.when(deploymentRepository.findOne(dm.getDeploymentId())).thenReturn(deployment);

    Mockito.doReturn(infrastructureManager).when(imClientFactory)
        .build(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());
    Mockito.doThrow(new ImClientException()).when(infrastructureManager)
        .getInfrastructureState(Mockito.anyString());

    assertThatThrownBy(() -> imService.isDeployed(dm)).isInstanceOf(DeploymentException.class);
  }

  @Test
  public void testFinalizeDeployImClientError() throws ImClientException {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    Mockito.when(deploymentRepository.findOne(dm.getDeploymentId())).thenReturn(deployment);

    Mockito.doReturn(infrastructureManager).when(imClientFactory)
        .build(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());
    Mockito.doThrow(new ImClientErrorException(new ResponseError("Not Found", 404)))
        .when(infrastructureManager).getInfrastructureOutputs(Mockito.anyString());

    assertThatThrownBy(() -> imService.finalizeDeploy(dm)).isInstanceOf(DeploymentException.class);
  }

  @Test
  public void testFinalizeDeployGenericExceptionError() throws ImClientException {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    Mockito.when(deploymentRepository.findOne(dm.getDeploymentId())).thenReturn(deployment);

    Mockito.doReturn(infrastructureManager).when(imClientFactory)
        .build(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());
    Mockito.doThrow(new ImClientException()).when(infrastructureManager)
        .getInfrastructureOutputs(Mockito.anyString());

    assertThatThrownBy(() -> imService.finalizeDeploy(dm)).isInstanceOf(DeploymentException.class);
  }

  @Test
  public void testIsNotYetDeployed() throws ImClientException {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    deployment.setTask(Task.DEPLOYER);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    InfrastructureState infrastructureState = generateInfrastructureState(States.RUNNING, 2);
    List<VirtualMachineInfo> info= generateVirtualMachineInfo(2);

    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    Mockito.doReturn(infrastructureManager).when(imClientFactory)
        .build(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());
    Mockito.when(infrastructureManager.getInfrastructureState(deployment.getEndpoint()))
        .thenReturn(infrastructureState);

    Mockito
        .when(infrastructureManager.getVmInfo(Mockito.eq(deployment.getEndpoint()),
            Mockito.anyString()))
        .thenReturn(info.get(0), info.get(1));
    Mockito
        .when(resourceRepository
            .findByDeployment_id(deployment.getId()))
        .thenReturn(new ArrayList<>(deployment.getResources()));

    boolean returnValue = imService.isDeployed(dm);

    assertThat(deployment.getTask()).isEqualTo(Task.DEPLOYER);
    assertThat(deployment.getStatus()).isEqualTo(Status.CREATE_IN_PROGRESS);
    assertThat(deployment.getDeploymentProvider()).isEqualTo(DeploymentProvider.IM);
    assertThat(deployment.getEndpoint()).isEqualTo(deployment.getEndpoint());
    assertThat(deployment.getResources()).hasSize(2);
    assertThat(deployment.getResources())
        .extracting(Resource::getState)
        .allMatch(NodeStates.CREATING::equals);
    assertThat(returnValue).isFalse();
  }

  @Test
  @Parameters({"FAILED","UNCONFIGURED"})
  public void testIsDeployedFailedInfrastructureStatus(States infrState) throws ImClientException {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    deployment.setTask(Task.DEPLOYER);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    InfrastructureState infrastructureState = generateInfrastructureState(infrState, 2);
    List<VirtualMachineInfo> info= generateVirtualMachineInfo(2);

    Mockito.when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);
    Mockito.doReturn(infrastructureManager).when(imClientFactory)
        .build(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());

    Mockito.when(infrastructureManager.getInfrastructureState(deployment.getEndpoint()))
        .thenReturn(infrastructureState);

    Mockito
        .when(infrastructureManager.getVmInfo(Mockito.eq(deployment.getEndpoint()),
            Mockito.anyString()))
        .thenReturn(info.get(0), info.get(1));
    Mockito
        .when(resourceRepository
            .findByDeployment_id(deployment.getId()))
        .thenReturn(new ArrayList<>(deployment.getResources()));

    assertThatThrownBy(() -> imService.isDeployed(dm)).hasMessageEndingWith(
        "Some error occurred during the contextualization of the IM infrastructure\n" +
        infrastructureState.getFormattedInfrastructureStateString());
  }

  @Test
  @Parameters({"true", "false"})
  public void testGetDeploymentExtendedInfo(boolean fail) throws ImClientException, JsonProcessingException {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    List<Resource> resources = new ArrayList<>(deployment.getResources());

    ObjectMapper mapper = new ObjectMapper();

    VirtualMachineInfo vmInfo0 = new VirtualMachineInfo(Lists.newArrayList());
    vmInfo0.getVmProperties().add(Maps.newHashMap());
    vmInfo0.getVmProperties().get(0).put("id",
        resources.get(0).getToscaNodeName());
    String vmInfo0s = mapper.writeValueAsString(vmInfo0);
    Map<String,String> metadata1 = new HashMap<>();
    metadata1.put("VirtualMachineInfo", vmInfo0s);
    resources.get(0).setMetadata(metadata1);

    VirtualMachineInfo vmInfo1 = new VirtualMachineInfo(Lists.newArrayList());
    vmInfo1.getVmProperties().add(Maps.newHashMap());
    vmInfo1.getVmProperties().get(0).put("id",
        resources.get(1).getToscaNodeName());
    String vmInfo1s = mapper.writeValueAsString(vmInfo1);
    Map<String,String> metadata2 = new HashMap<>();
    metadata2.put("VirtualMachineInfo", vmInfo1s);
    resources.get(1).setMetadata(metadata2);

    when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    when(resourceRepository.findByDeployment_id(deployment.getId())).thenReturn(resources);
    if (!fail) {
      assertThat(imService.getDeploymentExtendedInfo(dm).get())
          .isEqualTo("[" + vmInfo0s + "," + vmInfo1s + "]");
    } else {
      when(imService.getDeploymentExtendedInfoInternal(dm)).thenThrow(new RuntimeException("test failed"));
      assertThat(imService.getDeploymentExtendedInfo(dm))
          .isEqualTo(Optional.empty());
    }
  }

  @Test
  @Parameters({"true|false", "false|false", "true|true"})
  public void testGetDeploymentLog(boolean empty, boolean fail) throws ImClientException {
    Deployment deployment = ControllerTestUtils.createDeployment(0);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);
    String logMessage = empty ? "" : "Deployment log message";
    Property logMessageProperty = new Property("ContMsg",logMessage);
    Mockito.doReturn(infrastructureManager).when(imClientFactory)
        .build(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());
    Mockito.when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);
    Mockito.when(infrastructureManager.getInfrastructureContMsg(Mockito.anyString()))
        .thenReturn(logMessageProperty);
    if (empty) {
      if (fail) {
        when(imService.getDeploymentLogInternal(dm)).thenThrow(new RuntimeException("test failed"));
      }
      assertThat(imService.getDeploymentLog(dm)).isEqualTo(Optional.empty());
    } else {
      assertThat(imService.getDeploymentLog(dm)).isEqualTo(Optional.of(logMessage));
    }
  }

  @Test
  public void testFinalizeDeploy() throws ImClientException {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    Map<String, Object> outputs = Maps.newHashMap();
    outputs.put("firstKey", 1);
    outputs.put("SecondKey", null);

    InfOutputValues outputValues = new InfOutputValues(outputs);

    InfrastructureUris vmUrls = new InfrastructureUris(Lists.newArrayList());
    vmUrls.getUris().add(new InfrastructureUri(
        "http://localhost/infrastructures/" + deployment.getEndpoint() + "/" + 0));
    vmUrls.getUris().add(new InfrastructureUri(
        "http://localhost/infrastructures/" + deployment.getEndpoint() + "/" + 1));

    List<Resource> resources = new ArrayList<>(deployment.getResources());
    VirtualMachineInfo vmInfo0 = new VirtualMachineInfo(Lists.newArrayList());
    vmInfo0.getVmProperties().add(Maps.newHashMap());
    vmInfo0.getVmProperties().get(0).put("id",
        resources.get(0).getToscaNodeName());
    VirtualMachineInfo vmInfo1 = new VirtualMachineInfo(Lists.newArrayList());
    vmInfo1.getVmProperties().add(Maps.newHashMap());
    vmInfo1.getVmProperties().get(0).put("id",
        resources.get(1).getToscaNodeName());

    Mockito.when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);
    Mockito.when(deploymentRepository.save(deployment)).thenReturn(deployment);
    Mockito.when(resourceRepository.findByDeployment_id(deployment.getId()))
        .thenReturn(resources);
    Mockito.doReturn(infrastructureManager).when(imClientFactory)
        .build(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());

    Mockito.when(infrastructureManager.getInfrastructureOutputs(deployment.getEndpoint()))
        .thenReturn(outputValues);
    Mockito.when(infrastructureManager.getInfrastructureInfo(deployment.getEndpoint()))
        .thenReturn(vmUrls);
    Mockito
        .when(infrastructureManager.getVmInfo(deployment.getEndpoint(), String.valueOf(0)))
        .thenReturn(vmInfo0);
    Mockito
        .when(infrastructureManager.getVmInfo(deployment.getEndpoint(), String.valueOf(1)))
        .thenReturn(vmInfo1);

    imService.finalizeDeploy(dm);

    assertThat(deployment.getTask()).isEqualTo(Task.NONE);
    assertThat(deployment.getStatus()).isEqualTo(Status.CREATE_COMPLETE);
    assertThat(deployment.getDeploymentProvider()).isEqualTo(DeploymentProvider.IM);
    assertThat(deployment.getEndpoint()).isEqualTo(deployment.getEndpoint());
    assertThat(deployment.getResources()).hasSize(2);
    assertThat(deployment.getResources())
        .extracting(Resource::getState)
        .allMatch(NodeStates.STARTED::equals);
    assertThat(dm.isPollComplete()).isFalse();
  }

  @Test
  public void doProviderTimeoutSuccessful() {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    AbstractThrowableAssert<?, ? extends Throwable> assertion = assertThatCode(
        () -> imService.doProviderTimeout(dm));
    assertion.isInstanceOf(BusinessWorkflowException.class)
        .hasCauseExactlyInstanceOf(DeploymentException.class)
        .hasMessage("Error executing request to IM;"
            + " nested exception is it.reply.orchestrator.exception.service."
            + "DeploymentException: IM provider timeout during deployment");
  }

  @Test
  public void testDoUndeploySuccess() throws ImClientException {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);

    assertThat(imService.doUndeploy(dm)).isTrue();
  }

  @Test
  public void testDoUndeploySuccessDeleteComplete() throws ImClientException {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    deployment.setEndpoint("endpoint");
    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    Mockito.doReturn(infrastructureManager).when(imClientFactory)
        .build(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());
    assertThat(imService.doUndeploy(dm)).isTrue();
  }


  @Test
  public void testDoUndeployFail() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    deployment.setEndpoint("endpoint");
    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    Mockito.when(deploymentRepository.save(deployment)).thenReturn(deployment);
    Mockito.doReturn(infrastructureManager).when(imClientFactory)
        .build(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());
    ResponseError responseError = new ResponseError(null, 405);
    Mockito.doThrow(new ImClientErrorException(responseError)).when(infrastructureManager)
        .destroyInfrastructureAsync(Mockito.any(String.class));
    Mockito.doNothing().when(deploymentStatusHelper).updateOnError(Mockito.anyString(), Mockito.anyString());

    assertThatThrownBy(() -> imService.doUndeploy(dm)).isInstanceOf(DeploymentException.class);
  }

  @Test
  public void testDoUndeployFailNullPointerException() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);

    deployment.setEndpoint("endpoint");
    Mockito.when(deploymentRepository.save(deployment)).thenReturn(deployment);
    Mockito.doReturn(infrastructureManager).when(imClientFactory)
        .build(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());
    ResponseError responseError = new ResponseError(null, 405);
    Mockito.doThrow(new ImClientErrorException(responseError)).when(infrastructureManager)
        .destroyInfrastructure(Mockito.any(String.class));
    Mockito.doNothing().when(deploymentStatusHelper).updateOnError(Mockito.anyString(), Mockito.anyString());

    Mockito.doThrow(new NullPointerException()).when(infrastructureManager)
        .destroyInfrastructureAsync(Mockito.any(String.class));

    assertThatThrownBy(() -> imService.doUndeploy(dm)).isInstanceOf(NullPointerException.class);
  }

  @Test
  public void testIsUndeployedSuccess() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    String infrastructureId = UUID.randomUUID().toString();
    InfrastructureUri infrastructureUri =
        new InfrastructureUri("http://localhost:8080/infrastructures/" + infrastructureId);
    ArchiveRoot ar = new ArchiveRoot();

    Mockito.when(deploymentRepository.save(deployment)).thenReturn(deployment);
    Mockito.when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);
    Mockito.doReturn(ar).when(toscaService).prepareTemplate(deployment.getTemplate(),
        deployment.getParameters());
    Mockito.when(infrastructureManager.createInfrastructureAsync(Mockito.anyString(),
        Mockito.eq(BodyContentType.TOSCA))).thenReturn(infrastructureUri);
    Mockito.doReturn(infrastructureManager).when(imClientFactory)
        .build(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());

    assertThat(imService.isUndeployed(dm)).isTrue();

  }

  @Test
  public void testIsUndeployedFailImClientErrorException() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    deployment.setEndpoint("www.example.com");
    String infrastructureId = UUID.randomUUID().toString();
    InfrastructureUri infrastructureUri =
        new InfrastructureUri("http://localhost:8080/infrastructures/" + infrastructureId);
    ArchiveRoot ar = new ArchiveRoot();

    Mockito.when(deploymentRepository.save(deployment)).thenReturn(deployment);
    Mockito.when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);
    Mockito.doReturn(ar).when(toscaService).prepareTemplate(deployment.getTemplate(),
        deployment.getParameters());
    Mockito.when(infrastructureManager.createInfrastructureAsync(Mockito.anyString(),
        Mockito.eq(BodyContentType.TOSCA))).thenReturn(infrastructureUri);
    Mockito.doReturn(infrastructureManager).when(imClientFactory)
        .build(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());

    Mockito.doThrow(new ImClientErrorException(new ResponseError(null, 500)))
        .when(infrastructureManager).getInfrastructureState(Mockito.any(String.class));

    assertThatThrownBy(() -> imService.isUndeployed(dm)).isInstanceOf(DeploymentException.class);
  }

  @Test
  public void testIsUndeployedFail() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    deployment.setEndpoint("www.example.com");
    String infrastructureId = UUID.randomUUID().toString();
    InfrastructureUri infrastructureUri =
        new InfrastructureUri("http://localhost:8080/infrastructures/" + infrastructureId);
    ArchiveRoot ar = new ArchiveRoot();

    Mockito.when(deploymentRepository.save(deployment)).thenReturn(deployment);
    Mockito.when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);
    Mockito.doReturn(ar).when(toscaService).prepareTemplate(deployment.getTemplate(),
        deployment.getParameters());
    Mockito.when(infrastructureManager.createInfrastructureAsync(Mockito.anyString(),
        Mockito.eq(BodyContentType.TOSCA))).thenReturn(infrastructureUri);
    Mockito.doReturn(infrastructureManager).when(imClientFactory)
        .build(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());

    InfrastructureState infrastructureState = generateInfrastructureState(States.RUNNING, 2);
    Mockito.when(infrastructureManager.getInfrastructureState(deployment.getEndpoint()))
        .thenReturn(infrastructureState);

    assertThat(imService.isUndeployed(dm)).isFalse();
  }


  @Test
  public void testIsUndeployedFailImClientException() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    deployment.setEndpoint("www.example.com");
    String infrastructureId = UUID.randomUUID().toString();
    InfrastructureUri infrastructureUri =
        new InfrastructureUri("http://localhost:8080/infrastructures/" + infrastructureId);
    ArchiveRoot ar = new ArchiveRoot();

    Mockito.when(deploymentRepository.save(deployment)).thenReturn(deployment);
    Mockito.when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);
    Mockito.doReturn(ar).when(toscaService).prepareTemplate(deployment.getTemplate(),
        deployment.getParameters());
    Mockito.when(infrastructureManager.createInfrastructureAsync(Mockito.anyString(),
        Mockito.eq(BodyContentType.TOSCA))).thenReturn(infrastructureUri);
    Mockito.doReturn(infrastructureManager).when(imClientFactory)
        .build(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());

    Mockito.doThrow(new ImClientException()).when(infrastructureManager)
        .getInfrastructureState(Mockito.any(String.class));

    assertThatThrownBy(() -> imService.isUndeployed(dm)).isInstanceOf(DeploymentException.class);
  }

  private void mockMethodForDoUpdate(DeploymentMessage dm, Deployment deployment, InfrastructureUri infrastructureUri,
      ArchiveRoot oldAr, ArchiveRoot newAr) throws Exception {
    Mockito.when(deploymentRepository.save(deployment)).thenReturn(deployment);
    Mockito.when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);
    Mockito.doReturn(oldAr).when(toscaService).prepareTemplate(deployment.getTemplate(),
        deployment.getParameters());
    Mockito.doReturn(newAr).when(toscaService).prepareTemplate("newTemplate",
        deployment.getParameters());
    Mockito.when(infrastructureManager.createInfrastructureAsync(Mockito.anyString(),
        Mockito.eq(BodyContentType.TOSCA))).thenReturn(infrastructureUri);
    Mockito.doReturn(infrastructureManager).when(imClientFactory)
        .build(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());
  }


  @Test
  public void testDoUpdateNewNodeSuccesful() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    ComputeService cs = ComputeService
        .computeBuilder()
        .endpoint("http://example.com")
        .providerId("cloud-provider-id-1")
        .id("cloud-service-id-1")
        .type(CloudServiceType.COMPUTE)
        .endpoint("http://example.com")
        .serviceType(OPENSTACK_COMPUTE_SERVICE)
        .hostname("example.com")
        .build();
    CloudServicesOrderedIterator csi = new CloudServicesOrderedIterator(Lists.newArrayList(new CloudServiceWf(cs)));
    csi.next();
    dm.setCloudServicesOrderedIterator(csi);

    String infrastructureId = UUID.randomUUID().toString();
    InfrastructureUri infrastructureUri =
        new InfrastructureUri("http://localhost:8080/infrastructures/" + infrastructureId);

    ArchiveRoot oldAr = new ArchiveRoot();
    Topology t1 = new Topology();
    oldAr.setTopology(t1);

    // old node
    NodeTemplate ntOld = new NodeTemplate();
    ntOld.setName("oldNode");
    Map<String, NodeTemplate> oldNodes = new HashMap<>();
    oldNodes.put(ntOld.getName(), ntOld);

    oldAr.getTopology().setNodeTemplates(oldNodes);

    ArchiveRoot newAr = new ArchiveRoot();
    Topology t2 = new Topology();
    newAr.setTopology(t2);

    // new node
    NodeTemplate ntNew = new NodeTemplate();
    ntNew.setName("newNode");
    NodeTemplate ntNew2 = new NodeTemplate();
    ntNew2.setName("newNode2");
    NodeTemplate ntNew3 = new NodeTemplate();
    ntNew3.setName("newNode3");
    Map<String, NodeTemplate> newNodes = new HashMap<>();
    newNodes.put(ntOld.getName(), ntOld);
    newNodes.put(ntNew.getName(), ntNew);
    newNodes.put(ntNew2.getName(), ntNew2);
    newNodes.put(ntNew3.getName(), ntNew3);

    newAr.getTopology().setNodeTemplates(newNodes);

    mockMethodForDoUpdate(dm, deployment, infrastructureUri, oldAr, newAr);

    Mockito.doReturn(oldNodes.values()).when(toscaService).getScalableNodes(oldAr);
    Mockito.doReturn(newNodes.values()).when(toscaService).getScalableNodes(newAr);

    Mockito.doReturn(deployment.getTemplate()).when(toscaService)
        .updateTemplate(Mockito.anyString());
    Mockito.doReturn(newAr).when(toscaService).parseAndValidateTemplate("newTemplate",
        deployment.getParameters());
    Mockito.doNothing().when(indigoInputsPreProcessorService).processGetInputAttributes(eq(newAr),
        eq(deployment.getParameters()), Mockito.any());

    InfrastructureState infrastructureState = generateInfrastructureState(States.CONFIGURED, 1);
    Mockito.when(infrastructureManager.getInfrastructureState(deployment.getEndpoint()))
        .thenReturn(infrastructureState);

    assertThat(imService.doUpdate(dm, "newTemplate")).isTrue();
  }

  @Test
  @Parameters({"false", "true"})
  public void testDoUpdateNoNewNodeSuccesful(boolean failservice) throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    CloudService cs = CloudService
        .builder()
        .endpoint("http://example.com")
        .providerId("cloud-provider-id-1")
        .id("cloud-service-id-1")
        .type(CloudServiceType.STORAGE)
        .endpoint("http://example.com")
        .serviceType(ONEPROVIDER_STORAGE_SERVICE)
        .hostname("example.com")
        .build();
    ComputeService cs2 = ComputeService
        .computeBuilder()
        .endpoint("http://example.com")
        .providerId("cloud-provider-id-2")
        .id("cloud-service-id-2")
        .type(CloudServiceType.COMPUTE)
        .endpoint("http://example.com")
        .serviceType(OPENSTACK_COMPUTE_SERVICE)
        .hostname("example.com")
        .build();
    List<CloudService> services = new ArrayList<>();
    services.add(cs);
    if (!failservice) {
      services.add(cs2);
    }
    CloudServicesOrderedIterator csi = new CloudServicesOrderedIterator(services.stream().map(
        CloudServiceWf::new).collect(Collectors.toList()));
    csi.next();
    dm.setCloudServicesOrderedIterator(csi);

    String infrastructureId = UUID.randomUUID().toString();
    InfrastructureUri infrastructureUri =
        new InfrastructureUri("http://localhost:8080/infrastructures/" + infrastructureId);

    ArchiveRoot oldAr = new ArchiveRoot();
    Topology t1 = new Topology();
    oldAr.setTopology(t1);

    // old node
    NodeTemplate ntOld = new NodeTemplate();
    ntOld.setName("oldNode");
    Map<String, NodeTemplate> oldNodes = new HashMap<>();
    oldNodes.put(ntOld.getName(), ntOld);

    oldAr.getTopology().setNodeTemplates(oldNodes);

    ArchiveRoot newAr = new ArchiveRoot();
    Topology t2 = new Topology();
    newAr.setTopology(t2);

    // new node
    NodeTemplate ntNew = new NodeTemplate();
    ntNew.setName("newNode");
    Map<String, NodeTemplate> newNodes = new HashMap<>();
    newNodes.put(ntNew.getName(), ntNew);

    newAr.getTopology().setNodeTemplates(newNodes);

    Mockito.doReturn(oldNodes.values()).when(toscaService).getScalableNodes(oldAr);
    Mockito.doReturn(newNodes.values()).when(toscaService).getScalableNodes(newAr);
    mockMethodForDoUpdate(dm, deployment, infrastructureUri, oldAr, newAr);
    List<String> removalList = new ArrayList<>();
    String id = UUID.randomUUID().toString();
    removalList.add(id);
    Mockito.doReturn(removalList).when(toscaService).getRemovalList(Mockito.anyObject());
    Resource resource2 = new Resource();
    resource2.setIaasId(UUID.randomUUID().toString());
    Mockito.when(resourceRepository.findOne(id)).thenReturn(resource2);

    Mockito.doReturn(deployment.getTemplate()).when(toscaService)
        .updateTemplate(Mockito.anyString());
    Mockito.doReturn(resource2).when(resourceRepository).save(resource2);
    Mockito.doReturn(deployment.getTemplate()).when(toscaService)
        .updateTemplate(Mockito.anyString());

    InfrastructureState infrastructureState = generateInfrastructureState(States.CONFIGURED, 2);
    Mockito.when(infrastructureManager.getInfrastructureState(deployment.getEndpoint()))
        .thenReturn(infrastructureState);
    Mockito.doReturn(newAr).when(toscaService).parseAndValidateTemplate(deployment.getTemplate(),
        deployment.getParameters());
    Mockito.doNothing().when(indigoInputsPreProcessorService).processGetInputAttributes(eq(newAr),
        eq(deployment.getParameters()), Mockito.any());
    if (!failservice) {
      assertThat(imService.doUpdate(dm, deployment.getTemplate())).isTrue();
    } else {
      assertThatThrownBy(() -> imService.doUpdate(dm, deployment.getTemplate()))
          .isInstanceOf(DeploymentException.class);
    }
  }


  @Test
  public void testDoUpdateOrchestratorException() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    String infrastructureId = UUID.randomUUID().toString();
    InfrastructureUri infrastructureUri =
        new InfrastructureUri("http://localhost:8080/infrastructures/" + infrastructureId);

    ArchiveRoot oldAr = new ArchiveRoot();
    Topology topology = new Topology();
    oldAr.setTopology(topology);

    ArchiveRoot newAr = new ArchiveRoot();
    newAr.setTopology(topology);


    mockMethodForDoUpdate(dm, deployment, infrastructureUri, oldAr, newAr);

    Mockito.doThrow(new ToscaException("string")).when(toscaService)
        .parseAndValidateTemplate(Mockito.anyString(), Mockito.anyObject());

    assertThatThrownBy(() -> imService.doUpdate(dm, "newTemplate"))
        .isInstanceOf(OrchestratorException.class);

  }


  @Test
  public void testDoUpdateImClientException() throws Exception {

    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    String infrastructureId = UUID.randomUUID().toString();
    InfrastructureUri infrastructureUri =
        new InfrastructureUri("http://localhost:8080/infrastructures/" + infrastructureId);

    ArchiveRoot oldAr = new ArchiveRoot();
    Topology t1 = new Topology();
    oldAr.setTopology(t1);

    // old node
    NodeTemplate ntOld = new NodeTemplate();
    ntOld.setName("oldNode");
    Map<String, NodeTemplate> oldNodes = new HashMap<>();
    oldNodes.put(ntOld.getName(), ntOld);

    oldAr.getTopology().setNodeTemplates(oldNodes);

    ArchiveRoot newAr = new ArchiveRoot();
    Topology t2 = new Topology();
    newAr.setTopology(t2);

    // new node
    NodeTemplate ntNew = new NodeTemplate();
    ntNew.setName("newNode");
    NodeTemplate ntNew2 = new NodeTemplate();
    ntNew2.setName("newNode2");
    NodeTemplate ntNew3 = new NodeTemplate();
    ntNew3.setName("newNode3");
    Map<String, NodeTemplate> newNodes = new HashMap<>();
    newNodes.put(ntOld.getName(), ntOld);
    newNodes.put(ntNew.getName(), ntNew);
    newNodes.put(ntNew2.getName(), ntNew2);
    newNodes.put(ntNew3.getName(), ntNew3);

    newAr.getTopology().setNodeTemplates(newNodes);

    List<String> removalList = new ArrayList<>();
    String id = UUID.randomUUID().toString();
    removalList.add(id);

    Resource resource = new Resource();
    resource.setIaasId(UUID.randomUUID().toString());

    mockMethodForDoUpdate(dm, deployment, infrastructureUri, oldAr, newAr);
    Mockito.doReturn(oldNodes.values()).when(toscaService).getScalableNodes(oldAr);
    Mockito.doReturn(newNodes.values()).when(toscaService).getScalableNodes(newAr);

    Mockito.doReturn(removalList).when(toscaService).getRemovalList(Mockito.anyObject());

    Mockito.doReturn(deployment.getTemplate()).when(toscaService)
        .updateTemplate(Mockito.anyString());
    Mockito.doReturn(newAr).when(toscaService).parseAndValidateTemplate("newTemplate",
        deployment.getParameters());
    Mockito.doNothing().when(indigoInputsPreProcessorService).processGetInputAttributes(eq(newAr),
        eq(deployment.getParameters()), Mockito.any());
    Mockito.when(resourceRepository.findOne(id)).thenReturn(resource);
    Mockito.doReturn(resource).when(resourceRepository).save(resource);
    InfrastructureManager im = mock(InfrastructureManager.class);
    Mockito.doReturn(im).when(imClientFactory).build(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());
    Mockito.doThrow(new ImClientErrorException(new ResponseError("message", 404))).when(im)
        .getInfrastructureState(deployment.getEndpoint());

    assertThatThrownBy(() -> imService.doUpdate(dm, "newTemplate"))
        .isInstanceOf(DeploymentException.class);
  }

}
