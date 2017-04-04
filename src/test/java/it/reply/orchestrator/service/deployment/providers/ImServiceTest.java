package it.reply.orchestrator.service.deployment.providers;

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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingException;

import es.upv.i3m.grycap.im.InfrastructureManager;
import es.upv.i3m.grycap.im.States;
import es.upv.i3m.grycap.im.exceptions.ImClientErrorException;
import es.upv.i3m.grycap.im.exceptions.ImClientException;
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
import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.CloudProviderEndpoint.IaaSType;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.exception.service.ToscaException;
import it.reply.orchestrator.service.ToscaServiceImpl;
import it.reply.orchestrator.service.security.OAuth2TokenService;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
  private ImProperties imProperties = new ImProperties("im.url");
  
  @Mock
  private OidcProperties oidcProperties;

  @Mock
  private OAuth2TokenService oauth2TokenService;
  
  @Before
  public void setup() throws ParsingException {
    MockitoAnnotations.initMocks(this);
  }

  private DeploymentMessage generateDeployDm() {
    DeploymentMessage dm = new DeploymentMessage();
    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.setStatus(Status.CREATE_IN_PROGRESS);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    dm.setDeployment(deployment);
    dm.setDeploymentId(deployment.getId());
    deployment.getResources().addAll(ControllerTestUtils.createResources(deployment, 2, false));
    deployment.getResources().stream().forEach(r -> r.setState(NodeStates.CREATING));

    CloudProviderEndpoint chosenCloudProviderEndpoint = new CloudProviderEndpoint();
    chosenCloudProviderEndpoint.setCpComputeServiceId(UUID.randomUUID().toString());
    dm.setChosenCloudProviderEndpoint(chosenCloudProviderEndpoint);
    return dm;
  }

  private DeploymentMessage generateIsDeployedDm() {
    DeploymentMessage dm = generateDeployDm();
    String infrastructureId = UUID.randomUUID().toString();
    dm.getDeployment().setEndpoint(infrastructureId);
    dm.getDeployment().setTask(Task.DEPLOYER);
    dm.getDeployment().setDeploymentProvider(DeploymentProvider.IM);
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

  @Test
  public void testDoDeploySuccesful()
      throws ToscaException, ParsingException, IOException, ImClientException {
    DeploymentMessage dm = generateDeployDm();

    String infrastructureId = UUID.randomUUID().toString();
    InfrastructureUri infrastructureUri =
        new InfrastructureUri("http://localhost:8080/infrastructures/" + infrastructureId);
    ArchiveRoot ar = new ArchiveRoot();

    Mockito.when(deploymentRepository.save(dm.getDeployment())).thenReturn(dm.getDeployment());
    Mockito.when(deploymentRepository.findOne(dm.getDeployment().getId()))
        .thenReturn(dm.getDeployment());
    Mockito.doReturn(ar).when(toscaService).prepareTemplate(dm.getDeployment().getTemplate(),
        dm.getDeployment().getParameters());
    Mockito.when(infrastructureManager.createInfrastructure(Mockito.anyString(),
        Mockito.eq(BodyContentType.TOSCA))).thenReturn(infrastructureUri);
    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.any(DeploymentMessage.class));

    boolean returnValue = imService.doDeploy(dm);

    Assert.assertEquals(dm.getDeployment().getTask(), Task.DEPLOYER);
    Assert.assertEquals(dm.getDeployment().getStatus(), Status.CREATE_IN_PROGRESS);
    Assert.assertEquals(dm.getDeployment().getDeploymentProvider(), DeploymentProvider.IM);
    Assert.assertEquals(dm.getDeployment().getEndpoint(), infrastructureId);
    Assert.assertEquals(dm.getDeployment().getResources().size(), 2);
    Assert.assertEquals(dm.getDeployment().getResources().get(0).getState(), NodeStates.CREATING);
    Assert.assertEquals(dm.getDeployment().getResources().get(1).getState(), NodeStates.CREATING);
    Assert.assertTrue(dm.isCreateComplete());
    Assert.assertTrue(returnValue);
  }

  @Test
  public void testDoDeployNoId()
      throws ToscaException, ParsingException, IOException, ImClientException {
    DeploymentMessage dm = generateDeployDm();

    InfrastructureUri infrastructureUri =
        new InfrastructureUri("http://localhost:8080/infrastructures/");
    ArchiveRoot ar = new ArchiveRoot();

    Mockito.when(deploymentRepository.save(dm.getDeployment())).thenReturn(dm.getDeployment());
    Mockito.when(deploymentRepository.findOne(dm.getDeployment().getId()))
        .thenReturn(dm.getDeployment());
    Mockito.doReturn(ar).when(toscaService).prepareTemplate(dm.getDeployment().getTemplate(),
        dm.getDeployment().getParameters());
    Mockito.when(infrastructureManager.createInfrastructure(Mockito.anyString(),
        Mockito.eq(BodyContentType.TOSCA))).thenReturn(infrastructureUri);
    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.any(DeploymentMessage.class));

    boolean returnValue = imService.doDeploy(dm);

    Assert.assertEquals(dm.getDeployment().getTask(), Task.NONE);
    Assert.assertEquals(dm.getDeployment().getStatus(), Status.CREATE_FAILED);
    Assert.assertEquals(dm.getDeployment().getResources().size(), 2);
    Assert.assertEquals(dm.getDeployment().getResources().get(0).getState(), NodeStates.ERROR);
    Assert.assertEquals(dm.getDeployment().getResources().get(1).getState(), NodeStates.ERROR);
    Assert.assertEquals(dm.getDeployment().getDeploymentProvider(), DeploymentProvider.IM);
    Assert.assertEquals(dm.getDeployment().getEndpoint(), null);
    Assert.assertFalse(dm.isCreateComplete());
    Assert.assertFalse(returnValue);
  }

  @Test
  public void testDoDeployIMexception()
      throws ToscaException, ParsingException, IOException, ImClientException {
    DeploymentMessage dm = generateDeployDm();

    ArchiveRoot ar = new ArchiveRoot();
    ImClientErrorException imException =
        new ImClientErrorException(new ResponseError("Error", 500));

    Mockito.when(deploymentRepository.save(dm.getDeployment())).thenReturn(dm.getDeployment());
    Mockito.when(resourceRepository.save(Mockito.any(Resource.class)))
        .thenAnswer(r -> r.getArguments()[0]);
    Mockito.when(deploymentRepository.findOne(dm.getDeployment().getId()))
        .thenReturn(dm.getDeployment());

    Mockito.doReturn(ar).when(toscaService).prepareTemplate(dm.getDeployment().getTemplate(),
        dm.getDeployment().getParameters());
    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.any(DeploymentMessage.class));
    Mockito.when(infrastructureManager.createInfrastructure(Mockito.anyString(),
        Mockito.eq(BodyContentType.TOSCA))).thenThrow(imException);

    boolean returnValue = imService.doDeploy(dm);

    Assert.assertEquals(dm.getDeployment().getTask(), Task.NONE);
    Assert.assertEquals(dm.getDeployment().getStatus(), Status.CREATE_FAILED);
    Assert.assertEquals(dm.getDeployment().getResources().get(0).getState(), NodeStates.ERROR);
    Assert.assertEquals(dm.getDeployment().getResources().get(1).getState(), NodeStates.ERROR);
    Assert.assertEquals(dm.getDeployment().getDeploymentProvider(), DeploymentProvider.IM);
    Assert.assertEquals(dm.getDeployment().getResources().size(), 2);
    Assert.assertEquals(dm.getDeployment().getEndpoint(), null);
    Assert.assertEquals(dm.getDeployment().getStatusReason(),
        imException.getResponseError().getFormattedErrorMessage());
    Assert.assertFalse(dm.isCreateComplete());
    Assert.assertFalse(returnValue);
  }

  @Test
  public void testDoDeployGenericException()
      throws ToscaException, ParsingException, IOException, ImClientException {
    DeploymentMessage dm = generateDeployDm();

    ToscaException exception = new ToscaException("ToscaException");

    Mockito.when(deploymentRepository.save(dm.getDeployment())).thenReturn(dm.getDeployment());
    Mockito.when(resourceRepository.save(Mockito.any(Resource.class)))
        .thenAnswer(r -> r.getArguments()[0]);
    Mockito.when(deploymentRepository.findOne(dm.getDeployment().getId()))
        .thenReturn(dm.getDeployment());

    Mockito.doThrow(exception).when(toscaService).prepareTemplate(dm.getDeployment().getTemplate(),
        dm.getDeployment().getParameters());

    boolean returnValue = imService.doDeploy(dm);

    Assert.assertEquals(dm.getDeployment().getTask(), Task.NONE);
    Assert.assertEquals(dm.getDeployment().getStatus(), Status.CREATE_FAILED);
    Assert.assertEquals(dm.getDeployment().getResources().get(0).getState(), NodeStates.ERROR);
    Assert.assertEquals(dm.getDeployment().getResources().get(1).getState(), NodeStates.ERROR);
    Assert.assertEquals(dm.getDeployment().getDeploymentProvider(), DeploymentProvider.IM);
    Assert.assertEquals(dm.getDeployment().getResources().size(), 2);
    Assert.assertEquals(dm.getDeployment().getEndpoint(), null);
    Assert.assertEquals(dm.getDeployment().getStatusReason(), exception.getMessage());
    Assert.assertFalse(dm.isCreateComplete());
    Assert.assertFalse(returnValue);
  }

  @Test
  public void testIsDeployedSuccesful() throws ImClientException {
    DeploymentMessage dm = generateIsDeployedDm();

    InfrastructureState infrastructureState = generateInfrastructureState(States.CONFIGURED, 2);

    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.any(DeploymentMessage.class));
    Mockito.when(infrastructureManager.getInfrastructureState(dm.getDeployment().getEndpoint()))
        .thenReturn(infrastructureState);

    boolean returnValue = imService.isDeployed(dm);

    Assert.assertEquals(dm.getDeployment().getTask(), Task.DEPLOYER);
    Assert.assertEquals(dm.getDeployment().getStatus(), Status.CREATE_IN_PROGRESS);
    Assert.assertEquals(dm.getDeployment().getDeploymentProvider(), DeploymentProvider.IM);
    Assert.assertEquals(dm.getDeployment().getEndpoint(), dm.getDeployment().getEndpoint());
    Assert.assertEquals(dm.getDeployment().getResources().size(), 2);
    Assert.assertEquals(dm.getDeployment().getResources().get(0).getState(), NodeStates.CREATING);
    Assert.assertEquals(dm.getDeployment().getResources().get(1).getState(), NodeStates.CREATING);
    Assert.assertTrue(dm.isPollComplete());
    Assert.assertTrue(returnValue);
  }

  // [WIP]
  // @Test
  public void testIsDeployedFail() throws ImClientException {
    DeploymentMessage dm = generateIsDeployedDm();

    InfrastructureState infrastructureState = generateInfrastructureState(States.CONFIGURED, 2);

    // Mockito.doReturn(infrastructureManager).when(imService).getClient(Mockito.any(DeploymentMessage.class));
    Mockito.when(imService.getClient(Mockito.any(DeploymentMessage.class)))
        .thenThrow(new ImClientException());
    // Mockito.when(infrastructureManager.getInfrastructureState(dm.getDeployment().getEndpoint()))
    // .thenReturn(infrastructureState);

    boolean returnValue = imService.isDeployed(dm);

  }

  @Test
  public void testIsNotYetDeployed() throws ImClientException {
    DeploymentMessage dm = generateIsDeployedDm();

    InfrastructureState infrastructureState = generateInfrastructureState(States.RUNNING, 2);

    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.any(DeploymentMessage.class));
    Mockito.when(infrastructureManager.getInfrastructureState(dm.getDeployment().getEndpoint()))
        .thenReturn(infrastructureState);

    boolean returnValue = imService.isDeployed(dm);

    Assert.assertEquals(dm.getDeployment().getTask(), Task.DEPLOYER);
    Assert.assertEquals(dm.getDeployment().getStatus(), Status.CREATE_IN_PROGRESS);
    Assert.assertEquals(dm.getDeployment().getDeploymentProvider(), DeploymentProvider.IM);
    Assert.assertEquals(dm.getDeployment().getEndpoint(), dm.getDeployment().getEndpoint());
    Assert.assertEquals(dm.getDeployment().getResources().size(), 2);
    Assert.assertEquals(dm.getDeployment().getResources().get(0).getState(), NodeStates.CREATING);
    Assert.assertEquals(dm.getDeployment().getResources().get(1).getState(), NodeStates.CREATING);
    Assert.assertFalse(dm.isPollComplete());
    Assert.assertFalse(returnValue);
  }

  @Test
  public void testIsDeployedFailedInfrastructureStatus() throws ImClientException {
    DeploymentMessage dm = generateIsDeployedDm();

    InfrastructureState infrastructureState = generateInfrastructureState(States.FAILED, 2);

    Mockito.when(deploymentRepository.findOne(dm.getDeployment().getId()))
        .thenReturn(dm.getDeployment());
    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.any(DeploymentMessage.class));

    Mockito.when(infrastructureManager.getInfrastructureState(dm.getDeployment().getEndpoint()))
        .thenReturn(infrastructureState);

    try {
      imService.isDeployed(dm);
      Assert.fail();
    } catch (DeploymentException ex) {
      Assert
          .assertEquals(
              String.format(
                  "Fail to deploy deployment <%s>" + "\nIM id is: <%s>" + "\nIM response is: <%s>",
                  dm.getDeployment().getId(), dm.getDeployment().getEndpoint(),
                  infrastructureState.getFormattedInfrastructureStateString()),
              ex.getMessage());
    }

    Assert.assertEquals(dm.getDeployment().getTask(), Task.NONE);
    Assert.assertEquals(dm.getDeployment().getStatus(), Status.CREATE_FAILED);
    Assert.assertEquals(dm.getDeployment().getDeploymentProvider(), DeploymentProvider.IM);
    Assert.assertEquals(dm.getDeployment().getEndpoint(), dm.getDeployment().getEndpoint());
    Assert.assertEquals(dm.getDeployment().getResources().size(), 2);
    Assert.assertEquals(dm.getDeployment().getResources().get(0).getState(), NodeStates.ERROR);
    Assert.assertEquals(dm.getDeployment().getResources().get(1).getState(), NodeStates.ERROR);
    Assert.assertFalse(dm.isPollComplete());
  }

  @Test
  public void testIsDeployedUnconfiguredInfrastructureStatus() throws ImClientException {
    DeploymentMessage dm = generateIsDeployedDm();

    InfrastructureState infrastructureState = generateInfrastructureState(States.UNCONFIGURED, 2);

    Mockito.when(deploymentRepository.findOne(dm.getDeployment().getId()))
        .thenReturn(dm.getDeployment());
    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.any(DeploymentMessage.class));

    Mockito.when(infrastructureManager.getInfrastructureState(dm.getDeployment().getEndpoint()))
        .thenReturn(infrastructureState);
    try {
      imService.isDeployed(dm);
      Assert.fail();
    } catch (DeploymentException ex) {
      Assert
          .assertEquals(
              String.format(
                  "Fail to deploy deployment <%s>" + "\nIM id is: <%s>" + "\nIM response is: <%s>",
                  dm.getDeployment().getId(), dm.getDeployment().getEndpoint(),
                  infrastructureState.getFormattedInfrastructureStateString()),
              ex.getMessage());
    }

    Assert.assertEquals(dm.getDeployment().getTask(), Task.NONE);
    Assert.assertEquals(dm.getDeployment().getStatus(), Status.CREATE_FAILED);
    Assert.assertEquals(dm.getDeployment().getDeploymentProvider(), DeploymentProvider.IM);
    Assert.assertEquals(dm.getDeployment().getEndpoint(), dm.getDeployment().getEndpoint());
    Assert.assertEquals(dm.getDeployment().getResources().size(), 2);
    Assert.assertEquals(dm.getDeployment().getResources().get(0).getState(), NodeStates.ERROR);
    Assert.assertEquals(dm.getDeployment().getResources().get(1).getState(), NodeStates.ERROR);
    Assert.assertFalse(dm.isPollComplete());
  }

  @Test
  public void testFinalizeDeploy() throws ImClientException {
    DeploymentMessage dm = generateIsDeployedDm();

    Map<String, Object> outputs = Maps.newHashMap();
    outputs.put("firstKey", 1);
    outputs.put("SecondKey", null);

    InfOutputValues outputValues = new InfOutputValues(outputs);

    InfrastructureUris vmUrls = new InfrastructureUris(Lists.newArrayList());
    vmUrls.getUris().add(new InfrastructureUri(
        "http://localhost/infrastructures/" + dm.getDeployment().getEndpoint() + "/" + 0));
    vmUrls.getUris().add(new InfrastructureUri(
        "http://localhost/infrastructures/" + dm.getDeployment().getEndpoint() + "/" + 1));

    VirtualMachineInfo vmInfo0 = new VirtualMachineInfo(Lists.newArrayList());
    vmInfo0.getVmProperties().add(Maps.newHashMap());
    vmInfo0.getVmProperties().get(0).put("id",
        dm.getDeployment().getResources().get(0).getToscaNodeName());
    VirtualMachineInfo vmInfo1 = new VirtualMachineInfo(Lists.newArrayList());
    vmInfo0.getVmProperties().get(0).put("id",
        dm.getDeployment().getResources().get(1).getToscaNodeName());

    Mockito.when(deploymentRepository.findOne(dm.getDeployment().getId()))
        .thenReturn(dm.getDeployment());
    Mockito.when(deploymentRepository.save(dm.getDeployment())).thenReturn(dm.getDeployment());
    Mockito.when(resourceRepository.findByDeployment_id(dm.getDeployment().getId(), null))
        .thenReturn(new PageImpl<Resource>(dm.getDeployment().getResources()));
    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.any(DeploymentMessage.class));

    Mockito.when(infrastructureManager.getInfrastructureOutputs(dm.getDeployment().getEndpoint()))
        .thenReturn(outputValues);
    Mockito.when(infrastructureManager.getInfrastructureInfo(dm.getDeployment().getEndpoint()))
        .thenReturn(vmUrls);
    Mockito
        .when(infrastructureManager.getVmInfo(dm.getDeployment().getEndpoint(), String.valueOf(0)))
        .thenReturn(vmInfo0);
    Mockito
        .when(infrastructureManager.getVmInfo(dm.getDeployment().getEndpoint(), String.valueOf(1)))
        .thenReturn(vmInfo1);

    imService.finalizeDeploy(dm, true);

    Assert.assertEquals(dm.getDeployment().getTask(), Task.NONE);
    Assert.assertEquals(dm.getDeployment().getStatus(), Status.CREATE_COMPLETE);
    Assert.assertEquals(dm.getDeployment().getDeploymentProvider(), DeploymentProvider.IM);
    Assert.assertEquals(dm.getDeployment().getEndpoint(), dm.getDeployment().getEndpoint());
    Assert.assertEquals(dm.getDeployment().getResources().size(), 2);
    Assert.assertEquals(dm.getDeployment().getResources().get(0).getState(), NodeStates.STARTED);
    Assert.assertEquals(dm.getDeployment().getResources().get(1).getState(), NodeStates.STARTED);
    Assert.assertFalse(dm.isPollComplete());
  }

  @Test
  public void testUpdateOnError() {
    DeploymentMessage dm = generateDeployDm();
    String id = UUID.randomUUID().toString();

    Deployment deployment = dm.getDeployment();
    Mockito.when(deploymentRepository.findOne(id)).thenReturn(dm.getDeployment());

    deployment.setStatus(Status.DELETE_IN_PROGRESS);
    imService.updateOnError(id);
    Assert.assertEquals(Status.DELETE_FAILED, deployment.getStatus());

    deployment.setStatus(Status.UPDATE_IN_PROGRESS);
    imService.updateOnError(id);
    Assert.assertEquals(Status.UPDATE_FAILED, deployment.getStatus());

    deployment.setStatus(Status.UNKNOWN);
    imService.updateOnError(id);
    Assert.assertEquals(Status.UNKNOWN, deployment.getStatus());
  }

  @Test
  public void testGetClient() throws Exception {
    DeploymentMessage dm = generateDeployDm();
    CloudProviderEndpoint cloudProviderEndpoint = new CloudProviderEndpoint();
    cloudProviderEndpoint.setIaasType(IaaSType.OPENSTACK);
    cloudProviderEndpoint.setCpEndpoint("recas.ba.infn");
    cloudProviderEndpoint.setCpComputeServiceId(UUID.randomUUID().toString());
    dm.setChosenCloudProviderEndpoint(cloudProviderEndpoint);

    Mockito.when(oidcProperties.isEnabled()).thenReturn(true);
    OidcTokenId id = new OidcTokenId();
    dm.setRequestedWithToken(id);
    Mockito.when(oauth2TokenService.getAccessToken(id, OAuth2TokenService.REQUIRED_SCOPES))
    .thenReturn("J1qK1c18UUGJFAzz9xnH56584l4");

    // TO-DO: Assert equals both result and client

    cloudProviderEndpoint.setCpEndpoint("https://www.openstack.org/");
    dm.setChosenCloudProviderEndpoint(cloudProviderEndpoint);
    imService.getClient(dm);
    // TO-DO: Assert equals both result and client


    // OPENENBULA
    cloudProviderEndpoint.setIaasType(IaaSType.OPENNEBULA);
    dm.setChosenCloudProviderEndpoint(cloudProviderEndpoint);
    imService.getClient(dm);
    // TO-DO: Assert equals both result and client

    // AWS
    cloudProviderEndpoint.setIaasType(IaaSType.AWS);
    cloudProviderEndpoint.setUsername("username");
    cloudProviderEndpoint.setPassword("password");
    dm.setChosenCloudProviderEndpoint(cloudProviderEndpoint);
    imService.getClient(dm);
    // TO-DO: Assert equals both result and client
  }

  @Test(expected = DeploymentException.class)
  public void testGetClientFailDeployment() throws Exception {
    DeploymentMessage dm = generateDeployDm();
    CloudProviderEndpoint cloudProviderEndpoint = new CloudProviderEndpoint();
    cloudProviderEndpoint.setIaasType(IaaSType.OPENSTACK);
    cloudProviderEndpoint.setCpComputeServiceId(UUID.randomUUID().toString());
    cloudProviderEndpoint.setCpEndpoint("lorem.ipsum");
    dm.setChosenCloudProviderEndpoint(cloudProviderEndpoint);


    Mockito.when(oidcProperties.isEnabled()).thenReturn(true);
    OidcTokenId id = new OidcTokenId();
    dm.setRequestedWithToken(id);
    Mockito.when(oauth2TokenService.getAccessToken(id, OAuth2TokenService.REQUIRED_SCOPES))
    .thenReturn("J1qK1c18UUGJFAzz9xnH56584l4");
    
    imService.getClient(dm);
  }

  @Test
  public void testDoUndeploySuccess() throws ImClientException {
    DeploymentMessage dm = generateDeployDm();
    Mockito.when(deploymentRepository.save(dm.getDeployment())).thenReturn(dm.getDeployment());
    Assert.assertTrue(imService.doUndeploy(dm));

    dm.getDeployment().setEndpoint("endpoint");
    Mockito.when(deploymentRepository.save(dm.getDeployment())).thenReturn(dm.getDeployment());
    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.any(DeploymentMessage.class));
    Assert.assertTrue(imService.doUndeploy(dm));

    Mockito.doThrow(new ImClientErrorException(new ResponseError(null, 404)))
        .when(infrastructureManager).destroyInfrastructure(Mockito.any(String.class));
    Assert.assertTrue(imService.doUndeploy(dm));


  }

  @Test
  public void testDoUndeployFail() throws Exception {
    DeploymentMessage dm = generateDeployDm();
    dm.getDeployment().setEndpoint("endpoint");
    Mockito.when(deploymentRepository.save(dm.getDeployment())).thenReturn(dm.getDeployment());
    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.any(DeploymentMessage.class));
    ResponseError responseError = new ResponseError(null, 405);
    Mockito.doThrow(new ImClientErrorException(responseError)).when(infrastructureManager)
        .destroyInfrastructure(Mockito.any(String.class));
    Mockito.doNothing().when(imService).updateOnError(Mockito.anyString(), Mockito.anyString());
    Assert.assertFalse(imService.doUndeploy(dm));

    Mockito.doThrow(new NullPointerException()).when(infrastructureManager)
        .destroyInfrastructure(Mockito.any(String.class));
    Assert.assertFalse(imService.doUndeploy(dm));

  }

  @Test
  public void testIsUndeployedSuccess() throws Exception {
    DeploymentMessage dm = generateDeployDm();

    String infrastructureId = UUID.randomUUID().toString();
    InfrastructureUri infrastructureUri =
        new InfrastructureUri("http://localhost:8080/infrastructures/" + infrastructureId);
    ArchiveRoot ar = new ArchiveRoot();

    Mockito.when(deploymentRepository.save(dm.getDeployment())).thenReturn(dm.getDeployment());
    Mockito.when(deploymentRepository.findOne(dm.getDeployment().getId()))
        .thenReturn(dm.getDeployment());
    Mockito.doReturn(ar).when(toscaService).prepareTemplate(dm.getDeployment().getTemplate(),
        dm.getDeployment().getParameters());
    Mockito.when(infrastructureManager.createInfrastructure(Mockito.anyString(),
        Mockito.eq(BodyContentType.TOSCA))).thenReturn(infrastructureUri);
    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.any(DeploymentMessage.class));

    Assert.assertTrue(imService.isUndeployed(dm));


    Mockito.doThrow(new ImClientErrorException(new ResponseError(null, 404)))
        .when(infrastructureManager).getInfrastructureState(Mockito.any(String.class));
    Assert.assertTrue(imService.isUndeployed(dm));

  }

  @Test
  public void testIsUndeployedFail() throws Exception {
    DeploymentMessage dm = generateDeployDm();
    dm.getDeployment().setEndpoint("www.endpoint.com");
    String infrastructureId = UUID.randomUUID().toString();
    InfrastructureUri infrastructureUri =
        new InfrastructureUri("http://localhost:8080/infrastructures/" + infrastructureId);
    ArchiveRoot ar = new ArchiveRoot();

    Mockito.when(deploymentRepository.save(dm.getDeployment())).thenReturn(dm.getDeployment());
    Mockito.when(deploymentRepository.findOne(dm.getDeployment().getId()))
        .thenReturn(dm.getDeployment());
    Mockito.doReturn(ar).when(toscaService).prepareTemplate(dm.getDeployment().getTemplate(),
        dm.getDeployment().getParameters());
    Mockito.when(infrastructureManager.createInfrastructure(Mockito.anyString(),
        Mockito.eq(BodyContentType.TOSCA))).thenReturn(infrastructureUri);
    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.any(DeploymentMessage.class));

    Mockito.doThrow(new ImClientErrorException(new ResponseError(null, 500)))
        .when(infrastructureManager).getInfrastructureState(Mockito.any(String.class));
    Assert.assertFalse(imService.isUndeployed(dm));

    dm.getDeployment().setEndpoint("www.endpoint.com");
    Assert.assertFalse(imService.isUndeployed(dm));
  }

  @Test
  public void testFinalizeUndeploy() {
    DeploymentMessage dm = generateDeployDm();
    Mockito.doNothing().when(imService).updateOnSuccess(Mockito.anyString());
    imService.finalizeUndeploy(dm, true);
    Mockito.doNothing().when(imService).updateOnError(Mockito.anyString());
    imService.finalizeUndeploy(dm, false);
  }


  @Test
  public void testDoUpdateSuccesful()
      throws ToscaException, ParsingException, IOException, ImClientException {
    DeploymentMessage dm = generateDeployDm();

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


    Mockito.when(deploymentRepository.save(dm.getDeployment())).thenReturn(dm.getDeployment());
    Mockito.when(deploymentRepository.findOne(dm.getDeployment().getId()))
        .thenReturn(dm.getDeployment());
    Mockito.doReturn(oldAr).when(toscaService).prepareTemplate(dm.getDeployment().getTemplate(),
        dm.getDeployment().getParameters());
    Mockito.doReturn(newAr).when(toscaService).prepareTemplate("newTemplate",
        dm.getDeployment().getParameters());
    Mockito.when(infrastructureManager.createInfrastructure(Mockito.anyString(),
        Mockito.eq(BodyContentType.TOSCA))).thenReturn(infrastructureUri);
    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.any(DeploymentMessage.class));


    Mockito.doReturn(oldNodes.values()).when(toscaService).getScalableNodes(oldAr);
    Mockito.doReturn(newNodes.values()).when(toscaService).getScalableNodes(newAr);
    Mockito.doReturn(Optional.of(1)).doReturn(Optional.of(4))
        .when(toscaService).getCount(Mockito.any(NodeTemplate.class));
    Mockito.doReturn(dm.getDeployment().getTemplate()).when(toscaService)
        .updateTemplate(Mockito.anyString());

    boolean returnValue = imService.doUpdate(dm, "newTemplate");

    Assert.assertTrue(returnValue);

  }


}
