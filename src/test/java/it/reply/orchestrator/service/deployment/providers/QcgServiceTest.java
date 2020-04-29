/*
 * Copyright © 2019-2020 I.N.F.N.
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


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Lists;

import it.infn.ba.deep.qcg.client.Qcg;
import it.infn.ba.deep.qcg.client.model.Job;
import it.infn.ba.deep.qcg.client.model.JobDescription;
import it.infn.ba.deep.qcg.client.model.JobDescriptionExecution;
import it.infn.ba.deep.qcg.client.utils.QcgException;
import it.reply.orchestrator.config.specific.ToscaParserAwareTest;
import it.reply.orchestrator.controller.ControllerTestUtils;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.CloudProviderEndpoint.IaaSType;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.CloudServiceType;
import it.reply.orchestrator.dto.cmdb.QcgService;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.deployment.QcgJobsOrderedIterator;
import it.reply.orchestrator.dto.workflow.CloudServicesOrderedIterator;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.exception.service.BusinessWorkflowException;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.function.ThrowingFunction;
import it.reply.orchestrator.service.ToscaServiceImpl;
import it.reply.orchestrator.service.ToscaServiceTest;
import it.reply.orchestrator.service.deployment.providers.QcgServiceImpl.DeepJob;
import it.reply.orchestrator.service.deployment.providers.QcgServiceImpl.JobState;
import it.reply.orchestrator.service.deployment.providers.factory.QcgClientFactory;
import it.reply.orchestrator.util.TestUtil;
import it.reply.orchestrator.utils.ToscaConstants.Nodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.web.client.RestTemplate;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


@RunWith(JUnitParamsRunner.class)
@JsonTest
public class QcgServiceTest extends ToscaParserAwareTest {

  @InjectMocks
  private QcgServiceImpl qcgService;

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
  private Qcg qcg;

  @MockBean
  private QcgClientFactory qcgClientFactory;

  @SpyBean
  private ObjectMapper objectMapper;

  @Spy
  protected RestTemplate restTemplate;

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Before
  public void setup() throws Exception {

    MockitoAnnotations.initMocks(this);

    when(qcgClientFactory.build(any(CloudProviderEndpoint.class), any(String.class)))
        .thenReturn(qcg);

    Mockito
        .when(oauth2tokenService.executeWithClientForResult(
            Mockito.any(), Mockito.any(), Mockito.any()))
    	        .thenAnswer(y -> ((ThrowingFunction) y.getArguments()[1]).apply("token"));

  }

  private CloudProviderEndpoint generateCloudProviderEndpoint() {
    return CloudProviderEndpoint
        .builder()
        .iaasType(IaaSType.QCG)
        .cpEndpoint("http://www.example.com/api")
        .cpComputeServiceId("qcg-service-id")
        .build();
  }

  @Test
  @Parameters({"SUBMITTED", "PENDING", "EXECUTING", "FAILED", "FINISHED", "?", ""})
  public void getLastState(String jobState) {
    Job job = new Job();
    job.setState(jobState);
    if (jobState.equals("")) {
      assertThatCode(
          () -> QcgServiceImpl.getLastState(job))
          .isInstanceOf(DeploymentException.class)
          .hasMessage("Empty Qcg job status");

    } else {
      if (jobState.equals("?")) {
        assertThatCode(
            () -> QcgServiceImpl.getLastState(job))
            .isInstanceOf(DeploymentException.class)
            .hasMessage("Unknown Qcg job status: ?");
      } else {
        JobState lastState = QcgServiceImpl.getLastState(job);
        assertThat(lastState.toString()).isEqualTo(jobState);
      }
    }
  }

  @Test
  public void getDeploymentExtendedInfoInternal() {
    Deployment deployment = ControllerTestUtils.createDeployment();
    DeploymentMessage dm = generateDeployDmQcg(deployment);

    Resource resource1 = ControllerTestUtils.createResource(deployment,
        "tosca.nodes.indigo.Qcg.Job", "qcg_job");
    Map<String,String> metadata1 = new HashMap<>();
    metadata1.put("Job", "{\"Id\": \"1\"}");
    resource1.setMetadata(metadata1);
    Resource resource2 = ControllerTestUtils.createResource(deployment,
        "tosca.nodes.indigo.Qcg.Job", "qcg_job");
    Map<String,String> metadata2 = new HashMap<>();
    metadata2.put("Job", "{\"Id\": \"2\"}");
    resource2.setMetadata(metadata2);
    List<Resource> resources = new ArrayList<>();
    resources.add(resource1);
    resources.add(resource2);
    when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    when(resourceRepository.findByDeployment_id(deployment.getId())).thenReturn(resources);
    assertThat(qcgService.getDeploymentExtendedInfo(dm).get())
        .isEqualTo("[{\"Id\": \"1\"},{\"Id\": \"2\"}]");
  }

  @Test
  public void getDeploymentLogInternal() {
    DeploymentMessage dm = new DeploymentMessage();
    assertThat(qcgService.getDeploymentLog(dm)).isEqualTo(Optional.empty());
  }

  @Test
  @Parameters({"true", "false"})
  public void doDeploy(boolean isLast) throws QcgException {
    Deployment deployment = ControllerTestUtils.createDeployment();
    DeploymentMessage dm = generateDeployDmQcg(deployment);

    Job job = new Job();
    job.setCpus(1);

    JobDescription description = new JobDescription();
    JobDescriptionExecution execution = new JobDescriptionExecution();
    execution.setExecutable("/usr/bin/printf");
    description.setExecution(execution);
    job.setDescription(description);

    QcgJobsOrderedIterator topologyIterator = mock(QcgJobsOrderedIterator.class);
    dm.setQcgJobsIterator(topologyIterator);
    DeepJob deepJob = new DeepJob(job, "qcg_job");
    Resource resource1 = ControllerTestUtils.createResource(deployment,
        "tosca.nodes.indigo.Qcg.Job", "qcg_job");
    Resource resource2 = ControllerTestUtils.createResource(deployment,
        "tosca.nodes.indigo.Qcg.Job", "qcg_job");
    Map<String,String> metadata = new HashMap<>();
    metadata.put("Job", "{}");
    resource2.setMetadata(metadata);
    List<Resource> resources = new ArrayList<>();
    resources.add(resource1);
    resources.add(resource2);
    when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    when(topologyIterator.hasNext()).thenReturn(true, !isLast);
    when(topologyIterator.next()).thenReturn(deepJob);
    when(resourceRepository.findByToscaNodeNameAndDeployment_id(
        "qcg_job", deployment.getId())).thenReturn(resources);
    when(qcg.createJob(job.getDescription())).thenReturn(job);

    assertThat(qcgService.doDeploy(dm)).isEqualTo(isLast);
    verify(qcg, times(1)).createJob(description);
    if (isLast) {
      verify(topologyIterator, times(1)).reset();
    }
  }

  @Test
  @Parameters({"true|true", "true|false", "false|true", "false|false"})
  public void isDeployed(boolean isCompleted, boolean isLast) {
    Deployment deployment = ControllerTestUtils.createDeployment();
    DeploymentMessage dm = generateDeployDmQcg(deployment);

    Job job = new Job();
    job.setId("999");

    JobDescription description = new JobDescription();
    job.setDescription(description);
    QcgJobsOrderedIterator iterator = mock(QcgJobsOrderedIterator.class);
    dm.setQcgJobsIterator(iterator);

    when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    when(iterator.hasCurrent()).thenReturn(true, true);
    when(iterator.hasNext()).thenReturn(!isLast);
    when(iterator.current()).thenReturn(new DeepJob(job, "toscaName"));

    Job returnedJob = new Job();
    if (isCompleted) {
      returnedJob.setState("FINISHED");
    } else {
      returnedJob.setState("EXECUTING");
    }

    when(qcg.getJob("999")).thenReturn(returnedJob);

    boolean result = qcgService.isDeployed(dm);

    if (!isCompleted) {
      assertThat(result).isFalse();
      assertThat(dm.isSkipPollInterval()).isFalse();
    } else {
      assertThat(result).isEqualTo(isLast);
      assertThat(dm.isSkipPollInterval()).isTrue();
    }

  }

  @Test
  public void testCheckJobsOnQcgFail() {
    Job job = new Job();
    job.setId("999");
    job.setErrors("fail");
    job.setExit_code(2);
    job.setState("FAILED");
    when(qcg.getJob("999")).thenReturn(job);

    assertThatCode(
        () -> qcgService.checkJobState(job))
        .isInstanceOf(DeploymentException.class)
        .hasMessage("Qcg job 999 failed to execute with exit code:2 - message: fail");
  }

  @Test
  public void createJobOnQcgSuccessful() throws QcgException {

    Job job = new Job();
    JobDescription description = new JobDescription();
    JobDescriptionExecution execution = new JobDescriptionExecution();
    execution.setExecutable("/usr/bin/printf");
    description.setExecution(execution);
    job.setDescription(description);

    qcgService
    	.createJobOnQcg(generateCloudProviderEndpoint(), null, new DeepJob(job, "toscaName"));
    verify(qcg, times(1)).createJob(description);
  }

  @Test
  public void createJobOnQcgFail() throws QcgException {
    Job job = new Job();
    JobDescription description = new JobDescription();
    JobDescriptionExecution execution = new JobDescriptionExecution();
    execution.setExecutable("/usr/bin/printf");
    description.setExecution(execution);
    job.setDescription(description);

    doThrow(new QcgException(500, "some message")).when(qcg).createJob(description);

    assertThatCode(
        () -> qcgService
        .createJobOnQcg(generateCloudProviderEndpoint(), null, new DeepJob(job, "toscaName")))
        .isInstanceOf(DeploymentException.class)
        .hasCauseExactlyInstanceOf(QcgException.class)
        .hasMessage(
            "Failed to launch job on Qcg; nested exception is %s (http status: %s)",
            "some message", 500);
  }

  @Test
  public void doUpdateNoSupport() {
    assertThatCode(() -> qcgService.doUpdate(null, null))
        .isExactlyInstanceOf(UnsupportedOperationException.class)
        .hasMessage("Qcg job deployments do not support update.");
  }

  @Test
  public void isUndeployed() {
    Deployment deployment = ControllerTestUtils.createDeployment();
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    assertThat(qcgService.isUndeployed(dm)).isTrue();
  }

  @Test
  public void doCleanFailedDeploySuccessful() throws QcgException {
    Deployment deployment = ControllerTestUtils.createDeployment(1);
    deployment.setEndpoint("999");
    DeploymentMessage dm = generateDeployDmQcg(deployment);
    deployment.getResources().forEach(resource -> resource.setToscaNodeType(Nodes.Types.QCG));

    String jobId = deployment.getEndpoint();

    when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);

    qcgService.cleanFailedDeploy(dm);
    verify(qcg, times(1)).deleteJob(jobId);
  }

  @Test
  public void doProviderTimeoutSuccessful() {
    Deployment deployment = ControllerTestUtils.createDeployment(1);
    deployment.setEndpoint("999");
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    AbstractThrowableAssert<?, ? extends Throwable> assertion = assertThatCode(
        () -> qcgService.doProviderTimeout(dm));
    assertion.isInstanceOf(BusinessWorkflowException.class)
        .hasCauseExactlyInstanceOf(DeploymentException.class)
        .hasMessage("Error executing request to Qcg service;"
            + " nested exception is it.reply.orchestrator.exception.service."
            + "DeploymentException: Qcg service timeout during deployment");
  }

  @Test
  @Parameters({"true", "false"})
  public void doUndeploySuccessful(boolean isLast) {
    Deployment deployment = ControllerTestUtils.createDeployment(isLast ? 1 : 2);
    deployment.setEndpoint(isLast ? "999" : "1000");
    DeploymentMessage dm = generateDeployDmQcg(deployment);
    deployment.getResources().forEach(resource -> resource.setToscaNodeType(Nodes.Types.QCG));

    String jobId = deployment.getEndpoint();

    when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);

    assertThat(qcgService.doUndeploy(dm)).isEqualTo(isLast);
    verify(qcg, times(1)).deleteJob(jobId);
  }

  @Test
  @Parameters({"400|false", "404|false", "500|true"})
  public void deleteJobsOnQcgWithQcgException(int statusCode, boolean shouldFail)
      throws QcgException {

    doThrow(new QcgException(statusCode, "someMessage")).when(qcg).deleteJob("999");

    AbstractThrowableAssert<?, ? extends Throwable> assertion = assertThatCode(
        () -> qcgService
            .deleteJobsOnQcg(generateCloudProviderEndpoint(), null, "999"));
    if (shouldFail) {
      assertion.isInstanceOf(DeploymentException.class)
          .hasCauseExactlyInstanceOf(QcgException.class).hasMessage(
          "Failed to delete job 999 on Qcg; nested exception is %s (http status: %s)",
          "someMessage", statusCode);
    } else {
      assertion.doesNotThrowAnyException();
    }
  }

  @Test
  public void finalizeUndeployUpdateOnSuccess() {
    Deployment deployment = ControllerTestUtils.createDeployment();
    DeploymentMessage dm = generateDeployDmQcg(deployment);

    qcgService.finalizeUndeploy(dm);

    verify(deploymentStatusHelper, times(1))
        .updateOnSuccess(deployment.getId());
  }


  @Test
  public void finalizeDeployTestUpdateOnSuccess() {
    Deployment deployment = ControllerTestUtils.createDeployment();
    DeploymentMessage dm = generateDeployDmQcg(deployment);

    qcgService.finalizeDeploy(dm);

    verify(deploymentStatusHelper, times(1))
        .updateOnSuccess(deployment.getId());
  }

  @Test
  public void generateJobGraph() throws IOException {
    Deployment deployment = generateDeployment();
    DeploymentMessage dm = generateDeployDmQcg(deployment);
    QcgService qs = QcgService
        .qcgBuilder()
        .endpoint("http://www.example.com/api")
        .serviceType(CloudService.QCG_COMPUTE_SERVICE)
        .hostname("www.example.com")
        .providerId("provider-1")
        .id("provider-1-service-1")
        .type(CloudServiceType.COMPUTE)
        .build();

    CloudServicesOrderedIterator csi = new CloudServicesOrderedIterator(Lists.newArrayList(qs));
    csi.next();
    dm.setCloudServicesOrderedIterator(csi);

    QcgJobsOrderedIterator topologyIterator = qcgService.getJobsTopologicalOrder(
        dm, deployment);
    topologyIterator.next();
    assertThat(objectMapper.writer(SerializationFeature.INDENT_OUTPUT)
        .writeValueAsString(topologyIterator)).isEqualToNormalizingNewlines(TestUtil
        .getFileContentAsString(ToscaServiceTest.TEMPLATES_BASE_DIR + "qcg_jobs.json").trim());
  }

  private Deployment generateDeployment() throws IOException {
    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.getParameters().put("cpus", "1.0");

    deployment.setTemplate(
        TestUtil
            .getFileContentAsString(ToscaServiceTest.TEMPLATES_BASE_DIR + "qcg_jobs.yaml"));

    Resource job1 = new Resource();
    job1.setDeployment(deployment);
    job1.setId("1");
    job1.setState(NodeStates.INITIAL);
    job1.setToscaNodeName("qcg_job");
    job1.setToscaNodeType("tosca.nodes.indigo.Qcg.Job");
    deployment.getResources().add(job1);

    Mockito
        .when(resourceRepository.findByToscaNodeNameAndDeployment_id("qcg_job",
            deployment.getId()))
        .thenReturn(Lists.newArrayList(job1));

    return deployment;
  }

  private DeploymentMessage generateDeployDmQcg(Deployment deployment) {
	  DeploymentMessage dm = new DeploymentMessage();
	  dm.setDeploymentId(deployment.getId());
	  CloudProviderEndpoint chosenCloudProviderEndpoint = CloudProviderEndpoint
	      .builder()
	      .cpComputeServiceId(UUID.randomUUID().toString())
	      .cpEndpoint("http://www.example.com/api")
	      .iaasType(IaaSType.QCG)
	      .build();
	  dm.setChosenCloudProviderEndpoint(chosenCloudProviderEndpoint);
	  deployment.setCloudProviderEndpoint(chosenCloudProviderEndpoint);
	  return dm;
  }

}
