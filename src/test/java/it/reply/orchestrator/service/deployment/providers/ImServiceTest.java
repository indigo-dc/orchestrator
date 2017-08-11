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
import com.google.common.collect.Maps;

import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingException;

import es.upv.i3m.grycap.im.InfrastructureManager;
import es.upv.i3m.grycap.im.States;
import es.upv.i3m.grycap.im.auth.credentials.providers.ImCredentials;
import es.upv.i3m.grycap.im.auth.credentials.providers.OpenStackCredentials;
import es.upv.i3m.grycap.im.exceptions.ImClientErrorException;
import es.upv.i3m.grycap.im.exceptions.ImClientException;
import es.upv.i3m.grycap.im.exceptions.InfrastructureUuidNotFoundException;
import es.upv.i3m.grycap.im.pojo.InfOutputValues;
import es.upv.i3m.grycap.im.pojo.InfrastructureState;
import es.upv.i3m.grycap.im.pojo.InfrastructureUri;
import es.upv.i3m.grycap.im.pojo.InfrastructureUris;
import es.upv.i3m.grycap.im.pojo.ResponseError;
import es.upv.i3m.grycap.im.pojo.VirtualMachineInfo;
import es.upv.i3m.grycap.im.rest.client.BodyContentType;

import it.reply.orchestrator.config.properties.ImProperties;
import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.controller.ControllerTestUtils;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.CloudProviderEndpoint.IaaSType;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.exception.service.ToscaException;
import it.reply.orchestrator.service.ToscaServiceImpl;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.orchestrator.util.TestUtil;
import it.reply.orchestrator.utils.CommonUtils;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.assertj.core.internal.Conditions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.data.domain.PageImpl;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ImServiceTest {

  @Spy
  @InjectMocks
  private ImServiceImpl imService;

  @Spy
  private ToscaServiceImpl toscaService;

  @Spy
  @InjectMocks
  private DeploymentStatusHelper deploymentStatusHelper = new DeploymentStatusHelperImpl();

  @Mock
  private DeploymentRepository deploymentRepository;

  @Mock
  private ResourceRepository resourceRepository;

  @Mock
  private InfrastructureManager infrastructureManager;

  @Spy
  private ImProperties imProperties;

  @Mock
  private OidcProperties oidcProperties;

  @Mock
  private OAuth2TokenService oauth2TokenService;

  @Before
  public void setup() throws ParsingException {
    MockitoAnnotations.initMocks(this);
    imProperties.setUrl(CommonUtils.checkNotNull(URI.create("im.url")));
  }

  private DeploymentMessage generateIsDeployedDm() {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    String infrastructureId = UUID.randomUUID().toString();
    deployment.setEndpoint(infrastructureId);
    deployment.setTask(Task.DEPLOYER);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    return dm;
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

    String infrastructureId = UUID.randomUUID().toString();
    InfrastructureUri infrastructureUri =
        new InfrastructureUri("http://localhost:8080/infrastructures/" + infrastructureId);
    ArchiveRoot ar = new ArchiveRoot();

    Mockito.when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);

    Mockito.doReturn(ar).when(toscaService).prepareTemplate(deployment.getTemplate(),
        deployment.getParameters());
    Mockito.when(infrastructureManager.createInfrastructure(Mockito.anyString(),
        Mockito.eq(BodyContentType.TOSCA))).thenReturn(infrastructureUri);
    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());

    boolean returnValue = imService.doDeploy(dm);

    Assert.assertEquals(deployment.getTask(), Task.DEPLOYER);
    Assert.assertEquals(deployment.getStatus(), Status.CREATE_IN_PROGRESS);
    Assert.assertEquals(deployment.getDeploymentProvider(), DeploymentProvider.IM);
    Assert.assertEquals(deployment.getEndpoint(), infrastructureId);
    Assert.assertEquals(deployment.getResources().size(), 2);
    Assertions.assertThat(deployment.getResources()).extracting(Resource::getState).allMatch(NodeStates.CREATING::equals);
    Assert.assertTrue(returnValue);
  }

  @Test
  public void testDoDeployNoId()
      throws ToscaException, ParsingException, IOException, ImClientException {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    InfrastructureUri infrastructureUri =
        new InfrastructureUri("http://localhost:8080/infrastructures/");
    ArchiveRoot ar = new ArchiveRoot();

    Mockito.when(deploymentRepository.save(deployment)).thenReturn(deployment);
    Mockito.when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);
    Mockito.doReturn(ar).when(toscaService).prepareTemplate(deployment.getTemplate(),
        deployment.getParameters());
    Mockito.when(infrastructureManager.createInfrastructure(Mockito.anyString(),
        Mockito.eq(BodyContentType.TOSCA))).thenReturn(infrastructureUri);
    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());
    try {
      imService.doDeploy(dm);
    } catch (DeploymentException ex) {
      Assertions.assertThat(ex.getCause()).isInstanceOf(InfrastructureUuidNotFoundException.class);
    }
  }

  @Test
  public void testDoDeployIMexception()
      throws ToscaException, ParsingException, IOException, ImClientException {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    ArchiveRoot ar = new ArchiveRoot();
    ImClientErrorException imException =
        new ImClientErrorException(new ResponseError("Error", 500));

    Mockito.when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);

    Mockito.doReturn(ar).when(toscaService).prepareTemplate(deployment.getTemplate(),
        deployment.getParameters());
    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());
    Mockito.when(infrastructureManager.createInfrastructure(Mockito.anyString(),
        Mockito.eq(BodyContentType.TOSCA))).thenThrow(imException);
    try {
      imService.doDeploy(dm);
    } catch (DeploymentException ex) {
      Assertions.assertThat(ex.getCause()).isInstanceOf(ImClientException.class);
    }
  }

  @Test
  public void testIsDeployedSuccesful() throws ImClientException {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    deployment.setTask(Task.DEPLOYER);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    InfrastructureState infrastructureState = generateInfrastructureState(States.CONFIGURED, 2);

    List<VirtualMachineInfo> info= generateVirtualMachineInfo(2);
    
    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());
    Mockito.when(infrastructureManager.getInfrastructureState(deployment.getEndpoint()))
        .thenReturn(infrastructureState);
    Mockito
        .when(infrastructureManager.getVmInfo(Mockito.eq(deployment.getEndpoint()), Mockito.anyString()))
        .thenReturn(info.get(0), info.get(1));
    Mockito.when(resourceRepository
            .findByDeployment_id(deployment.getId())).thenReturn(new ArrayList<>(deployment.getResources()));
    
    boolean returnValue = imService.isDeployed(dm);

    Assert.assertEquals(deployment.getTask(), Task.DEPLOYER);
    Assert.assertEquals(deployment.getStatus(), Status.CREATE_IN_PROGRESS);
    Assert.assertEquals(deployment.getDeploymentProvider(), DeploymentProvider.IM);
    Assert.assertEquals(deployment.getEndpoint(), deployment.getEndpoint());
    Assert.assertEquals(deployment.getResources().size(), 2);
    Assertions
        .assertThat(deployment.getResources())
        .extracting(Resource::getState)
        .allMatch(NodeStates.CREATING::equals);
    Assert.assertTrue(returnValue);
  }

  @Test(expected = DeploymentException.class)
  public void testIsDeployedFail() throws ImClientException {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    Mockito.when(deploymentRepository.findOne(dm.getDeploymentId())).thenReturn(deployment);

    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());
    Mockito.doThrow(new ImClientException()).when(infrastructureManager)
        .getInfrastructureState(Mockito.anyString());

    imService.isDeployed(dm);
  }

  @Test(expected=DeploymentException.class)
  public void testFinalizeDeployImClientError() throws ImClientException {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    Mockito.when(deploymentRepository.findOne(dm.getDeploymentId())).thenReturn(deployment);

    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());
    Mockito.doThrow(new ImClientErrorException(new ResponseError("Not Found", 404)))
        .when(infrastructureManager).getInfrastructureOutputs(Mockito.anyString());

    imService.finalizeDeploy(dm);
  }

  @Test(expected=DeploymentException.class)
  public void testFinalizeDeployGenericExceptionError() throws ImClientException {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    Mockito.when(deploymentRepository.findOne(dm.getDeploymentId())).thenReturn(deployment);

    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());
    Mockito.doThrow(new ImClientException()).when(infrastructureManager)
        .getInfrastructureOutputs(Mockito.anyString());

    imService.finalizeDeploy(dm);
  }

  @Test
  public void testUpdateOnErrorDeleteStatus() {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);
    deployment.setStatus(Status.DELETE_FAILED);
    Mockito.when(deploymentRepository.findOne(dm.getDeploymentId())).thenReturn(deployment);
    imService.updateOnError(dm.getDeploymentId(), "message");
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
    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());
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

    Assert.assertEquals(deployment.getTask(), Task.DEPLOYER);
    Assert.assertEquals(deployment.getStatus(), Status.CREATE_IN_PROGRESS);
    Assert.assertEquals(deployment.getDeploymentProvider(), DeploymentProvider.IM);
    Assert.assertEquals(deployment.getEndpoint(), deployment.getEndpoint());
    Assert.assertEquals(deployment.getResources().size(), 2);
    Assertions.assertThat(deployment.getResources()).extracting(Resource::getState).allMatch(NodeStates.CREATING::equals);
    Assert.assertFalse(returnValue);
  }

  @Test
  public void testIsDeployedFailedInfrastructureStatus() throws ImClientException {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    deployment.setTask(Task.DEPLOYER);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    InfrastructureState infrastructureState = generateInfrastructureState(States.FAILED, 2);
    List<VirtualMachineInfo> info= generateVirtualMachineInfo(2);

    Mockito.when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);
    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());

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
    
    try {
      imService.isDeployed(dm);
      Assert.fail();
    } catch (DeploymentException ex) {
      Assert
          .assertEquals(
              "Some error occurred during the contextualization of the IM infrastructure\n"
                  +
                  infrastructureState.getFormattedInfrastructureStateString(),
              ex.getMessage());
    }
  }

  @Test
  public void testIsDeployedUnconfiguredInfrastructureStatus() throws ImClientException {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    deployment.setTask(Task.DEPLOYER);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    InfrastructureState infrastructureState = generateInfrastructureState(States.UNCONFIGURED, 2);
    List<VirtualMachineInfo> info= generateVirtualMachineInfo(2);

    Mockito.when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);
    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());

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
    
    try {
      imService.isDeployed(dm);
      Assert.fail();
    } catch (DeploymentException ex) {
      Assert
          .assertEquals(
              "Some error occurred during the contextualization of the IM infrastructure\n"
                  +
                  infrastructureState.getFormattedInfrastructureStateString(),
              ex.getMessage());
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
    vmInfo0.getVmProperties().get(0).put("id",
        resources.get(1).getToscaNodeName());

    Mockito.when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);
    Mockito.when(deploymentRepository.save(deployment)).thenReturn(deployment);
    Mockito.when(resourceRepository.findByDeployment_id(deployment.getId()))
        .thenReturn(resources);
    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());

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

    Assert.assertEquals(deployment.getTask(), Task.NONE);
    Assert.assertEquals(deployment.getStatus(), Status.CREATE_COMPLETE);
    Assert.assertEquals(deployment.getDeploymentProvider(), DeploymentProvider.IM);
    Assert.assertEquals(deployment.getEndpoint(), deployment.getEndpoint());
    Assert.assertEquals(deployment.getResources().size(), 2);
    Assertions.assertThat(deployment.getResources()).extracting(Resource::getState).allMatch(NodeStates.STARTED::equals);
    Assert.assertFalse(dm.isPollComplete());
  }

  @Test
  public void testUpdateOnErrorDeleteInProgress() {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);

    String id = deployment.getId();
    
    Mockito.when(deploymentRepository.findOne(id)).thenReturn(deployment);

    deployment.setStatus(Status.DELETE_IN_PROGRESS);
    imService.updateOnError(id, new RuntimeException());
    Assert.assertEquals(Status.DELETE_FAILED, deployment.getStatus());

  }
  
  @Test
  public void testUpdateOnErrorUpdateInProgress() {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);

    String id = deployment.getId();
    
    Mockito.when(deploymentRepository.findOne(id)).thenReturn(deployment);

    deployment.setStatus(Status.UPDATE_IN_PROGRESS);
    imService.updateOnError(id, new RuntimeException());
    Assert.assertEquals(Status.UPDATE_FAILED, deployment.getStatus());
  }
  
  
  @Test
  public void testUpdateOnErrorUnknown() {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);

    String id = deployment.getId();
    
    Mockito.when(deploymentRepository.findOne(id)).thenReturn(deployment);


    deployment.setStatus(Status.UNKNOWN);
    imService.updateOnError(id, new RuntimeException());
    Assert.assertEquals(Status.UNKNOWN, deployment.getStatus());
  }
  
  
  
  @Test
  public void testUpdateOnError() {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);

    String id = deployment.getId();
    
    Mockito.when(deploymentRepository.findOne(id)).thenReturn(deployment);

    deployment.setStatus(Status.DELETE_IN_PROGRESS);
    imService.updateOnError(id, new RuntimeException());
    Assert.assertEquals(Status.DELETE_FAILED, deployment.getStatus());

    deployment.setStatus(Status.UPDATE_IN_PROGRESS);
    imService.updateOnError(id, new RuntimeException());
    Assert.assertEquals(Status.UPDATE_FAILED, deployment.getStatus());

    deployment.setStatus(Status.UNKNOWN);
    imService.updateOnError(id, new RuntimeException());
    Assert.assertEquals(Status.UNKNOWN, deployment.getStatus());
  }
  
  

  @Test
  public void testGetClientOpenStack() throws Exception {
    CloudProviderEndpoint cloudProviderEndpoint = new CloudProviderEndpoint();
    cloudProviderEndpoint.setIaasType(IaaSType.OPENSTACK);
    cloudProviderEndpoint.setCpEndpoint("https://recas.ba.infn/");
    cloudProviderEndpoint.setCpComputeServiceId(UUID.randomUUID().toString());

    Mockito.when(oidcProperties.isEnabled()).thenReturn(true);
    OidcTokenId id = new OidcTokenId();
    Mockito.when(oauth2TokenService.getAccessToken(id, OAuth2TokenService.REQUIRED_SCOPES))
        .thenReturn("J1qK1c18UUGJFAzz9xnH56584l4");
    InfrastructureManager client = imService.getClient(Lists.newArrayList(cloudProviderEndpoint), id);
    
    // result
    OpenStackCredentials cred = OpenStackCredentials.buildCredentials()
         .withTenant("oidc")
         .withUsername("indigo-dc")
         .withPassword("J1qK1c18UUGJFAzz9xnH56584l4")
         .withHost("https://recas.ba.infn");
    
    String imAuthHeader = ImCredentials.buildCredentials().withToken("J1qK1c18UUGJFAzz9xnH56584l4").serialize();
    String imUrl = imProperties.getUrl().toString();
    
    InfrastructureManager result = new InfrastructureManager(imUrl, String.format("%s\\n%s", imAuthHeader, cred.serialize()));
    // TO-DO: Assert equals both result and client (How?)

    cloudProviderEndpoint.setCpEndpoint("https://www.openstack.org/");
    imService.getClient(Lists.newArrayList(cloudProviderEndpoint), id);
    // TO-DO: Assert equals both result and client

  }
  
  @Test
  public void testGetClientOpenNebula() throws Exception {
    CloudProviderEndpoint cloudProviderEndpoint = new CloudProviderEndpoint();
    cloudProviderEndpoint.setIaasType(IaaSType.OPENNEBULA);
    cloudProviderEndpoint.setCpEndpoint("https://recas.ba.infn/");
    cloudProviderEndpoint.setCpComputeServiceId(UUID.randomUUID().toString());

    Mockito.when(oidcProperties.isEnabled()).thenReturn(true);
    OidcTokenId id = new OidcTokenId();

    Mockito.when(oauth2TokenService.getAccessToken(id, OAuth2TokenService.REQUIRED_SCOPES))
        .thenReturn("J1qK1c18UUGJFAzz9xnH56584l4");
    InfrastructureManager client = imService.getClient(Lists.newArrayList(cloudProviderEndpoint), id);

    // TO-DO: Assert equals both result and client

    // AWS
    cloudProviderEndpoint.setIaasType(IaaSType.AWS);
    cloudProviderEndpoint.setUsername("username");
    cloudProviderEndpoint.setPassword("password");
    imService.getClient(Lists.newArrayList(cloudProviderEndpoint), id);
    // TO-DO: Assert equals both result and client
  }
  
  
  @Test
  public void testGetClientAWS() throws Exception {
    CloudProviderEndpoint cloudProviderEndpoint = new CloudProviderEndpoint();
    cloudProviderEndpoint.setIaasType(IaaSType.AWS);
    cloudProviderEndpoint.setUsername("username");
    cloudProviderEndpoint.setPassword("password");
    cloudProviderEndpoint.setCpEndpoint("https://recas.ba.infn/");
    cloudProviderEndpoint.setCpComputeServiceId(UUID.randomUUID().toString());

    Mockito.when(oidcProperties.isEnabled()).thenReturn(true);
    OidcTokenId id = new OidcTokenId();
    Mockito.when(oauth2TokenService.getAccessToken(id, OAuth2TokenService.REQUIRED_SCOPES))
        .thenReturn("J1qK1c18UUGJFAzz9xnH56584l4");
    InfrastructureManager client = imService.getClient(Lists.newArrayList(cloudProviderEndpoint), id);

    // TO-DO: Assert equals both result and client
  }


  
  @Test(expected = DeploymentException.class)
  public void testGetClientFailDeployment() throws Exception {
    CloudProviderEndpoint cloudProviderEndpoint = new CloudProviderEndpoint();
    cloudProviderEndpoint.setIaasType(IaaSType.OPENSTACK);
    cloudProviderEndpoint.setCpComputeServiceId(UUID.randomUUID().toString());
    cloudProviderEndpoint.setCpEndpoint("lorem.ipsum");

    Mockito.when(oidcProperties.isEnabled()).thenReturn(true);
    OidcTokenId id = new OidcTokenId();
    Mockito.when(oauth2TokenService.getAccessToken(id, OAuth2TokenService.REQUIRED_SCOPES))
        .thenReturn("J1qK1c18UUGJFAzz9xnH56584l4");

    imService.getClient(Lists.newArrayList(cloudProviderEndpoint), id);
  }

  @Test
  public void testDoUndeploySuccess() throws ImClientException {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);
    
    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    
    Assert.assertTrue(imService.doUndeploy(dm));
  }

  @Test
  public void testDoUndeploySuccessDeleteComplete() throws ImClientException {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);
    
    deployment.setEndpoint("endpoint");
    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());
    Assert.assertTrue(imService.doUndeploy(dm));
  }


  @Test(expected=DeploymentException.class)
  public void testDoUndeployFail() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);
    
    deployment.setEndpoint("endpoint");
    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    Mockito.when(deploymentRepository.save(deployment)).thenReturn(deployment);
    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());
    ResponseError responseError = new ResponseError(null, 405);
    Mockito.doThrow(new ImClientErrorException(responseError)).when(infrastructureManager)
        .destroyInfrastructure(Mockito.any(String.class));
    Mockito.doNothing().when(imService).updateOnError(Mockito.anyString(), Mockito.anyString());
    imService.doUndeploy(dm);
  }

  @Test(expected=NullPointerException.class)
  public void testDoUndeployFailNullPointerException() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);
    
    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    
    deployment.setEndpoint("endpoint");
    Mockito.when(deploymentRepository.save(deployment)).thenReturn(deployment);
    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());
    ResponseError responseError = new ResponseError(null, 405);
    Mockito.doThrow(new ImClientErrorException(responseError)).when(infrastructureManager)
        .destroyInfrastructure(Mockito.any(String.class));
    Mockito.doNothing().when(imService).updateOnError(Mockito.anyString(), Mockito.anyString());

    Mockito.doThrow(new NullPointerException()).when(infrastructureManager)
        .destroyInfrastructure(Mockito.any(String.class));
    Assert.assertFalse(imService.doUndeploy(dm));
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
    Mockito.when(infrastructureManager.createInfrastructure(Mockito.anyString(),
        Mockito.eq(BodyContentType.TOSCA))).thenReturn(infrastructureUri);
    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());

    Assert.assertTrue(imService.isUndeployed(dm));

  }

  @Test(expected=DeploymentException.class)
  public void testIsUndeployedFailImClientErrorException() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);
    
    deployment.setEndpoint("www.endpoint.com");
    String infrastructureId = UUID.randomUUID().toString();
    InfrastructureUri infrastructureUri =
        new InfrastructureUri("http://localhost:8080/infrastructures/" + infrastructureId);
    ArchiveRoot ar = new ArchiveRoot();

    Mockito.when(deploymentRepository.save(deployment)).thenReturn(deployment);
    Mockito.when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);
    Mockito.doReturn(ar).when(toscaService).prepareTemplate(deployment.getTemplate(),
        deployment.getParameters());
    Mockito.when(infrastructureManager.createInfrastructure(Mockito.anyString(),
        Mockito.eq(BodyContentType.TOSCA))).thenReturn(infrastructureUri);
    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());

    Mockito.doThrow(new ImClientErrorException(new ResponseError(null, 500)))
        .when(infrastructureManager).getInfrastructureState(Mockito.any(String.class));
    imService.isUndeployed(dm);
  }

  @Test
  public void testIsUndeployedFail() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);
    
    deployment.setEndpoint("www.endpoint.com");
    String infrastructureId = UUID.randomUUID().toString();
    InfrastructureUri infrastructureUri =
        new InfrastructureUri("http://localhost:8080/infrastructures/" + infrastructureId);
    ArchiveRoot ar = new ArchiveRoot();

    Mockito.when(deploymentRepository.save(deployment)).thenReturn(deployment);
    Mockito.when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);
    Mockito.doReturn(ar).when(toscaService).prepareTemplate(deployment.getTemplate(),
        deployment.getParameters());
    Mockito.when(infrastructureManager.createInfrastructure(Mockito.anyString(),
        Mockito.eq(BodyContentType.TOSCA))).thenReturn(infrastructureUri);
    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());

    InfrastructureState infrastructureState = generateInfrastructureState(States.RUNNING, 2);
    Mockito.when(infrastructureManager.getInfrastructureState(deployment.getEndpoint()))
        .thenReturn(infrastructureState);

    Assert.assertFalse(imService.isUndeployed(dm));
  }


  @Test(expected=DeploymentException.class)
  public void testIsUndeployedFailImClientException() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);
    
    deployment.setEndpoint("www.endpoint.com");
    String infrastructureId = UUID.randomUUID().toString();
    InfrastructureUri infrastructureUri =
        new InfrastructureUri("http://localhost:8080/infrastructures/" + infrastructureId);
    ArchiveRoot ar = new ArchiveRoot();

    Mockito.when(deploymentRepository.save(deployment)).thenReturn(deployment);
    Mockito.when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);
    Mockito.doReturn(ar).when(toscaService).prepareTemplate(deployment.getTemplate(),
        deployment.getParameters());
    Mockito.when(infrastructureManager.createInfrastructure(Mockito.anyString(),
        Mockito.eq(BodyContentType.TOSCA))).thenReturn(infrastructureUri);
    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());

    Mockito.doThrow(new ImClientException()).when(infrastructureManager)
        .getInfrastructureState(Mockito.any(String.class));
    Assert.assertFalse(imService.isUndeployed(dm));
  }

  @Test
  public void testFinalizeUndeploy() {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);
    
    Mockito.doNothing().when(imService).updateOnSuccess(Mockito.anyString());
    imService.finalizeUndeploy(dm);
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
    Mockito.when(infrastructureManager.createInfrastructure(Mockito.anyString(),
        Mockito.eq(BodyContentType.TOSCA))).thenReturn(infrastructureUri);
    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());
  }


  @Test
  public void testDoUpdateNewNodeSuccesful() throws Exception {
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

    // old node
    NodeTemplate ntOld = new NodeTemplate();
    ntOld.setName("oldNode");
    Map<String, NodeTemplate> oldNodes = new HashMap<>();
    oldNodes.put("oldNode", ntOld);

    // new node
    NodeTemplate ntNew = new NodeTemplate();
    ntNew.setName("newNode");
    NodeTemplate ntNew2 = new NodeTemplate();
    ntNew2.setName("newNode2");
    NodeTemplate ntNew3 = new NodeTemplate();
    ntNew3.setName("newNode3");
    Map<String, NodeTemplate> newNodes = new HashMap<>();
    newNodes.put("oldNode", ntOld);
    newNodes.put("newNode", ntNew);
    newNodes.put("newNode2", ntNew2);
    newNodes.put("newNode3", ntNew3);

    mockMethodForDoUpdate(dm, deployment, infrastructureUri, oldAr, newAr);

    Mockito.doReturn(oldNodes.values()).when(toscaService).getScalableNodes(oldAr);
    Mockito.doReturn(newNodes.values()).when(toscaService).getScalableNodes(newAr);
    Mockito.doReturn(Optional.of(1)).doReturn(Optional.of(4)).when(toscaService)
        .getCount(Mockito.any(NodeTemplate.class));
    Mockito.doReturn(deployment.getTemplate()).when(toscaService)
        .updateTemplate(Mockito.anyString());

    Assert.assertTrue(imService.doUpdate(dm, "newTemplate"));

  }

  @Test
  public void testDoUpdateNoNewNodeSuccesful() throws Exception {
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

    oldAr = new ArchiveRoot();
    topology = new Topology();
    oldAr.setTopology(topology);

    newAr = new ArchiveRoot();
    newAr.setTopology(topology);

    // old node
    NodeTemplate ntOld = new NodeTemplate();
    ntOld.setName("oldNode");
    Map<String, NodeTemplate> oldNodes = new HashMap<>();
    oldNodes = new HashMap<>();
    oldNodes.put("oldNode", ntOld);

    // new node
    NodeTemplate ntNew = new NodeTemplate();
    ntNew.setName("newNode");
    Map<String, NodeTemplate> newNodes = new HashMap<>();
    newNodes.put("oldNode", ntOld);

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

    Mockito.doReturn(Optional.of(1)).doReturn(Optional.of(0)).when(toscaService)
        .getCount(Mockito.any(NodeTemplate.class));
    Mockito.doReturn(deployment.getTemplate()).when(toscaService)
        .updateTemplate(Mockito.anyString());
    Mockito.doReturn(resource2).when(resourceRepository).save(resource2);
    Mockito.doReturn(deployment.getTemplate()).when(toscaService)
        .updateTemplate(Mockito.anyString());

    Assert.assertTrue(imService.doUpdate(dm, "newTemplate"));
  }



  @Test(expected = OrchestratorException.class)
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
        .prepareTemplate(Mockito.anyString(), Mockito.anyObject());

    imService.doUpdate(dm, "newTemplate");

  }


  @Test(expected=DeploymentException.class)
  public void testDoUpdateImClientException() throws Exception {

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

    // old node
    NodeTemplate ntOld = new NodeTemplate();
    ntOld.setName("oldNode");
    Map<String, NodeTemplate> oldNodes = new HashMap<>();
    oldNodes.put("oldNode", ntOld);

    // new node
    NodeTemplate ntNew = new NodeTemplate();
    ntNew.setName("newNode");
    NodeTemplate ntNew2 = new NodeTemplate();
    ntNew2.setName("newNode2");
    NodeTemplate ntNew3 = new NodeTemplate();
    ntNew3.setName("newNode3");
    Map<String, NodeTemplate> newNodes = new HashMap<>();
    newNodes.put("oldNode", ntOld);
    newNodes.put("newNode", ntNew);
    newNodes.put("newNode2", ntNew2);
    newNodes.put("newNode3", ntNew3);

    List<String> removalList = new ArrayList<>();
    String id = UUID.randomUUID().toString();
    removalList.add(id);

    Resource resource = new Resource();
    resource.setIaasId(UUID.randomUUID().toString());

    mockMethodForDoUpdate(dm, deployment, infrastructureUri, oldAr, newAr);
    Mockito.doReturn(oldNodes.values()).when(toscaService).getScalableNodes(oldAr);
    Mockito.doReturn(newNodes.values()).when(toscaService).getScalableNodes(newAr);

    Mockito.doReturn(removalList).when(toscaService).getRemovalList(Mockito.anyObject());

    Mockito.doReturn(Optional.of(1)).doReturn(Optional.of(0)).when(toscaService)
        .getCount(Mockito.any(NodeTemplate.class));
    Mockito.doReturn(deployment.getTemplate()).when(toscaService)
        .updateTemplate(Mockito.anyString());
    Mockito.doReturn(deployment.getTemplate()).when(toscaService)
        .updateTemplate(Mockito.anyString());
    Mockito.when(resourceRepository.findOne(id)).thenReturn(resource);
    Mockito.doReturn(resource).when(resourceRepository).save(resource);
    InfrastructureManager im = Mockito.mock(InfrastructureManager.class);
    Mockito.doReturn(im).when(imService).getClient(Mockito.anyListOf(CloudProviderEndpoint.class), Mockito.any());
    Mockito.doThrow(new ImClientErrorException(new ResponseError("message", 404))).when(im)
        .addResource(Mockito.anyString(), Mockito.anyString(), Mockito.anyObject());

    Assert.assertFalse(imService.doUpdate(dm, "newTemplate"));

  }

}
