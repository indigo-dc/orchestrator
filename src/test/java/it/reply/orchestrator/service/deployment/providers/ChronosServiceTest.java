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

package it.reply.orchestrator.service.deployment.providers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import alien4cloud.model.components.ComplexPropertyValue;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.tosca.model.ArchiveRoot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import it.infn.ba.indigo.chronos.client.Chronos;
import it.infn.ba.indigo.chronos.client.model.v1.Job;
import it.infn.ba.indigo.chronos.client.utils.ChronosException;
import it.reply.orchestrator.config.properties.ChronosProperties;
import it.reply.orchestrator.config.specific.ToscaParserAwareTest;
import it.reply.orchestrator.controller.ControllerTestUtils;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.deployment.ChronosJobsOrderedIterator;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.ToscaServiceImpl;
import it.reply.orchestrator.service.ToscaServiceTest;
import it.reply.orchestrator.service.deployment.providers.ChronosServiceImpl.IndigoJob;
import it.reply.orchestrator.service.deployment.providers.ChronosServiceImpl.JobState;
import it.reply.orchestrator.service.deployment.providers.factory.ChronosClientFactory;
import it.reply.orchestrator.util.TestUtil;
import it.reply.orchestrator.utils.CommonUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

@RunWith(JUnitParamsRunner.class)
@JsonTest
public class ChronosServiceTest extends ToscaParserAwareTest {

  @InjectMocks
  private ChronosServiceImpl chronosService;

  @SpyBean
  private ChronosProperties chronosProperties;

  @SpyBean
  @Autowired
  private ToscaServiceImpl toscaService;

  @MockBean
  private DeploymentStatusHelper deploymentStatusHelper;

  @MockBean
  private DeploymentRepository deploymentRepository;

  @MockBean
  private ResourceRepository resourceRepository;

  @MockBean
  private Chronos chronos;

  @MockBean
  private ChronosClientFactory chronosClientFactory;

  @SpyBean
  private ObjectMapper objectMapper;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(chronosClientFactory.build(any(Deployment.class))).thenReturn(chronos);
    chronosProperties.setLocalVolumesHostBasePath("/tmp");
    chronosProperties.afterPropertiesSet();
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

    assertThat(
        ((ScalarPropertyValue) envVars.get("INPUT_ONEDATA_PROVIDERS")).getValue())
        .isEqualTo("input_provider_1");
    assertThat(
        ((ScalarPropertyValue) envVars.get("OUTPUT_ONEDATA_PROVIDERS")).getValue())
        .isEqualTo("output_provider_3");
  }

  @Test
  public void checkOneDataHardCodedParamsSubstitutionInServiceTemplate() throws Exception {
    String template = TestUtil.getFileContentAsString(ToscaServiceTest.TEMPLATES_ONEDATA_BASE_DIR
        + "tosca_onedata_requirements_hardcoded_service.yaml");

    Map<String, Object> inputs = new HashMap<String, Object>();
    ArchiveRoot ar = toscaService.prepareTemplate(template, inputs);
    OneData serviceOd = OneData
        .builder()
        .token("token")
        .space("space")
        .path("path")
        .providers("provider")
        .build();
    OneData inputOd = OneData
        .builder()
        .token("token_input")
        .space("space_input")
        .path("path_input")
        .providers("provider_input_1,provider_input_2")
        .build();
    OneData outputOd = OneData
        .builder()
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

    assertThat(
        ((ScalarPropertyValue) envVars.get("ONEDATA_SERVICE_TOKEN")).getValue())
        .isEqualTo(serviceOd.getToken());
    assertThat(
        ((ScalarPropertyValue) envVars.get("ONEDATA_SPACE")).getValue())
        .isEqualTo(serviceOd.getSpace());
    assertThat(
        ((ScalarPropertyValue) envVars.get("ONEDATA_PATH")).getValue())
        .isEqualTo(serviceOd.getPath());
    assertThat(serviceOd.getProviders()).hasSize(1);
    assertThat(((ScalarPropertyValue) envVars.get("ONEDATA_PROVIDERS")).getValue())
        .isEqualTo(serviceOd.getProviders().get(0).getEndpoint());
  }

  @Test
  @Parameters({"0,0,FRESH", "1,0,SUCCESS", "1,1,SUCCESS", "0,1,FAILURE"})
  public void getLastState(int successCount, int errorCount, JobState expectedState) {
    Job job = new Job();
    job.setSuccessCount(successCount);
    job.setErrorCount(errorCount);
    JobState lastState = ChronosServiceImpl.getLastState(job);

    assertThat(lastState).isEqualTo(expectedState);
  }

  @Test
  @Parameters({"true", "false"})
  public void doDeploy(boolean isLast) throws ChronosException {
    Deployment deployment = ControllerTestUtils.createDeployment();
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    Job job = new Job();
    job.setName("ChronosName");

    ChronosJobsOrderedIterator topologyIterator = mock(ChronosJobsOrderedIterator.class);
    dm.setChronosJobsIterator(topologyIterator);

    when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    when(topologyIterator.hasNext()).thenReturn(true, !isLast);
    when(topologyIterator.next()).thenReturn(new IndigoJob(job, "toscaName"));

    assertThat(chronosService.doDeploy(dm)).isEqualTo(isLast);
    verify(chronos, times(1)).createJob(job);
    if (isLast) {
      verify(topologyIterator, times(1)).reset();
    }
  }

  @Test
  @Parameters({"true|true", "true|false", "false|true", "false|false"})
  public void isDeployed(boolean isCompleted, boolean isLast) {
    Deployment deployment = ControllerTestUtils.createDeployment();
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    Job job = new Job();
    job.setName("ChronosName");

    ChronosJobsOrderedIterator iterator = mock(ChronosJobsOrderedIterator.class);
    dm.setChronosJobsIterator(iterator);

    when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    when(iterator.hasNext()).thenReturn(true, !isLast);
    when(iterator.next()).thenReturn(new IndigoJob(job, "toscaName"));

    Job returnedJob = new Job();
    if (isCompleted) {
      returnedJob.setSuccessCount(1);
    } else {
      returnedJob.setSuccessCount(0);
      returnedJob.setErrorCount(0);
    }

    when(chronos.getJob("ChronosName")).thenReturn(Lists.newArrayList(returnedJob));

    boolean result = chronosService.isDeployed(dm);

    if (!isCompleted) {
      assertThat(result).isFalse();
      assertThat(dm.isSkipPollInterval()).isFalse();
    } else {
      assertThat(result).isEqualTo(isLast);
      assertThat(dm.isSkipPollInterval()).isTrue();
    }

  }

  @Test
  public void testCheckJobsOnChronosFail() {
    Job job = new Job();
    job.setName("JobName");
    job.setSuccessCount(0);
    job.setErrorCount(1);
    when(chronos.getJob("JobName")).thenReturn(Lists.newArrayList(job));

    assertThatCode(
        () -> chronosService.checkJobsOnChronos(chronos, new IndigoJob(job, "toscaName")))
        .isInstanceOf(DeploymentException.class)
        .hasMessage("Chronos job JobName failed to execute");
  }

  @Test
  @Parameters({"true", "false"})
  public void createJobOnChronosSuccessful(boolean isScheduled) throws ChronosException {

    Job job = new Job();
    if (!isScheduled) {
      job.setParents(Lists.newArrayList("some parent"));
    }
    chronosService.createJobOnChronos(chronos, new IndigoJob(job, "toscaName"));

    if (isScheduled) {
      verify(chronos, times(1)).createJob(job);
      verify(chronos, never()).createDependentJob(any(Job.class));
    } else {
      verify(chronos, never()).createJob(any(Job.class));
      verify(chronos, times(1)).createDependentJob(job);
    }
  }

  @Test
  public void createJobOnChronosFail() throws ChronosException {
    Job job = new Job();
    job.setName("JobName");
    doThrow(new ChronosException(500, "some message")).when(chronos).createJob(job);

    assertThatCode(
        () -> chronosService.createJobOnChronos(chronos, new IndigoJob(job, "toscaName")))
        .isInstanceOf(DeploymentException.class)
        .hasCauseExactlyInstanceOf(ChronosException.class)
        .hasMessage(
            "Failed to launch job <%s> on Chronos; nested exception is %s (http status: %s)",
            "JobName", "some message", 500);
  }

  @Test
  public void doUpdateNoSupport() {
    assertThatCode(() -> chronosService.doUpdate(null, null))
        .isExactlyInstanceOf(UnsupportedOperationException.class)
        .hasMessage("Chronos job deployments do not support update.");
  }

  @Test
  public void isUndeployed() {
    Deployment deployment = ControllerTestUtils.createDeployment();
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    assertThat(chronosService.isUndeployed(dm)).isTrue();
  }

  @Test
  @Parameters({"true", "false"})
  public void doUndeploySuccessful(boolean isLast) throws ChronosException {
    Deployment deployment = ControllerTestUtils.createDeployment();
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    Job job = new Job();
    job.setName("ChronosName");

    ChronosJobsOrderedIterator iterator = mock(ChronosJobsOrderedIterator.class);
    dm.setChronosJobsIterator(iterator);

    when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    when(iterator.hasNext()).thenReturn(true, !isLast);
    when(iterator.next()).thenReturn(new IndigoJob(job, "toscaName"));

    assertThat(chronosService.doUndeploy(dm)).isEqualTo(isLast);
    verify(chronos, times(1)).deleteJob("ChronosName");
  }

  @Test
  @Parameters({"400|false", "404|false", "500|true"})
  public void deleteJobsOnChronosWithChronosException(int statusCode, boolean shouldFail)
      throws ChronosException {
    Job job = new Job();
    job.setName("ChronosName");
    IndigoJob indigoJob = new IndigoJob(job, "toscaName");
    doThrow(new ChronosException(statusCode, "someMessage")).when(chronos).deleteJob("ChronosName");

    AbstractThrowableAssert<?, ? extends Throwable> assertion = assertThatCode(
        () -> chronosService.deleteJobsOnChronos(chronos, indigoJob));
    if (shouldFail) {
      assertion.isInstanceOf(DeploymentException.class)
          .hasCauseExactlyInstanceOf(ChronosException.class).hasMessage(
          "Failed to delete job ChronosName on Chronos; nested exception is %s (http status: %s)",
          "someMessage", statusCode);
    } else {
      assertion.doesNotThrowAnyException();
    }
  }

  @Test
  public void finalizeUndeployUpdateOnSuccess() {
    Deployment deployment = ControllerTestUtils.createDeployment();
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    chronosService.finalizeUndeploy(dm);

    verify(deploymentStatusHelper, times(1))
        .updateOnSuccess(deployment.getId());
  }

  @Test
  public void finalizeDeployTestUpdateOnSuccess() {
    Deployment deployment = ControllerTestUtils.createDeployment();
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    chronosService.finalizeDeploy(dm);

    verify(deploymentStatusHelper, times(1))
        .updateOnSuccess(deployment.getId());
  }

  @Test
  public void generateJobGraph() throws IOException {
    Deployment deployment = generateDeployment();

    when(chronosClientFactory.getFrameworkProperties(deployment)).thenReturn(chronosProperties);

    ChronosJobsOrderedIterator topologyIterator = chronosService.getJobsTopologicalOrder(
        deployment, new HashMap<>());
    topologyIterator.next();
    assertThat(objectMapper.writer(SerializationFeature.INDENT_OUTPUT)
        .writeValueAsString(topologyIterator)).isEqualToNormalizingNewlines(TestUtil
        .getFileContentAsString(ToscaServiceTest.TEMPLATES_BASE_DIR + "chronos_2_jobs.json"));
  }

  private Deployment generateDeployment() throws IOException {
    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.getParameters().put("cpus", "1.0");

    deployment.setTemplate(
        TestUtil
            .getFileContentAsString(ToscaServiceTest.TEMPLATES_BASE_DIR + "chronos_2_jobs.yaml"));

    Resource runtime1 = new Resource();
    runtime1.setDeployment(deployment);
    runtime1.setId("1");
    runtime1.setState(NodeStates.INITIAL);
    runtime1.setToscaNodeName("docker_runtime1");
    runtime1.setToscaNodeType("tosca.nodes.indigo.Container.Runtime.Docker");
    deployment.getResources().add(runtime1);

    Resource job1 = new Resource();
    job1.setDeployment(deployment);
    job1.setId("2");
    job1.setState(NodeStates.INITIAL);
    job1.setToscaNodeName("chronos_job");
    job1.setToscaNodeType("tosca.nodes.indigo.Container.Application.Docker.Chronos");
    job1.addRequiredResource(runtime1);
    deployment.getResources().add(job1);

    Mockito
        .when(resourceRepository.findByToscaNodeNameAndDeployment_id("docker_runtime1",
            deployment.getId()))
        .thenReturn(Lists.newArrayList(runtime1));

    Mockito
        .when(resourceRepository.findByToscaNodeNameAndDeployment_id("chronos_job",
            deployment.getId()))
        .thenReturn(Lists.newArrayList(job1));

    Resource runtime2 = new Resource();
    runtime2.setDeployment(deployment);
    runtime2.setId("3");
    runtime2.setState(NodeStates.INITIAL);
    runtime2.setToscaNodeName("docker_runtime2");
    runtime2.setToscaNodeType("tosca.nodes.indigo.Container.Runtime.Docker");
    deployment.getResources().add(runtime2);

    Resource job2 = new Resource();
    job2.setDeployment(deployment);
    job2.setId("4");
    job2.setState(NodeStates.INITIAL);
    job2.setToscaNodeName("chronos_job_upload");
    job2.setToscaNodeType("tosca.nodes.indigo.Container.Application.Docker.Chronos");
    job2.addRequiredResource(runtime2);
    deployment.getResources().add(job2);

    Mockito
        .when(resourceRepository.findByToscaNodeNameAndDeployment_id("docker_runtime2",
            deployment.getId()))
        .thenReturn(Lists.newArrayList(runtime2));

    Mockito
        .when(resourceRepository.findByToscaNodeNameAndDeployment_id("chronos_job_upload",
            deployment.getId()))
        .thenReturn(Lists.newArrayList(job2));

    return deployment;
  }
}
