package it.reply.orchestrator.service;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;

import com.google.common.collect.Maps;

import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.tosca.model.ArchiveRoot;

import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.controller.ControllerTestUtils;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.entity.WorkflowReference;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.request.DeploymentRequest;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.exception.http.ConflictException;
import it.reply.orchestrator.exception.http.NotFoundException;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.workflowmanager.orchestrator.bpm.BusinessProcessManager;
import it.reply.workflowmanager.orchestrator.bpm.BusinessProcessManager.RUNTIME_STRATEGY;

import org.jbpm.ruleflow.instance.RuleFlowProcessInstance;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public class DeploymentServiceTest {

  @InjectMocks
  private DeploymentService deploymentService = new DeploymentServiceImpl();

  @Mock
  private DeploymentRepository deploymentRepository;

  @Mock
  private ResourceRepository resourceRepository;

  @Mock
  private ToscaServiceImpl toscaService = new ToscaServiceImpl();

  @Mock
  private BusinessProcessManager wfService;

  @Mock
  private OAuth2TokenService oauth2TokenService;

  @Mock
  private OidcProperties oidcProperties;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void getDeploymentsSuccessful() throws Exception {
    List<Deployment> deployments = ControllerTestUtils.createDeployments(5, false);

    Mockito.when(deploymentRepository.findAll((Pageable) null))
        .thenReturn(new PageImpl<Deployment>(deployments));

    Page<Deployment> pagedDeployment = deploymentService.getDeployments(null);

    Assert.assertEquals(pagedDeployment.getContent(), deployments);

  }

  @Test
  public void getDeploymentsPagedSuccessful() throws Exception {
    Pageable pageable = new PageRequest(0, 10);
    List<Deployment> deployments = ControllerTestUtils.createDeployments(10, false);

    Mockito.when(deploymentRepository.findAll(pageable))
        .thenReturn(new PageImpl<Deployment>(deployments));

    Page<Deployment> pagedDeployment = deploymentService.getDeployments(pageable);

    Assert.assertEquals(pagedDeployment.getContent(), deployments);
    Assert.assertTrue(pagedDeployment.getNumberOfElements() == 10);
  }

  @Test
  public void getDeploymentSuccessful() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment();

    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);

    Deployment returneDeployment = deploymentService.getDeployment(deployment.getId());

    Assert.assertEquals(returneDeployment, deployment);
  }

  @Test(expected = NotFoundException.class)
  public void getDeploymentError() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment();

    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(null);

    deploymentService.getDeployment(deployment.getId());
  }

  private Deployment basecreateDeploymentSuccessful(DeploymentRequest deploymentRequest,
      Map<String, NodeTemplate> nodeTemplates) throws Exception {

    if (nodeTemplates == null) {
      nodeTemplates = Maps.newHashMap();
    }

    ArchiveRoot parsingResult = new ArchiveRoot();
    parsingResult.setTopology(new Topology());
    parsingResult.getTopology().setNodeTemplates(nodeTemplates);

    Mockito.when(toscaService.prepareTemplate(deploymentRequest.getTemplate(),
        deploymentRequest.getParameters())).thenReturn(parsingResult);

    Mockito.when(deploymentRepository.save(Mockito.any(Deployment.class)))
        .thenAnswer(y -> y.getArguments()[0]);

    Mockito.when(toscaService.getNodeCapabilityByName(Mockito.any(), Mockito.any()))
        .thenCallRealMethod();

    Mockito.when(resourceRepository.save(Mockito.any(Resource.class))).thenAnswer(y -> {
      Resource res = (Resource) y.getArguments()[0];
      res.getDeployment().getResources().add(res);
      return res;
    });

    Mockito.when(oidcProperties.isEnabled()).thenReturn(true);
    Mockito.when(oauth2TokenService.getOAuth2Token()).thenReturn("token");

    Mockito.when(wfService.startProcess(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(new RuleFlowProcessInstance());

    return deploymentService.createDeployment(deploymentRequest);
  }

  @Test
  public void createComputeDeploymentSuccessful() throws Exception {

    String nodeName1 = "server1";
    String nodeName2 = "server2";
    String nodeType = "tosca.nodes.indigo.Compute";

    DeploymentRequest deploymentRequest = new DeploymentRequest();

    Map<String, Capability> capabilities = Maps.newHashMap();

    NodeTemplate nt = new NodeTemplate();
    nt.setCapabilities(capabilities);
    nt.setType(nodeType);

    Map<String, NodeTemplate> nts = Maps.newHashMap();
    nts.put(nodeName1, nt);

    nt = new NodeTemplate();
    nt.setCapabilities(capabilities);
    nt.setType(nodeType);
    nts.put(nodeName2, nt);

    Deployment returneDeployment = basecreateDeploymentSuccessful(deploymentRequest, nts);

    Assert.assertEquals(returneDeployment.getResources().size(), 2);

    Assert.assertThat(returneDeployment.getResources().get(0).getToscaNodeName(),
        anyOf(is(nodeName1), is(nodeName2)));
    Assert.assertEquals(returneDeployment.getResources().get(0).getToscaNodeType(), nodeType);
    Assert.assertEquals(returneDeployment.getResources().get(0).getState(), NodeStates.CREATING);
    Mockito.verify(resourceRepository).save(returneDeployment.getResources().get(0));

    Assert.assertThat(returneDeployment.getResources().get(1).getToscaNodeName(),
        anyOf(is(nodeName1), is(nodeName2)));
    Assert.assertEquals(returneDeployment.getResources().get(1).getToscaNodeType(), nodeType);
    Assert.assertEquals(returneDeployment.getResources().get(1).getState(), NodeStates.CREATING);
    Mockito.verify(resourceRepository).save(returneDeployment.getResources().get(1));

    Mockito.verify(deploymentRepository, Mockito.atLeast(1)).save(returneDeployment);
    Mockito.verify(resourceRepository).save(returneDeployment.getResources().get(0));
  }

  @Test
  public void createComputeScalableDeploymentSuccessful() throws Exception {
    DeploymentRequest deploymentRequest = new DeploymentRequest();

    Capability capability = new Capability();
    capability.setProperties(Maps.newHashMap());

    Map<String, Capability> capabilities = Maps.newHashMap();
    capabilities.put("scalable", capability);

    NodeTemplate nt = new NodeTemplate();
    nt.setCapabilities(capabilities);
    nt.setType("tosca.nodes.indigo.Compute");

    Map<String, NodeTemplate> nts = Maps.newHashMap();
    nts.put("server", nt);

    Deployment returneDeployment = basecreateDeploymentSuccessful(deploymentRequest, nts);

    Assert.assertEquals(returneDeployment.getResources().size(), 1);
  }

  @Test
  public void createComputeScalableWithCountDeploymentSuccessful() throws Exception {
    DeploymentRequest deploymentRequest = new DeploymentRequest();

    String nodeName = "server";
    String nodeType = "tosca.nodes.indigo.Compute";

    Capability capability = new Capability();
    capability.setProperties(Maps.newHashMap());
    capability.getProperties().put("count", new ScalarPropertyValue("2"));

    Map<String, Capability> capabilities = Maps.newHashMap();
    capabilities.put("scalable", capability);

    NodeTemplate nt = new NodeTemplate();
    nt.setCapabilities(capabilities);
    nt.setType(nodeType);

    Map<String, NodeTemplate> nts = Maps.newHashMap();
    nts.put(nodeName, nt);

    Deployment returneDeployment = basecreateDeploymentSuccessful(deploymentRequest, nts);

    Assert.assertEquals(returneDeployment.getResources().size(), 2);
    Assert.assertEquals(returneDeployment.getResources().get(0).getToscaNodeName(), nodeName);
    Assert.assertEquals(returneDeployment.getResources().get(0).getToscaNodeType(), nodeType);
    Assert.assertEquals(returneDeployment.getResources().get(1).getToscaNodeName(), nodeName);
    Assert.assertEquals(returneDeployment.getResources().get(1).getToscaNodeType(), nodeType);
    Mockito.verify(resourceRepository).save(returneDeployment.getResources().get(0));
    Mockito.verify(resourceRepository).save(returneDeployment.getResources().get(1));

  }

  @Test
  public void createDeploymentWithCallbackSuccessful() throws Exception {
    DeploymentRequest deploymentRequest = new DeploymentRequest();
    String callback = "http://localhost:8080";
    deploymentRequest.setCallback(callback);

    Deployment returneDeployment = basecreateDeploymentSuccessful(deploymentRequest, null);

    Assert.assertEquals(returneDeployment.getCallback(), callback);
  }

  @Test
  public void createChronosDeploymentSuccessful() throws Exception {
    DeploymentRequest deploymentRequest = new DeploymentRequest();

    String nodeName1 = "job1";
    String nodeName2 = "job2";
    String nodeType = "tosca.nodes.indigo.Container.Application.Docker.Chronos";

    Map<String, NodeTemplate> nts = Maps.newHashMap();
    NodeTemplate nt = new NodeTemplate();
    nt.setType(nodeType);
    nts.put(nodeName1, nt);

    nt = new NodeTemplate();
    nt.setType(nodeType);
    nts.put(nodeName2, nt);

    Deployment returneDeployment = basecreateDeploymentSuccessful(deploymentRequest, nts);

    Assert.assertEquals(returneDeployment.getResources().size(), 2);
    Assert.assertThat(returneDeployment.getResources().get(0).getToscaNodeName(),
        anyOf(is(nodeName1), is(nodeName2)));
    Assert.assertEquals(returneDeployment.getResources().get(0).getToscaNodeType(), nodeType);
    Assert.assertThat(returneDeployment.getResources().get(1).getToscaNodeName(),
        anyOf(is(nodeName1), is(nodeName2)));
    Assert.assertEquals(returneDeployment.getResources().get(1).getToscaNodeType(), nodeType);
    Mockito.verify(resourceRepository).save(returneDeployment.getResources().get(0));
    Mockito.verify(resourceRepository).save(returneDeployment.getResources().get(1));
  }

  @Test(expected = NotFoundException.class)
  public void deleteDeploymentNotFoud() throws Exception {
    Mockito.when(deploymentRepository.findOne("id")).thenReturn(null);

    deploymentService.deleteDeployment("id");
  }

  @Test(expected = ConflictException.class)
  public void deleteDeploymentDeleteInProgress() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.setStatus(Status.DELETE_IN_PROGRESS);

    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);

    deploymentService.deleteDeployment(deployment.getId());
  }

  @Test(expected = ConflictException.class)
  public void deleteDeploymentDeleteComplete() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.setStatus(Status.DELETE_COMPLETE);

    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);

    deploymentService.deleteDeployment(deployment.getId());
  }

  @Test
  public void deleteDeploymentNoProviderSuccesful() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.setStatus(Status.CREATE_COMPLETE);
    deployment.setDeploymentProvider(null);
    deployment.setEndpoint(null);

    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    Mockito.when(deploymentRepository.save(deployment)).thenReturn(deployment);

    deploymentService.deleteDeployment(deployment.getId());

    Mockito.verifyZeroInteractions(wfService);
    Mockito.verify(deploymentRepository).delete(deployment);
  }

  @Test
  public void deleteDeploymentNoProviderError() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.setStatus(Status.CREATE_COMPLETE);
    deployment.setDeploymentProvider(null);
    deployment.setEndpoint("http://endpoint.com/uuid");

    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    Mockito.when(deploymentRepository.save(deployment)).thenReturn(deployment);

    try {
      deploymentService.deleteDeployment(deployment.getId());
    } catch (DeploymentException e) {
      Assert.assertEquals("Error deleting deploy <" + deployment.getId()
          + ">: Deployment provider is null but the endpoint is <" + deployment.getEndpoint() + ">",
          e.getMessage());
    }

    Mockito.verifyZeroInteractions(wfService);
    Mockito.verify(deploymentRepository, Mockito.never()).delete(deployment);
  }

  @Test
  public void deleteDeploymentSuccesfulWithReferences() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.setStatus(Status.CREATE_IN_PROGRESS);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    WorkflowReference wr1 = new WorkflowReference(0, RUNTIME_STRATEGY.PER_PROCESS_INSTANCE);
    deployment.getWorkflowReferences().add(wr1);
    WorkflowReference wr2 = new WorkflowReference(1, RUNTIME_STRATEGY.PER_PROCESS_INSTANCE);
    deployment.getWorkflowReferences().add(wr2);

    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    Mockito.when(deploymentRepository.save(deployment)).thenReturn(deployment);
    Mockito.doNothing().when(wfService).abortProcess(Mockito.anyLong(),
        Mockito.any(RUNTIME_STRATEGY.class));

    Mockito.when(wfService.startProcess(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(new RuleFlowProcessInstance());

    deploymentService.deleteDeployment(deployment.getId());

    Mockito.verify(deploymentRepository, Mockito.never()).delete(deployment);
    Mockito.verify(wfService).abortProcess(wr1.getId(), wr1.getRuntimeStrategy());
    Mockito.verify(wfService).abortProcess(wr2.getId(), wr2.getRuntimeStrategy());
    Mockito.verify(deploymentRepository, Mockito.atLeast(1)).save(deployment);
  }

  @Test
  public void deleteDeploymentSuccesfulNoReferences() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.setStatus(Status.CREATE_IN_PROGRESS);
    deployment.setDeploymentProvider(DeploymentProvider.IM);

    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    Mockito.when(deploymentRepository.save(deployment)).thenReturn(deployment);

    Mockito.when(wfService.startProcess(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(new RuleFlowProcessInstance());

    deploymentService.deleteDeployment(deployment.getId());

    Mockito.verify(deploymentRepository, Mockito.never()).delete(deployment);
    Mockito.verify(wfService, Mockito.never()).abortProcess(Mockito.anyLong(),
        Mockito.any(RUNTIME_STRATEGY.class));
    Mockito.verify(deploymentRepository, Mockito.atLeast(1)).save(deployment);
  }
}
