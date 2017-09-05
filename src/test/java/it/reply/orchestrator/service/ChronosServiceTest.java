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

package it.reply.orchestrator.service;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;

import com.google.common.collect.ImmutableMap;

import alien4cloud.model.components.ComplexPropertyValue;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.tosca.model.ArchiveRoot;

import it.infn.ba.indigo.chronos.client.Chronos;
import it.infn.ba.indigo.chronos.client.model.v1.Job;
import it.reply.orchestrator.config.properties.OrchestratorProperties;
import it.reply.orchestrator.config.specific.ToscaParserAwareTest;
import it.reply.orchestrator.controller.ControllerTestUtils;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.deployment.DeploymentMessage.TemplateTopologicalOrderIterator;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.service.deployment.providers.ChronosServiceImpl;
import it.reply.orchestrator.service.deployment.providers.ChronosServiceImpl.IndigoJob;
import it.reply.orchestrator.service.deployment.providers.ChronosServiceImpl.JobState;
import it.reply.orchestrator.service.deployment.providers.DeploymentStatusHelper;
import it.reply.orchestrator.util.TestUtil;
import it.reply.orchestrator.utils.CommonUtils;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ChronosServiceTest extends ToscaParserAwareTest {

  @InjectMocks
  @Spy
  private ChronosServiceImpl chronosService;
  
  @Spy
  private OrchestratorProperties orchestratorProperties;

  @Spy
  @InjectMocks
  private ToscaServiceImpl toscaService;
  
  @Spy
  private IndigoInputsPreProcessorService indigoInputsPreProcessorService;

  @Mock
  private DeploymentStatusHelper deploymentStatusHelper;

  @Mock
  private DeploymentRepository deploymentRepository;
  
  @Mock
  private ResourceRepository resourceRepository;
  
  @Mock
  private Chronos chronos;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    Mockito.doReturn(chronos).when(chronosService).getChronosClient();
  }

  @Override
  protected ToscaServiceImpl getToscaService() {
    return toscaService;
  }

  @Test
  public void checkOneDataHardCodedParamsSubstitutionInUserDefinedTemplate() throws Exception {
    String template = TestUtil.getFileContentAsString(ToscaServiceTest.TEMPLATES_ONEDATA_BASE_DIR
        + "tosca_onedata_requirements_hardcoded_userdefined.yaml");
    Map<String, Object> inputs = new HashMap<String, Object>();
    inputs.put("input_onedata_providers", "input_provider_1,input_provider_2");
    inputs.put("input_onedata_space", "input_onedata_space");
    inputs.put("input_onedata_token", "input_token");
    inputs.put("input_path", "input/path");
    inputs.put("input_onedata_zone", "input_zone");
    inputs.put("output_onedata_providers", "output_provider_3,output_provider_4");
    inputs.put("output_onedata_space", "output_onedata_space");
    inputs.put("output_onedata_token", "output_token");
    inputs.put("output_path", "output/path");
    inputs.put("output_onedata_zone", "output_zone");
    ArchiveRoot ar = toscaService.prepareTemplate(template, inputs);

    Map<String, OneData> odRequirements = toscaService.extractOneDataRequirements(ar, inputs);

    Map<String, OneData> odParameters = odRequirements;
    // ImmutableMap.of("service", new OneData("token", "space", "path",
    // "provider"), "input",
    // new OneData("token_input", "space_input", "path_input",
    // "provider_input_1,provider_input_2"),
    // "output", new OneData("token_output", "space_output", "path_output",
    // "provider_output_1,provider2_output_2"));

    String customizedTemplate = chronosService.replaceHardCodedParams(template, odParameters);

    // Re-parse template (TODO: serialize the template in-memory
    // representation?)
    ar = toscaService.prepareTemplate(customizedTemplate, inputs);

    Map<String, NodeTemplate> nodes = ar.getTopology().getNodeTemplates();
    NodeTemplate chronosJob = nodes.get("chronos_job");
    Map<String, Object> envVars = CommonUtils
        .<ComplexPropertyValue>optionalCast(
            toscaService.getNodePropertyByName(chronosJob, "environment_variables"))
        .get()
        .getValue();

    assertEquals("input_provider_1",
        ((ScalarPropertyValue) envVars.get("INPUT_ONEDATA_PROVIDERS")).getValue());
    assertEquals("output_provider_3",
        ((ScalarPropertyValue) envVars.get("OUTPUT_ONEDATA_PROVIDERS")).getValue());
  }

  @Test
  public void checkOneDataHardCodedParamsSubstitutionInServiceTemplate() throws Exception {
    String template = TestUtil.getFileContentAsString(ToscaServiceTest.TEMPLATES_ONEDATA_BASE_DIR
        + "tosca_onedata_requirements_hardcoded_service.yaml");

    Map<String, Object> inputs = new HashMap<String, Object>();
    ArchiveRoot ar = toscaService.prepareTemplate(template, inputs);
    OneData serviceOd = OneData.builder()
        .token("token")
        .space("space")
        .path("path")
        .providers("provider")
        .build();
    OneData inputOd = OneData.builder()
        .token("token_input")
        .space("space_input")
        .path("path_input")
        .providers("provider_input_1,provider_input_2")
        .build();
    OneData outputOd = OneData.builder()
        .token("token_output")
        .space("space_output")
        .path("path_output")
        .providers("provider_output_1,provider2_output_2")
        .build();
    Map<String, OneData> odParameters =
        ImmutableMap.of("service", serviceOd, "input", inputOd, "output", outputOd);

    String customizedTemplate = chronosService.replaceHardCodedParams(template, odParameters);

    // Re-parse template (TODO: serialize the template in-memory
    // representation?)
    ar = toscaService.prepareTemplate(customizedTemplate, inputs);

    Map<String, NodeTemplate> nodes = ar.getTopology().getNodeTemplates();
    NodeTemplate chronosJob = nodes.get("chronos_job");
    Map<String, Object> envVars = CommonUtils
        .<ComplexPropertyValue>optionalCast(
            toscaService.getNodePropertyByName(chronosJob, "environment_variables"))
        .get()
        .getValue();

    assertEquals(serviceOd.getToken(),
        ((ScalarPropertyValue) envVars.get("ONEDATA_SERVICE_TOKEN")).getValue());
    assertEquals(serviceOd.getSpace(),
        ((ScalarPropertyValue) envVars.get("ONEDATA_SPACE")).getValue());
    assertEquals(serviceOd.getPath(),
        ((ScalarPropertyValue) envVars.get("ONEDATA_PATH")).getValue());
    assertEquals(serviceOd.getProviders().size(), 1);
    assertEquals(serviceOd.getProviders().stream().map(info -> info.getEndpoint())
        .collect(Collectors.toList()).get(0),
        ((ScalarPropertyValue) envVars.get("ONEDATA_PROVIDERS")).getValue());
  }

  @Test
  public void getLastState() {
    Job job = new Job();
    job.setSuccessCount(5);
    JobState lastState = ChronosServiceImpl.getLastState(job);
    assertEquals(JobState.SUCCESS, lastState);
    job.setSuccessCount(0);
    job.setErrorCount(1);
    lastState = ChronosServiceImpl.getLastState(job);
    assertEquals(JobState.FAILURE, lastState);
    job.setSuccessCount(0);
    job.setErrorCount(0);
    lastState = ChronosServiceImpl.getLastState(job);
    assertEquals(JobState.FRESH, lastState);
  }

  @Test
  public void doDeploy() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);
    ArchiveRoot ar = new ArchiveRoot();
    Topology topology = new Topology();
    topology.setNodeTemplates(new HashMap<>());
    ar.setTopology(topology);
    Mockito.when(deploymentRepository.findOne(deployment.getId()))
    .thenReturn(deployment);
    Mockito.doReturn(ar).when(toscaService).prepareTemplate(anyString(),
        anyMapOf(String.class, Object.class));
    Assertions.assertThat(chronosService.doDeploy(dm)).isTrue();
  }

  @Test
  public void isDeployedNoMoreJob() {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);
    TemplateTopologicalOrderIterator templateTopologicalOrderIterator =
        new TemplateTopologicalOrderIterator(new ArrayList<>());
    dm.setTemplateTopologicalOrderIterator(templateTopologicalOrderIterator);
    Mockito.when(deploymentRepository.findOne(deployment.getId()))
    .thenReturn(deployment);
    Assert.assertTrue(chronosService.isDeployed(dm));
  }
  
  /*
  @Test
  public void isDeployedMoreJob() {
    DeploymentMessage dm = TestUtil.generateDeployDm();
    ArrayList<Resource> resources = new ArrayList<>();
    resources.add(new Resource("indigoJob"));
    TemplateTopologicalOrderIterator templateTopologicalOrderIterator =
        new TemplateTopologicalOrderIterator(resources);
    
    /////////////////////////
    Map<String,IndigoJob> chronosJobGraph = new HashMap<String,IndigoJob>();
    Job job = new Job();
    job.setName("indigoJob");
    IndigoJob indigoJob = new IndigoJob("indigoJob",job);
    
    chronosJobGraph.put("indigoJob", indigoJob);
    dm.setChronosJobGraph(chronosJobGraph);
    
    dm.setTemplateTopologicalOrderIterator(templateTopologicalOrderIterator);
    Assert.assertTrue(chronosServiceImplMock.isDeployed(dm));
  }*/

  @Test(expected = RuntimeException.class)
  public void isDeployedMoreJobFailGetJobStatu() {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);
    Map<String, IndigoJob> jobGraph = new HashMap<>();
    Job chronosJob = new Job();
    chronosJob.setName("chronosJobName");
    IndigoJob job = new IndigoJob("toscaNodeName", chronosJob);
    jobGraph.put("toscaNodeName", job);
    dm.setChronosJobGraph(jobGraph);
    TemplateTopologicalOrderIterator templateTopologicalOrderIterator =
        Mockito.mock(TemplateTopologicalOrderIterator.class);
    dm.setTemplateTopologicalOrderIterator(templateTopologicalOrderIterator);
    orchestratorProperties.setJobChunkSize(1);

    Resource currentNode = new Resource();
    currentNode.setToscaNodeName("toscaNodeName");

    Mockito.when(templateTopologicalOrderIterator.getCurrent()).thenReturn(currentNode);

    // crash in getJobStatus because client.getJob exec async request. can i mock this?
    chronosService.isDeployed(dm);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void doUpdateNoSupport() {
    chronosService.doUpdate(null, null);
  }

  @Test
  public void isUndeploy() {
    Assert.assertTrue(chronosService.isUndeployed(null));
  }

  @Test
  public void finalizeUndeployUpdateOnSuccess() {
    DeploymentMessage dm = new DeploymentMessage();
    Mockito.doNothing().when(deploymentStatusHelper).updateOnSuccess(any(String.class));
    chronosService.finalizeUndeploy(dm);
  }
  
  @Test
  public void doUndeployWithChronosJob() {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);
    Map<String, IndigoJob> jobGraph = new HashMap<>();
    Job chronosJob = new Job();
    chronosJob.setName("chronosJobName");
    IndigoJob job = new IndigoJob("toscaNodeName", chronosJob);
    jobGraph.put("toscaNodeName", job);
    dm.setChronosJobGraph(jobGraph);
    
    Mockito.when(deploymentRepository.findOne(deployment.getId()))
    .thenReturn(deployment);
    
   Assertions.assertThat(chronosService.doUndeploy(dm)).isTrue();
  }
  
  @Test
  public void doUndeployWithoutChronosJob() {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);
    Mockito.when(deploymentRepository.findOne(deployment.getId()))
    .thenReturn(deployment);
    Assert.assertTrue(chronosService.doUndeploy(dm));
  }

  @Test
  public void finalizeDeployTestUpdateOnSuccess() {
    Deployment deployment = ControllerTestUtils.createDeployment(2);
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);
    Mockito.doNothing().when(deploymentStatusHelper).updateOnSuccess(any(String.class));
    chronosService.finalizeDeploy(dm);
  }

}
