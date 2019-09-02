/*
 * Copyright © 2019 I.N.F.N.
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


//import alien4cloud.model.components.ComplexPropertyValue;
//import alien4cloud.model.components.ScalarPropertyValue;
//import alien4cloud.model.topology.NodeTemplate;
//import alien4cloud.tosca.model.ArchiveRoot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
//import com.google.common.collect.ImmutableMap;
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
import it.reply.orchestrator.dto.cmdb.QcgServiceData;
import it.reply.orchestrator.dto.cmdb.Type;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.deployment.QcgJobsOrderedIterator;
//import it.reply.orchestrator.dto.onedata.OneData;
//import it.reply.orchestrator.dto.onedata.OneData.OneDataProviderInfo;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.function.ThrowingFunction;
import it.reply.orchestrator.service.ToscaServiceImpl;
import it.reply.orchestrator.service.ToscaServiceTest;
import it.reply.orchestrator.service.deployment.providers.QcgServiceImpl.DeepJob;
import it.reply.orchestrator.service.deployment.providers.QcgServiceImpl.JobState;
import it.reply.orchestrator.service.deployment.providers.factory.QcgClientFactory;
import it.reply.orchestrator.util.TestUtil;
//import it.reply.orchestrator.utils.CommonUtils;
import it.reply.orchestrator.utils.ToscaConstants.Nodes;

import java.io.IOException;
//import java.util.Map;
import java.util.UUID;

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

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

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
    
//    Job resp1 = (new JobDecoder()).decode("{\"id\":\"999\",\"attributes\":{},\"user\":\"default-user\",\"state\":\"FINISHED\",\"operation\":null,\"note\":\"3c915645-5402-471c-b9c6-dcc84a114ae6\",\"description\":{\"note\":\"3c915645-5402-471c-b9c6-dcc84a114ae6\",\"execution\":{\"directory\":\"/qcg/${QCGNCOMP_JOB_ID}\",\"executable\":\"/usr/bin/date\",\"environment\":{\"USER\":\"slurm_user\",\"QCGNCOMP_JOB_ID\":\"999\",\"QCGNCOMP_JOB_SECRET_AUTH\":\"0a05ef399fc54112abae2b9b1eb4bff8\"},\"directory_policy\":{\"create\":\"OVERWRITE\",\"remove\":\"NEVER\"}}},\"operation_start\":null,\"resource\":null,\"queue\":\"normal\",\"local_user\":\"slurm_user\",\"local_group\":null,\"local_id\":\"53\",\"submit_time\":\"2019-06-07T12:55:23.912772Z\",\"start_time\":\"2019-06-07T12:55:25Z\",\"finish_time\":\"2019-06-07T12:55:25Z\",\"updated_time\":\"2019-06-07T12:55:29.468312Z\",\"eta\":null,\"nodes\":\"c1\",\"cpus\":1,\"exit_code\":0,\"errors\":null,\"resubmit\":1,\"work_dir\":\"/qcg/999\",\"created_work_dir\":true,\"last_seen\":\"2019-06-07T12:55:29.466927Z\"}");
    
    Mockito
        .when(oauth2tokenService.executeWithClientForResult(
            Mockito.any(), Mockito.any(), Mockito.any()))
    	        .thenAnswer(y -> ((ThrowingFunction) y.getArguments()[1]).apply("token"));
//        .thenAnswer(new Answer<Job>() {
//            public Job answer(InvocationOnMock invocation) throws Throwable {
//                return resp1;
//            }
//        });
    

  }
  
  private CloudProviderEndpoint generateCloudProviderEndpoint() {
    return CloudProviderEndpoint
        .builder()
        .iaasType(IaaSType.QCG)
        .cpEndpoint("http://www.example.com/api")
        .cpComputeServiceId("qcg-service-id")
        .password("password")
        .username("username")
        .build();
  }

  /*
  @Test
  public void checkOneDataParamsSubstitutionInTemplate() throws Exception {
    String template = TestUtil.getFileContentAsString(ToscaServiceTest.TEMPLATES_ONEDATA_BASE_DIR
        + "tosca_onedata_requirements.yaml");
    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.setTemplate(template);

    OneData serviceOd = OneData
        .builder()
        .token("service-token")
        .space("service-space")
        .onezone("service-onezone.example.com")
        .path("/service/path/")
        .selectedOneprovider(OneDataProviderInfo
            .builder()
            .endpoint("service-oneprovider.example.com")
            .build())
        .serviceSpace(true)
        .build();
    OneData userOd = OneData
        .builder()
        .token("user-token")
        .onezone("user-onezone.example.com")
        .selectedOneprovider(OneDataProviderInfo
            .builder()
            .endpoint("user-oneprovider-1.example.com")
            .build())
        .build();

    Map<String, OneData> odParameters = ImmutableMap.of(
        "onedata_service_space", serviceOd,
        "onedata_space", userOd);

    ArchiveRoot ar = qcgService.prepareTemplate(deployment, odParameters);

    Map<String, NodeTemplate> nodes = ar.getTopology().getNodeTemplates();
    NodeTemplate qcgJob = nodes.get("qcg_job");
    Map<String, Object> envVars = CommonUtils
        .<ComplexPropertyValue>optionalCast(
            toscaService.getNodePropertyByName(qcgJob, "environment_variables"))
        .get()
        .getValue();

    assertThat(((ScalarPropertyValue) envVars.get("ONEDATA_SPACE_TOKEN")).getValue())
        .isEqualTo(userOd.getToken());
    assertThat(((ScalarPropertyValue) envVars.get("ONEDATA_SPACE_ONEZONE")).getValue())
        .isEqualTo(userOd.getOnezone());
    assertThat(((ScalarPropertyValue) envVars.get("ONEDATA_SPACE_SELECTED_PROVIDER")).getValue())
        .isEqualTo(userOd.getSelectedOneprovider().getEndpoint());
    assertThat(((ScalarPropertyValue) envVars.get("ONEDATA_SERVICE_SPACE_TOKEN")).getValue())
        .isEqualTo(serviceOd.getToken());
    assertThat(((ScalarPropertyValue) envVars.get("ONEDATA_SERVICE_SPACE_ONEZONE")).getValue())
        .isEqualTo(serviceOd.getOnezone());
    assertThat(
        ((ScalarPropertyValue) envVars.get("ONEDATA_SERVICE_SPACE_SELECTED_PROVIDER")).getValue())
        .isEqualTo(serviceOd.getSelectedOneprovider().getEndpoint());
    assertThat(((ScalarPropertyValue) envVars.get("ONEDATA_SERVICE_SPACE_NAME")).getValue())
        .isEqualTo(serviceOd.getSpace());
    assertThat(((ScalarPropertyValue) envVars.get("ONEDATA_SERVICE_SPACE_PATH")).getValue())
        .isEqualTo(serviceOd.getPath());

  }
 */
  
  @Test
  @Parameters({"0,,FRESH", "1,,SUCCESS", "1,fail,FAILURE", "0,fail,FAILURE"})
  public void getLastState(int successCount, String error, JobState expectedState) {
    Job job = new Job();
    job.setResubmit(successCount);
    job.setErrors(error);
    JobState lastState = QcgServiceImpl.getLastState(job);

    assertThat(lastState).isEqualTo(expectedState);
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

    when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    when(topologyIterator.hasNext()).thenReturn(true, !isLast);
    when(topologyIterator.next()).thenReturn(new DeepJob(job,"toscaName"));

    assertThat(qcgService.doDeploy(dm)).isEqualTo(isLast);
    verify(qcg, times(1)).createJob(job.getDescription());
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

    QcgJobsOrderedIterator iterator = mock(QcgJobsOrderedIterator.class);
    dm.setQcgJobsIterator(iterator);

    when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    when(iterator.hasCurrent()).thenReturn(true, true);
    when(iterator.hasNext()).thenReturn(!isLast);
    when(iterator.current()).thenReturn(new DeepJob(job,"toscaName"));

    Job returnedJob = new Job();
    if (isCompleted) {
      returnedJob.setResubmit(1);
    } else {
      returnedJob.setResubmit(0);
      returnedJob.setErrors(null);
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
    job.setResubmit(0);
    job.setErrors("fail");
    when(qcg.getJob("999")).thenReturn(job);

    assertThatCode(
        () -> qcgService.checkJobsOnQcg(generateCloudProviderEndpoint(), null, "999"))
        .isInstanceOf(DeploymentException.class)
        .hasMessage("Qcg job 999 failed to execute");
  }

  @Test
  public void createJobOnQcgSuccessful() throws QcgException {

    Job job = new Job();
    JobDescription description = new JobDescription();
    JobDescriptionExecution execution = new JobDescriptionExecution();
    execution.setExecutable("/usr/bin/printf");
    description.setExecution(execution);
    job.setDescription(description);
    
    /*Job updated =*/ qcgService
    	.createJobOnQcg(generateCloudProviderEndpoint(), null, new DeepJob(job,"toscaName"));
    verify(qcg, times(1)).createJob(job.getDescription());
  }

  @Test
  public void createJobOnQcgFail() throws QcgException {
    Job job = new Job();
    job.setId("999");
    JobDescription description = new JobDescription();
    JobDescriptionExecution execution = new JobDescriptionExecution();
    execution.setExecutable("/usr/bin/printf");
    description.setExecution(execution);
    job.setDescription(description);
    
    doThrow(new QcgException(500, "some message")).when(qcg).createJob(job.getDescription());

    assertThatCode(
        () -> qcgService
        .createJobOnQcg(generateCloudProviderEndpoint(), null, new DeepJob(job,"toscaName")))
        .isInstanceOf(DeploymentException.class)
        .hasCauseExactlyInstanceOf(QcgException.class)
        .hasMessage(
            "Failed to launch job <%s> on Qcg; nested exception is %s (http status: %s)",
            "999", "some message", 500);
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
  @Parameters({"true", "false"})
  public void doUndeploySuccessful(boolean isLast) throws QcgException {
    Deployment deployment = ControllerTestUtils.createDeployment(isLast ? 1 : 2);
    deployment.setEndpoint(isLast ? "999" : "1000");
    DeploymentMessage dm = generateDeployDmQcg(deployment);
    deployment.getResources().forEach(resource -> resource.setToscaNodeType(Nodes.QCG));

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
    QcgServiceData qcgProperties = QcgServiceData
        .qcgBuilder()
        .endpoint("http://www.example.com/api")
        .serviceType(CloudService.QCG_COMPUTE_SERVICE)
        .hostname("www.example.com")
        .providerId("TEST")
        .type(Type.COMPUTE)
        .build();
    when(qcgClientFactory.getFrameworkProperties(dm)).thenReturn(qcgProperties);

    QcgJobsOrderedIterator topologyIterator = qcgService.getJobsTopologicalOrder(
        dm, deployment);
    topologyIterator.next();
    assertThat(objectMapper.writer(SerializationFeature.INDENT_OUTPUT)
            .writeValueAsString(topologyIterator)).isEqualToNormalizingNewlines(TestUtil
                    .getFileContentAsString(ToscaServiceTest.TEMPLATES_BASE_DIR + "qcg_jobs.json"));
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
