package it.reply.orchestrator.service.deployment.providers;

import com.google.common.collect.Lists;

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

import it.reply.orchestrator.controller.ControllerTestUtils;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.exception.service.ToscaException;
import it.reply.orchestrator.service.ToscaServiceImpl;

import org.elasticsearch.common.collect.Maps;
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
import java.util.Map;
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

  @Before
  public void setup() throws ParsingException {
    MockitoAnnotations.initMocks(this);
  }

  private DeploymentMessage generateDeployDm() {
    DeploymentMessage dm = new DeploymentMessage();
    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.setStatus(Status.CREATE_IN_PROGRESS);
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
                  "Fail to deploy deployment <%s>." + "\nIM id is: <%s>" + "\nIM response is: <%s>",
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
                  "Fail to deploy deployment <%s>." + "\nIM id is: <%s>" + "\nIM response is: <%s>",
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
}
