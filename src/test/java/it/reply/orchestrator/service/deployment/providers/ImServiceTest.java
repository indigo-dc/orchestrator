package it.reply.orchestrator.service.deployment.providers;

import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingException;

import es.upv.i3m.grycap.im.InfrastructureManager;
import es.upv.i3m.grycap.im.exceptions.ImClientException;
import es.upv.i3m.grycap.im.pojo.InfrastructureUri;
import es.upv.i3m.grycap.im.rest.client.BodyContentType;

import it.reply.orchestrator.controller.ControllerTestUtils;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.exception.service.ToscaException;
import it.reply.orchestrator.service.ToscaServiceImpl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.io.IOException;
import java.util.UUID;

public class ImServiceTest {

  @Spy
  @InjectMocks
  private ImServiceImpl imService;

  @Spy
  private ToscaServiceImpl toscaService;

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

  @Test
  public void testDeploySuccesful()
      throws ToscaException, ParsingException, IOException, ImClientException {
    DeploymentMessage dm = new DeploymentMessage();
    Deployment deployment = ControllerTestUtils.createDeployment();
    dm.setDeployment(deployment);
    dm.setDeploymentId(deployment.getId());

    String infrastructureId = UUID.randomUUID().toString();
    InfrastructureUri infrastructureUri =
        new InfrastructureUri("http://localhost:8080/infrastructures/" + infrastructureId);
    ArchiveRoot ar = new ArchiveRoot();

    Mockito.when(deploymentRepository.save(deployment)).thenReturn(deployment);
    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);

    Mockito.doReturn(ar).when(toscaService).prepareTemplate(deployment.getTemplate(),
        deployment.getParameters());
    Mockito.when(infrastructureManager.createInfrastructure(Mockito.anyString(),
        Mockito.eq(BodyContentType.TOSCA))).thenReturn(infrastructureUri);
    Mockito.doReturn(infrastructureManager).when(imService)
        .getClient(Mockito.any(DeploymentMessage.class));

    boolean returnValue = imService.doDeploy(dm);

    Assert.assertEquals(deployment.getTask(), Task.DEPLOYER);
    Assert.assertEquals(deployment.getDeploymentProvider(), DeploymentProvider.IM);
    Assert.assertEquals(deployment.getEndpoint(), infrastructureId);
    Assert.assertTrue(dm.isCreateComplete());
    Assert.assertTrue(returnValue);

  }

}
