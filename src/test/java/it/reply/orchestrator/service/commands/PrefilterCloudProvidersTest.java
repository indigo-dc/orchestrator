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

package it.reply.orchestrator.service.commands;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.tosca.model.ArchiveRoot;

import com.google.common.collect.Lists;

import it.reply.orchestrator.controller.ControllerTestUtils;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.CloudServiceData;
import it.reply.orchestrator.dto.cmdb.ImageData;
import it.reply.orchestrator.dto.cmdb.Type;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.deployment.PlacementPolicy;
import it.reply.orchestrator.dto.deployment.SlaPlacementPolicy;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.slam.Preference;
import it.reply.orchestrator.dto.slam.PreferenceCustomer;
import it.reply.orchestrator.dto.slam.Priority;
import it.reply.orchestrator.dto.slam.Service;
import it.reply.orchestrator.dto.slam.Sla;
import it.reply.orchestrator.dto.slam.SlamPreferences;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.exception.service.WorkflowException;
import it.reply.orchestrator.service.ToscaService;
import it.reply.orchestrator.util.TestUtil;
import it.reply.orchestrator.utils.WorkflowConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.assertj.core.util.Maps;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class PrefilterCloudProvidersTest extends
    BaseRankCloudProvidersCommandTest<PrefilterCloudProviders> {

  @Mock
  private DeploymentRepository deploymentRepository;

  @Mock
  private ToscaService toscaService;

  public PrefilterCloudProvidersTest() {
    super(new PrefilterCloudProviders());
  }

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testBasicCustomExecuteSuccess() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment();
    DeploymentMessage generateDeployDm = TestUtil.generateDeployDm(deployment);
    deployment.setDeploymentProvider(DeploymentProvider.HEAT);
    RankCloudProvidersMessage rankCloudProvidersMessage = new RankCloudProvidersMessage();
    rankCloudProvidersMessage.setDeploymentId(generateDeployDm.getDeploymentId());

    when(deploymentRepository.findOne(generateDeployDm.getDeploymentId()))
        .thenReturn(deployment);
    
    ExecutionEntity execution = new ExecutionEntityBuilder()
        .withMockedVariable(WorkflowConstants.Param.RANK_CLOUD_PROVIDERS_MESSAGE, rankCloudProvidersMessage)
        .build();

    assertThatCode(() -> command.execute(execution))
        .doesNotThrowAnyException();
  }


  @Test
  public void testCustomExecuteSuccess() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment();
    DeploymentMessage generateDeployDm = TestUtil.generateDeployDm(deployment);
    String id = UUID.randomUUID().toString();
    RankCloudProvidersMessage rankCloudProvidersMessage =
        generateRankCloudProvidersMessage(deployment, DeploymentProvider.CHRONOS);

    // set slam preferences
    SlamPreferences slamPreferences = getSlamPreferences(id);
    rankCloudProvidersMessage.setSlamPreferences(slamPreferences);

    // set placement policies
    Map<String, PlacementPolicy> placementPolicies = new HashMap<>();
    placementPolicies.put("policy", new SlaPlacementPolicy(new ArrayList<>(), id));
    rankCloudProvidersMessage.setPlacementPolicies(placementPolicies);

    // set cloud provider
    Map<String, CloudService> cloudServices = getCloudServices();
    Map<String, CloudProvider> cloudProviders = getCloudProviders(cloudServices);
    rankCloudProvidersMessage.setCloudProviders(cloudProviders);

    // set one data requirements
    Map<String, OneData> oneDataRequirements = getOneDataRequirements();
    rankCloudProvidersMessage.setOneDataRequirements(oneDataRequirements);

    ArchiveRoot ar = new ArchiveRoot();
    when(deploymentRepository.findOne(generateDeployDm.getDeploymentId()))
        .thenReturn(deployment);
    when(toscaService.parseTemplate(Mockito.anyString())).thenReturn(ar);
    when(toscaService.contextualizeImages(Mockito.anyObject(), Mockito.anyObject(),
            Mockito.anyObject()))
        .thenReturn(Maps.newHashMap(Boolean.FALSE, new HashMap<>()));

    ExecutionEntity execution = new ExecutionEntityBuilder()
        .withMockedVariable(WorkflowConstants.Param.RANK_CLOUD_PROVIDERS_MESSAGE, rankCloudProvidersMessage)
        .build();

    assertThatCode(() -> command.execute(execution))
        .doesNotThrowAnyException();
  }

  @Test
  public void testCustomExecuteOrchestratorExceptionNoSinglePlacement() throws Exception {
    String id = UUID.randomUUID().toString();
    Deployment deployment = ControllerTestUtils.createDeployment();
    DeploymentMessage generateDeployDm = TestUtil.generateDeployDm(deployment);
    RankCloudProvidersMessage rankCloudProvidersMessage =
        generateRankCloudProvidersMessage(deployment, DeploymentProvider.CHRONOS);

    // set slam preferences
    SlamPreferences slamPreferences = getSlamPreferences(id);
    rankCloudProvidersMessage.setSlamPreferences(slamPreferences);


    // set placementPolicies
    Map<String, PlacementPolicy> placementPolicies = new HashMap<>();
    placementPolicies.put("policy_1", new SlaPlacementPolicy(new ArrayList<String>(), id));
    placementPolicies.put("policy_2",
        new SlaPlacementPolicy(new ArrayList<String>(), UUID.randomUUID().toString()));

    rankCloudProvidersMessage.setPlacementPolicies(placementPolicies);
    ArchiveRoot ar = new ArchiveRoot();
    when(deploymentRepository.findOne(generateDeployDm.getDeploymentId()))
        .thenReturn(deployment);
    when(toscaService.parseTemplate(Mockito.anyString())).thenReturn(ar);
    when(toscaService.contextualizeImages(Mockito.anyObject(), Mockito.anyObject(),
            Mockito.anyObject()))
        .thenReturn(new HashMap<>());

    ExecutionEntity execution = new ExecutionEntityBuilder()
        .withMockedVariable(WorkflowConstants.Param.RANK_CLOUD_PROVIDERS_MESSAGE, rankCloudProvidersMessage)
        .build();

    assertThatThrownBy(() -> command.execute(execution))
        .isInstanceOf(WorkflowException.class)
        .hasCauseInstanceOf(OrchestratorException.class)
        .hasMessage("Error filtering Cloud Providers; nested exception is it.reply.orchestrator.exception.OrchestratorException: Only a single placement policy is supported");
  }

  @Test
  public void testCustomExecuteOrchestratorExceptioNoSLAWithId() throws Exception {
    String id = UUID.randomUUID().toString();
    Deployment deployment = ControllerTestUtils.createDeployment();
    DeploymentMessage generateDeployDm = TestUtil.generateDeployDm(deployment);
    RankCloudProvidersMessage rankCloudProvidersMessage =
        generateRankCloudProvidersMessage(deployment, DeploymentProvider.CHRONOS);

    // set slam preferences
    SlamPreferences slamPreferences = getSlamPreferences(id);
    rankCloudProvidersMessage.setSlamPreferences(slamPreferences);

    // set placement policies
    Map<String, PlacementPolicy> placementPolicies = new HashMap<>();
    // use another id for launch exception
    String slaId = UUID.randomUUID().toString();
    placementPolicies.put("policy",
        new SlaPlacementPolicy(new ArrayList<String>(), slaId));

    rankCloudProvidersMessage.setPlacementPolicies(placementPolicies);
    ArchiveRoot ar = new ArchiveRoot();
    when(deploymentRepository.findOne(generateDeployDm.getDeploymentId()))
        .thenReturn(deployment);
    when(toscaService.parseTemplate(Mockito.anyString())).thenReturn(ar);
    when(toscaService.contextualizeImages(Mockito.anyObject(), Mockito.anyObject(),
            Mockito.anyObject()))
        .thenReturn(new HashMap<>());

    ExecutionEntity execution = new ExecutionEntityBuilder()
        .withMockedVariable(WorkflowConstants.Param.RANK_CLOUD_PROVIDERS_MESSAGE, rankCloudProvidersMessage)
        .build();

    assertThatThrownBy(() -> command.execute(execution))
        .isInstanceOf(WorkflowException.class)
        .hasCauseInstanceOf(OrchestratorException.class)
        .hasMessage("Error filtering Cloud Providers; nested exception is it.reply.orchestrator.exception.OrchestratorException: No SLA with id " + slaId + " available");
  }

  @Test
  public void testCustomExecuteOrchestratorExceptioNoSLAPlacement() throws Exception {
    String id = UUID.randomUUID().toString();
    Deployment deployment = ControllerTestUtils.createDeployment();
    DeploymentMessage generateDeployDm = TestUtil.generateDeployDm(deployment);
    RankCloudProvidersMessage rankCloudProvidersMessage =
        generateRankCloudProvidersMessage(deployment, DeploymentProvider.CHRONOS);

    // set slam preferences
    SlamPreferences slamPreferences = getSlamPreferences(id);
    rankCloudProvidersMessage.setSlamPreferences(slamPreferences);

    // set placement policies
    Map<String, PlacementPolicy> placementPolicies = new HashMap<>();
    placementPolicies.put("policy", Mockito.mock(PlacementPolicy.class));
    rankCloudProvidersMessage.setPlacementPolicies(placementPolicies);

    ArchiveRoot ar = new ArchiveRoot();
    when(deploymentRepository.findOne(generateDeployDm.getDeploymentId()))
        .thenReturn(deployment);
    when(toscaService.parseTemplate(Mockito.anyString())).thenReturn(ar);
    when(toscaService.contextualizeImages(Mockito.anyObject(), Mockito.anyObject(),
            Mockito.anyObject()))
        .thenReturn(new HashMap<>());

    ExecutionEntity execution = new ExecutionEntityBuilder()
        .withMockedVariable(WorkflowConstants.Param.RANK_CLOUD_PROVIDERS_MESSAGE, rankCloudProvidersMessage)
        .build();

    assertThatThrownBy(() -> command.execute(execution))
        .isInstanceOf(WorkflowException.class)
        .hasCauseInstanceOf(OrchestratorException.class)
        .hasMessage("Error filtering Cloud Providers; nested exception is it.reply.orchestrator.exception.OrchestratorException: Only SLA placement policies are supported");
  }

  @Test
  public void testCustomExecuteRemoveCloudService() throws Exception {
    String id = UUID.randomUUID().toString();
    Deployment deployment = ControllerTestUtils.createDeployment();
    DeploymentMessage generateDeployDm = TestUtil.generateDeployDm(deployment);
    RankCloudProvidersMessage rankCloudProvidersMessage =
        generateRankCloudProvidersMessage(deployment, DeploymentProvider.HEAT);

    // set slam preferences
    SlamPreferences slamPreferences = getSlamPreferences(id);
    rankCloudProvidersMessage.setSlamPreferences(slamPreferences);

    // set cloud providers
    Map<String, CloudService> cloudServices = getCloudServices();
    Map<String, CloudProvider> cloudProviders = getCloudProviders(cloudServices);
    rankCloudProvidersMessage.setCloudProviders(cloudProviders);

    when(deploymentRepository.findOne(generateDeployDm.getDeploymentId()))
        .thenReturn(deployment);

    when(toscaService.contextualizeImages(Mockito.anyObject(), Mockito.anyObject(),
            Mockito.anyObject()))
        .thenReturn(
            Maps.newHashMap(Boolean.FALSE,
                Maps.newHashMap(new NodeTemplate(), ImageData.builder().build())));

    ExecutionEntity execution = new ExecutionEntityBuilder()
        .withMockedVariable(WorkflowConstants.Param.RANK_CLOUD_PROVIDERS_MESSAGE, rankCloudProvidersMessage)
        .build();

    assertThatCode(() -> command.execute(execution))
        .doesNotThrowAnyException();
  }



  private RankCloudProvidersMessage generateRankCloudProvidersMessage(Deployment deployment,
      DeploymentProvider dp) {
    deployment.setDeploymentProvider(dp);
    RankCloudProvidersMessage rankCloudProvidersMessage = new RankCloudProvidersMessage();
    rankCloudProvidersMessage.setDeploymentId(deployment.getId());
    return rankCloudProvidersMessage;
  }

  private SlamPreferences getSlamPreferences(String id) {
    return SlamPreferences
        .builder()
        .preferences(Lists
            .newArrayList(Preference
                .builder()
                .preferences(Lists
                    .newArrayList(PreferenceCustomer
                        .builder()
                        .priority(Lists
                            .newArrayList(Priority
                                .builder()
                                .serviceId(UUID.randomUUID().toString())
                                .build()))
                        .build()))
                .build()))
        .sla(Lists
            .newArrayList(Sla
                .builder()
                .id(id)
                .services(Lists
                    .newArrayList(Service
                        .builder()
                        .serviceId(UUID.randomUUID().toString())
                        .build()))
                .build()))
        .build();
  }

  private Map<String, CloudProvider> getCloudProviders(Map<String, CloudService> cloudServices) {
    CloudProvider cloudProvider = CloudProvider
        .builder()
        .id("provider-RECAS-BARI")
        .cmdbProviderServices(cloudServices)
        .build();
    Map<String, CloudProvider> cloudProviders = new HashMap<>();
    cloudProviders.put(cloudProvider.getId(), cloudProvider);
    return cloudProviders;
  }

  private Map<String, CloudService> getCloudServices() {
    CloudService cloudService = CloudService
        .builder()
        .id("provider-RECAS-BARI")
        .data(CloudServiceData
            .builder()
            .type(Type.COMPUTE)
            .endpoint("http://example.com")
            .providerId("providerId")
            .serviceType(CloudService.ONEPROVIDER_STORAGE_SERVICE)
            .build())
        .build();
    Map<String, CloudService> cloudServices = new HashMap<>();
    cloudServices.put(cloudService.getId(), cloudService);
    return cloudServices;
  }

  private Map<String, OneData> getOneDataRequirements() {
    Map<String, OneData> oneDataRequirements = new HashMap<>();
    OneData onedataInput = OneData.builder()
        .token("token")
        .space("space")
        .path("path")
        .providers("providers")
        .smartScheduling(true)
        .build();
    OneData onedataOutput = OneData.builder()
        .token("token")
        .space("space")
        .path("path")
        .providers("providers")
        .smartScheduling(true)
        .build();
    oneDataRequirements.put("input", onedataInput);
    oneDataRequirements.put("output", onedataOutput);
    return oneDataRequirements;
  }

}
