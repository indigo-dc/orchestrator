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

package it.reply.orchestrator.service.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.assertj.core.util.Maps;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.tosca.model.ArchiveRoot;

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
import it.reply.orchestrator.service.ToscaService;
import it.reply.orchestrator.util.TestUtil;
import it.reply.orchestrator.utils.WorkflowConstants;

public class PrefilterCloudProvidersTest {

  @InjectMocks
  private PrefilterCloudProviders prefilterCloudProviders;

  @Mock
  private DeploymentRepository deploymentRepository;

  @Mock
  private ToscaService toscaService;

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

    Mockito.when(deploymentRepository.findOne(generateDeployDm.getDeploymentId()))
        .thenReturn(deployment);
    ExecutionResults expectedResult = TestUtil.generateExpectedResult(true);

    ExecutionResults result = prefilterCloudProviders.customExecute(new CommandContext(), rankCloudProvidersMessage);
    TestUtil.assertBaseResults(expectedResult, result);
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
    List<PlacementPolicy> placementPolicies = new ArrayList<>();
    placementPolicies.add(new SlaPlacementPolicy(new ArrayList<String>(), id));
    rankCloudProvidersMessage.setPlacementPolicies(placementPolicies);

    // set cloud provider
    Map<String, CloudService> cloudServices = getCloudServices();
    Map<String, CloudProvider> cloudProviders = getCloudProviders(cloudServices);
    rankCloudProvidersMessage.setCloudProviders(cloudProviders);

    // set one data requirements
    Map<String, OneData> oneDataRequirements = getOneDataRequirements();
    rankCloudProvidersMessage.setOneDataRequirements(oneDataRequirements);

    ArchiveRoot ar = new ArchiveRoot();
    Mockito.when(deploymentRepository.findOne(generateDeployDm.getDeploymentId()))
        .thenReturn(deployment);
    Mockito.when(toscaService.parseTemplate(Mockito.anyString())).thenReturn(ar);
    Mockito
        .when(toscaService.contextualizeImages(Mockito.anyObject(), Mockito.anyObject(),
            Mockito.anyObject()))
        .thenReturn(Maps.newHashMap(Boolean.FALSE, new HashMap<>()));
    ExecutionResults expectedResult = TestUtil.generateExpectedResult(true);

    ExecutionResults result = prefilterCloudProviders.customExecute(new CommandContext(), rankCloudProvidersMessage);
    TestUtil.assertBaseResults(expectedResult, result);

  }

  @Test(expected = OrchestratorException.class)
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
    List<PlacementPolicy> placementPolicies = new ArrayList<>();
    placementPolicies.add(new SlaPlacementPolicy(new ArrayList<String>(), id));
    placementPolicies
        .add(new SlaPlacementPolicy(new ArrayList<String>(), UUID.randomUUID().toString()));

    rankCloudProvidersMessage.setPlacementPolicies(placementPolicies);
    ArchiveRoot ar = new ArchiveRoot();
    Mockito.when(deploymentRepository.findOne(generateDeployDm.getDeploymentId()))
        .thenReturn(deployment);
    Mockito.when(toscaService.parseTemplate(Mockito.anyString())).thenReturn(ar);
    Mockito
        .when(toscaService.contextualizeImages(Mockito.anyObject(), Mockito.anyObject(),
            Mockito.anyObject()))
        .thenReturn(new HashMap<>());
    ExecutionResults expectedResult = TestUtil.generateExpectedResult(true);

    ExecutionResults result = prefilterCloudProviders.customExecute(new CommandContext(), rankCloudProvidersMessage);
    TestUtil.assertBaseResults(expectedResult, result);

  }

  @Test(expected = OrchestratorException.class)
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
    List<PlacementPolicy> placementPolicies = new ArrayList<>();
    // use another id for launch exception
    placementPolicies
        .add(new SlaPlacementPolicy(new ArrayList<String>(), UUID.randomUUID().toString()));

    rankCloudProvidersMessage.setPlacementPolicies(placementPolicies);
    ArchiveRoot ar = new ArchiveRoot();
    Mockito.when(deploymentRepository.findOne(generateDeployDm.getDeploymentId()))
        .thenReturn(deployment);
    Mockito.when(toscaService.parseTemplate(Mockito.anyString())).thenReturn(ar);
    Mockito
        .when(toscaService.contextualizeImages(Mockito.anyObject(), Mockito.anyObject(),
            Mockito.anyObject()))
        .thenReturn(new HashMap<>());
    ExecutionResults expectedResult = TestUtil.generateExpectedResult(true);

    ExecutionResults result = prefilterCloudProviders.customExecute(new CommandContext(), rankCloudProvidersMessage);
    TestUtil.assertBaseResults(expectedResult, result);

  }

  @Test(expected = OrchestratorException.class)
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
    List<PlacementPolicy> placementPolicies = new ArrayList<>();
    placementPolicies.add(getPlacementePolicies());
    rankCloudProvidersMessage.setPlacementPolicies(placementPolicies);

    ArchiveRoot ar = new ArchiveRoot();
    Mockito.when(deploymentRepository.findOne(generateDeployDm.getDeploymentId()))
        .thenReturn(deployment);
    Mockito.when(toscaService.parseTemplate(Mockito.anyString())).thenReturn(ar);
    Mockito
        .when(toscaService.contextualizeImages(Mockito.anyObject(), Mockito.anyObject(),
            Mockito.anyObject()))
        .thenReturn(new HashMap<>());
    ExecutionResults expectedResult = TestUtil.generateExpectedResult(true);

    ExecutionResults result = prefilterCloudProviders.customExecute(new CommandContext(), rankCloudProvidersMessage);
    TestUtil.assertBaseResults(expectedResult, result);

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

    Mockito.when(deploymentRepository.findOne(generateDeployDm.getDeploymentId()))
        .thenReturn(deployment);

    Mockito
        .when(toscaService.contextualizeImages(Mockito.anyObject(), Mockito.anyObject(),
            Mockito.anyObject()))
        .thenReturn(
            Maps.newHashMap(Boolean.FALSE, Maps.newHashMap(new NodeTemplate(), new ImageData())));

    ExecutionResults expectedResult = TestUtil.generateExpectedResult(true);

    ExecutionResults result = prefilterCloudProviders.customExecute(new CommandContext(), rankCloudProvidersMessage);
    TestUtil.assertBaseResults(expectedResult, result);

  }



  private RankCloudProvidersMessage generateRankCloudProvidersMessage(Deployment deployment,
      DeploymentProvider dp) {
    deployment.setDeploymentProvider(dp);
    RankCloudProvidersMessage rankCloudProvidersMessage = new RankCloudProvidersMessage();
    rankCloudProvidersMessage.setDeploymentId(deployment.getId());
    return rankCloudProvidersMessage;
  }

  private SlamPreferences getSlamPreferences(String id) {
    SlamPreferences slamPreferences = new SlamPreferences();
    List<Sla> arrayList = new ArrayList<>();
    Sla sla = new Sla();
    sla.setId(id);
    List<Service> services = new ArrayList<Service>();
    Service service = new Service();
    service.setServiceId(UUID.randomUUID().toString());
    services.add(service);
    sla.setServices(services);
    arrayList.add(sla);
    slamPreferences.setSla(arrayList);
    List<Priority> priorities = getPriorities();
    Preference extPreference = new Preference();
    extPreference.setPreferences(getPreferencesCustomer(priorities));
    List<Preference> preferences = new ArrayList<Preference>();
    preferences.add(extPreference);
    slamPreferences.setPreferences(preferences);
    return slamPreferences;
  }

  private List<Priority> getPriorities() {
    List<Priority> priorities = new ArrayList<Priority>();
    Priority priority = new Priority();
    priority.setServiceId(UUID.randomUUID().toString());
    priorities.add(priority);
    return priorities;
  }

  private List<PreferenceCustomer> getPreferencesCustomer(List<Priority> priorities) {
    PreferenceCustomer preferenceCustomer = new PreferenceCustomer();
    preferenceCustomer.setPriority(priorities);
    List<PreferenceCustomer> preferencesCustomer = new ArrayList<PreferenceCustomer>();
    preferencesCustomer.add(preferenceCustomer);
    return preferencesCustomer;
  }

  private Map<String, CloudProvider> getCloudProviders(Map<String, CloudService> cloudServices) {
    CloudProvider cloudProvider = new CloudProvider("provider-RECAS-BARI");
    cloudProvider.setCmdbProviderServices(cloudServices);
    Map<String, CloudProvider> cloudProviders = new HashMap<>();
    cloudProviders.put("key", cloudProvider);
    return cloudProviders;
  }

  private Map<String, CloudService> getCloudServices() {
    CloudService cloudService = new CloudService();
    CloudServiceData csd = new CloudServiceData();
    csd.setType(Type.COMPUTE);
    csd.setServiceType("eu.egi.cloud.storage-management.oneprovider");
    cloudService.setData(csd);
    Map<String, CloudService> cloudServices = new HashMap<>();
    cloudServices.put("key", cloudService);
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


  // Used for generate excpetion
  private PlacementPolicy getPlacementePolicies() {
    return new PlacementPolicy() {

      private static final long serialVersionUID = -3043392471995029378L;

      @Override
      public List<String> getNodes() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public void setNodes(List<String> nodes) {
        // TODO Auto-generated method stub

      }

    };

  }
}
