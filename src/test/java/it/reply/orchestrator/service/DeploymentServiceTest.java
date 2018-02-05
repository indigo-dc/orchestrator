/*
 * Copyright © 2015-2018 Santer Reply S.p.A.
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

import static org.assertj.core.api.Assertions.*;

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
import it.reply.orchestrator.exception.http.BadRequestException;
import it.reply.orchestrator.exception.http.ConflictException;
import it.reply.orchestrator.exception.http.NotFoundException;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.workflowmanager.orchestrator.bpm.BusinessProcessManager;
import it.reply.workflowmanager.orchestrator.bpm.BusinessProcessManager.RUNTIME_STRATEGY;

import org.jbpm.ruleflow.instance.RuleFlowProcessInstance;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class DeploymentServiceTest {

  @InjectMocks
  private DeploymentService deploymentService = new DeploymentServiceImpl();

  @Mock
  private DeploymentRepository deploymentRepository;

  @Mock
  private ResourceRepository resourceRepository;

  @Spy
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
    List<Deployment> deployments = ControllerTestUtils.createDeployments(5);

    Mockito
        .when(deploymentRepository.findAll((Pageable) null))
        .thenReturn(new PageImpl<Deployment>(deployments));

    Page<Deployment> pagedDeployment = deploymentService.getDeployments(null, null);

    assertThat(pagedDeployment.getContent()).isEqualTo(deployments);

  }

  @Test
  public void getDeploymentsPagedSuccessful() throws Exception {
    Pageable pageable = new PageRequest(0, 10);
    List<Deployment> deployments = ControllerTestUtils.createDeployments(10);

    Mockito
        .when(deploymentRepository.findAll(pageable))
        .thenReturn(new PageImpl<Deployment>(deployments));

    Page<Deployment> pagedDeployment = deploymentService.getDeployments(pageable, null);

    assertThat(pagedDeployment.getContent()).isEqualTo(deployments);
    assertThat(pagedDeployment.getNumberOfElements()).isEqualTo(10);
  }

  @Test
  public void getDeploymentSuccessful() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment();

    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);

    Deployment returneDeployment = deploymentService.getDeployment(deployment.getId());

    assertThat(returneDeployment).isEqualTo(deployment);
  }

  @Test
  public void getDeploymentError() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment();

    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(null);

    assertThatThrownBy(() -> deploymentService.getDeployment(deployment.getId()))
        .isInstanceOf(NotFoundException.class);
  }

  private Deployment basecreateDeploymentSuccessful(DeploymentRequest deploymentRequest,
      Map<String, NodeTemplate> nodeTemplates) throws Exception {

    if (nodeTemplates == null) {
      nodeTemplates = Maps.newHashMap();
    }

    ArchiveRoot parsingResult = new ArchiveRoot();
    parsingResult.setTopology(new Topology());
    parsingResult.getTopology().setNodeTemplates(nodeTemplates);

    Mockito.doReturn(parsingResult).when(toscaService).prepareTemplate(
        deploymentRequest.getTemplate(),
        deploymentRequest.getParameters());

    Mockito
        .when(deploymentRepository.save(Mockito.any(Deployment.class)))
        .thenAnswer(y -> y.getArguments()[0]);

    Mockito.when(resourceRepository.save(Mockito.any(Resource.class))).thenAnswer(y -> {
      Resource res = (Resource) y.getArguments()[0];
      res.getDeployment().getResources().add(res);
      return res;
    });

    Mockito
        .when(wfService.startProcess(Mockito.any(), Mockito.any(), Mockito.any()))
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
    nt.setName(nodeName1);

    Map<String, NodeTemplate> nts = Maps.newHashMap();
    nts.put(nodeName1, nt);

    nt = new NodeTemplate();
    nt.setCapabilities(capabilities);
    nt.setType(nodeType);
    nt.setName(nodeName2);
    nts.put(nodeName2, nt);

    Deployment returneDeployment = basecreateDeploymentSuccessful(deploymentRequest, nts);

    assertThat(returneDeployment.getResources()).hasSize(2);

    assertThat(returneDeployment.getResources())
        .extracting(Resource::getToscaNodeName)
        .containsExactlyInAnyOrder(nodeName1, nodeName2);
    assertThat(returneDeployment.getResources()).allSatisfy(resource -> {
      assertThat(resource.getToscaNodeType()).isEqualTo(nodeType);
      assertThat(resource.getState()).isEqualTo(NodeStates.INITIAL);
    });

    returneDeployment
        .getResources()
        .forEach(resource -> Mockito.verify(resourceRepository).save(resource));

    Mockito.verify(deploymentRepository, Mockito.atLeast(1)).save(returneDeployment);
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

    assertThat(returneDeployment.getResources()).hasSize(1);
  }

  @Test
  public void createComputeScalableWithCountDeploymentSuccessful() throws Exception {
    DeploymentRequest deploymentRequest = new DeploymentRequest();

    String nodeName = "server";
    String nodeType = "tosca.nodes.indigo.Compute";

    Capability capability = new Capability();
    capability.setProperties(Maps.newHashMap());
    ScalarPropertyValue countValue = new ScalarPropertyValue("2");
    countValue.setPrintable(true);
    capability.getProperties().put("count", countValue);

    Map<String, Capability> capabilities = Maps.newHashMap();
    capabilities.put("scalable", capability);

    NodeTemplate nt = new NodeTemplate();
    nt.setCapabilities(capabilities);
    nt.setType(nodeType);
    nt.setName(nodeName);

    Map<String, NodeTemplate> nts = Maps.newHashMap();
    nts.put(nodeName, nt);

    Deployment returneDeployment = basecreateDeploymentSuccessful(deploymentRequest, nts);

    assertThat(returneDeployment.getResources()).hasSize(2);
    assertThat(returneDeployment.getResources()).allSatisfy(resource -> {
      assertThat(resource.getToscaNodeName()).isEqualTo(nodeName);
      assertThat(resource.getToscaNodeType()).isEqualTo(nodeType);
    });

    returneDeployment
        .getResources()
        .forEach(resource -> Mockito.verify(resourceRepository).save(resource));

  }

  @Test
  public void createDeploymentWithCallbackSuccessful() throws Exception {
    DeploymentRequest deploymentRequest = new DeploymentRequest();
    String callback = "http://localhost:8080";
    deploymentRequest.setCallback(callback);

    Deployment returneDeployment = basecreateDeploymentSuccessful(deploymentRequest, null);

    assertThat(returneDeployment.getCallback()).isEqualTo(callback);
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
    nt.setName(nodeName1);
    nts.put(nodeName1, nt);

    nt = new NodeTemplate();
    nt.setType(nodeType);
    nt.setName(nodeName2);
    nts.put(nodeName2, nt);

    Deployment returneDeployment = basecreateDeploymentSuccessful(deploymentRequest, nts);

    assertThat(returneDeployment.getResources())
        .extracting(Resource::getToscaNodeName)
        .containsExactlyInAnyOrder(nodeName1, nodeName2);
    assertThat(returneDeployment.getResources()).allSatisfy(resource -> {
      assertThat(resource.getToscaNodeType()).isEqualTo(nodeType);
    });

    returneDeployment
        .getResources()
        .forEach(resource -> Mockito.verify(resourceRepository).save(resource));
  }

  @Test
  public void deleteDeploymentNotFound() throws Exception {
    Mockito.when(deploymentRepository.findOne("id")).thenReturn(null);

    assertThatThrownBy(() -> deploymentService.deleteDeployment("id"))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  @Parameters({
      "DELETE_IN_PROGRESS",
      "DELETE_COMPLETE" })
  public void deleteDeploymentFailForConflict(Status status) throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.setStatus(status);

    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);

    assertThatThrownBy(() -> deploymentService.deleteDeployment(deployment.getId()))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  @Parameters({
      "CREATE_IN_PROGRESS",
      "CREATE_COMPLETE",
      "CREATE_FAILED",
      "UPDATE_IN_PROGRESS",
      "UPDATE_COMPLETE",
      "UPDATE_FAILED",
      "DELETE_FAILED",
      "UNKNOWN" })
  public void deleteDeploymentSuccesfulWithReferences(Status status) throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.setStatus(status);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    deployment.setEndpoint("endpoint");
    WorkflowReference wr1 = new WorkflowReference(0, RUNTIME_STRATEGY.PER_PROCESS_INSTANCE);
    deployment.getWorkflowReferences().add(wr1);
    WorkflowReference wr2 = new WorkflowReference(1, RUNTIME_STRATEGY.PER_PROCESS_INSTANCE);
    deployment.getWorkflowReferences().add(wr2);

    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    Mockito.when(deploymentRepository.save(deployment)).thenReturn(deployment);
    Mockito.doNothing().when(wfService).abortProcess(Mockito.anyLong(),
        Mockito.any(RUNTIME_STRATEGY.class));

    Mockito
        .when(wfService.startProcess(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(new RuleFlowProcessInstance());

    deploymentService.deleteDeployment(deployment.getId());

    assertThat(deployment.getStatus()).isEqualTo(Status.DELETE_IN_PROGRESS);
    assertThat(deployment.getWorkflowReferences()).hasSize(1);
    Mockito.verify(deploymentRepository, Mockito.never()).delete(deployment);
    Mockito.verify(wfService).abortProcess(wr1.getId(), wr1.getRuntimeStrategy());
    Mockito.verify(wfService).abortProcess(wr2.getId(), wr2.getRuntimeStrategy());
  }

  @Test
  @Parameters({
      "CREATE_IN_PROGRESS",
      "CREATE_COMPLETE",
      "CREATE_FAILED",
      "UPDATE_IN_PROGRESS",
      "UPDATE_COMPLETE",
      "UPDATE_FAILED",
      "DELETE_FAILED",
      "UNKNOWN" })
  public void deleteDeploymentSuccesfulNoReferences(Status status) throws Exception {

    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.setStatus(status);

    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);

    Mockito
        .when(wfService.startProcess(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(new RuleFlowProcessInstance());

    deploymentService.deleteDeployment(deployment.getId());

    Mockito.verify(wfService, Mockito.never()).abortProcess(Mockito.anyLong(),
        Mockito.any(RUNTIME_STRATEGY.class));
    Mockito.verify(deploymentRepository, Mockito.never()).save(deployment);
    Mockito.verify(deploymentRepository, Mockito.times(1)).delete(deployment);
  }

  // test fail with chrono
  // TO-DO

  @Test
  @Parameters({
      "CHRONOS",
      "MARATHON" })
  public void updateDeploymentBadRequest(DeploymentProvider provider) throws Exception {

    String id = UUID.randomUUID().toString();
    Deployment deployment = ControllerTestUtils.createDeployment(id);
    deployment.setDeploymentProvider(provider);
    Mockito.when(deploymentRepository.findOne(id)).thenReturn(deployment);

    assertThatThrownBy(() -> deploymentService.updateDeployment(id, new DeploymentRequest()))
        .isInstanceOf(BadRequestException.class);

  }

  @Test
  public void updateDeploymentNotFound() throws Exception {
    String id = UUID.randomUUID().toString();
    Mockito.when(deploymentRepository.findOne(id)).thenReturn(null);
    assertThatThrownBy(() -> deploymentService.updateDeployment(id, null))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  @Parameters({
      "CREATE_FAILED",
      "CREATE_IN_PROGRESS",
      "DELETE_IN_PROGRESS",
      "DELETE_FAILED",
      "DELETE_COMPLETE",
      "UPDATE_IN_PROGRESS",
      "UNKNOWN" })
  public void updateDeploymentConflict(Status status) throws Exception {
    String id = UUID.randomUUID().toString();
    Deployment deployment = ControllerTestUtils.createDeployment(id);
    deployment.setDeploymentProvider(DeploymentProvider.HEAT);
    deployment.setStatus(status);
    Mockito.when(deploymentRepository.findOne(id)).thenReturn(deployment);

    assertThatThrownBy(() -> deploymentService.updateDeployment(id, new DeploymentRequest()))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  @Parameters({
      "CREATE_COMPLETE",
      "UPDATE_FAILED",
      "UPDATE_COMPLETE" })
  public void updateDeploymentSuccess(Status status) throws Exception {
    DeploymentRequest deploymentRequest = new DeploymentRequest();
    Map<String, NodeTemplate> nts = getNodeTemplates();

    // case create complete
    Deployment deployment = basecreateDeploymentSuccessful(deploymentRequest, nts);
    deployment.setDeploymentProvider(DeploymentProvider.IM);

    deployment.setStatus(status);
    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    deploymentService.updateDeployment(deployment.getId(), deploymentRequest);
    assertThat(deployment.getStatus()).isEqualTo(Status.UPDATE_IN_PROGRESS);
  }

  private static Map<String, NodeTemplate> getNodeTemplates() {
    String nodeName1 = "server1";
    String nodeName2 = "server2";
    String nodeType = "tosca.nodes.indigo.Compute";

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
    return nts;
  }
}
