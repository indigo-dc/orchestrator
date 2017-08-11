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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.drools.core.process.instance.impl.WorkItemImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.kie.api.runtime.process.WorkItem;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import it.reply.orchestrator.config.properties.OneDataProperties;
import it.reply.orchestrator.config.properties.OneDataProperties.ServiceSpaceProperties;
import it.reply.orchestrator.controller.ControllerTestUtils;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.ranker.RankedCloudProvider;
import it.reply.orchestrator.service.CloudProviderEndpointServiceImpl;
import it.reply.orchestrator.service.OneDataService;
import it.reply.orchestrator.service.deployment.providers.DeploymentStatusHelper;
import it.reply.orchestrator.util.TestUtil;
import it.reply.orchestrator.utils.WorkflowConstants;
import it.reply.workflowmanager.utils.Constants;

public class UpdateDeploymentTest {

  @InjectMocks
  @Spy
  UpdateDeployment updateDeployment;

  @Mock
  DeploymentRepository deploymentRepository;

  @Mock
  CloudProviderEndpointServiceImpl cloudProviderEndpointServiceImpl;

  @Mock
  OneDataService oneDataService;
  
  @Spy
  private OneDataProperties oneDataProperties;
  
  @Spy
  private ServiceSpaceProperties serviceSpaceProperties;

  @Mock
  DeploymentStatusHelper deploymentStatusHelper;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    oneDataProperties.setServiceSpace(serviceSpaceProperties);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCustomExecuteFail() throws Exception {
    CommandContext commandContext = new CommandContext();
    WorkItem workItem = new WorkItemImpl();
    commandContext.setData(Constants.WORKITEM, workItem);
    updateDeployment.customExecute(commandContext);
  }


  @Test
  public void testCustomExecuteSuccess() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment();
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);
    CommandContext commandContext = new CommandContext();
    WorkItemImpl workItem = new WorkItemImpl();

    RankCloudProvidersMessage rankCloudProvidersMessage = new RankCloudProvidersMessage();
    rankCloudProvidersMessage.setDeploymentId(deployment.getId());

    CloudProvider cp = new CloudProvider("provider-RECAS-BARI");
    Map<String, CloudProvider> map = new HashMap<>();
    map.put("name", cp);
    rankCloudProvidersMessage.setCloudProviders(map);

    RankedCloudProvider chosenCp = new RankedCloudProvider();
    chosenCp.setName("name");
    dm.setChosenCloudProvider(cp);
    
    Mockito.when(cloudProviderEndpointServiceImpl.chooseCloudProvider(Mockito.any(Deployment.class),
        Mockito.any(RankCloudProvidersMessage.class))).thenReturn(chosenCp);
    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    Mockito.doNothing().when(deploymentStatusHelper).updateOnError(Mockito.anyString(),
        Mockito.any(Exception.class));

    Mockito
        .when(cloudProviderEndpointServiceImpl.getCloudProviderEndpoint(cp,
            rankCloudProvidersMessage.getPlacementPolicies(), false))
        .thenReturn(dm.getChosenCloudProviderEndpoint());
    workItem.setParameter(WorkflowConstants.WF_PARAM_RANK_CLOUD_PROVIDERS_MESSAGE,
        rankCloudProvidersMessage);
    workItem.setParameter(WorkflowConstants.WF_PARAM_DEPLOYMENT_MESSAGE, dm);

    commandContext.setData(Constants.WORKITEM, workItem);

    ExecutionResults expectedResult = new ExecutionResults();
    expectedResult.setData(Constants.RESULT_STATUS, "OK");
    expectedResult.setData(Constants.OK_RESULT, true);
    expectedResult.setData(WorkflowConstants.WF_PARAM_DEPLOYMENT_MESSAGE, dm);
    
    serviceSpaceProperties.setOneproviderUrl(URI.create("http://endpoint.com"));
    
    updateDeployment.generateOneDataParameters(rankCloudProvidersMessage, dm);
    Assert.assertEquals(expectedResult.toString(),
        updateDeployment.customExecute(commandContext).toString());

  }

  @Test
  public void testCustomExecuteSuccessWithInputData() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment();
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);
    CommandContext commandContext = new CommandContext();
    WorkItemImpl workItem = new WorkItemImpl();

    RankCloudProvidersMessage rankCloudProvidersMessage = new RankCloudProvidersMessage();
    rankCloudProvidersMessage.setDeploymentId(deployment.getId());

    CloudProvider cp = new CloudProvider("provider-RECAS-BARI");
    Map<String, CloudProvider> map = new HashMap<>();
    map.put("name", cp);
    rankCloudProvidersMessage.setCloudProviders(map);

    RankedCloudProvider chosenCp = new RankedCloudProvider();
    chosenCp.setName("name");
    dm.setChosenCloudProvider(cp);

    commandContext.setData(Constants.WORKITEM, workItem);

    Map<String, OneData> oneDataRequirements = new HashMap<>();
    OneData onedata =
        OneData.builder().token("token").space("space").path("path").providers("providers").build();
    onedata.setSmartScheduling(true);
    oneDataRequirements.put("input", onedata);
    dm.setOneDataRequirements(oneDataRequirements);
    rankCloudProvidersMessage.setOneDataRequirements(oneDataRequirements);

    workItem.setParameter(WorkflowConstants.WF_PARAM_RANK_CLOUD_PROVIDERS_MESSAGE,
        rankCloudProvidersMessage);
    workItem.setParameter(WorkflowConstants.WF_PARAM_DEPLOYMENT_MESSAGE, dm);
    
    ExecutionResults expectedResult = new ExecutionResults();

    expectedResult = new ExecutionResults();
    expectedResult.setData(Constants.RESULT_STATUS, "OK");
    expectedResult.setData(Constants.OK_RESULT, false);
    expectedResult.setData(WorkflowConstants.WF_PARAM_DEPLOYMENT_MESSAGE, dm);
    updateDeployment.generateOneDataParameters(rankCloudProvidersMessage, dm);
    Assert.assertEquals(expectedResult.toString(),
        updateDeployment.customExecute(commandContext).toString());

  }

  @Test
  public void testCustomExecuteSuccessWithOutputData() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment();
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);
    CommandContext commandContext = new CommandContext();
    WorkItemImpl workItem = new WorkItemImpl();

    RankCloudProvidersMessage rankCloudProvidersMessage = new RankCloudProvidersMessage();
    rankCloudProvidersMessage.setDeploymentId(deployment.getId());

    CloudProvider cp = new CloudProvider("provider-RECAS-BARI");
    Map<String, CloudProvider> map = new HashMap<>();
    map.put("name", cp);
    rankCloudProvidersMessage.setCloudProviders(map);

    RankedCloudProvider chosenCp = new RankedCloudProvider();
    chosenCp.setName("name");
    dm.setChosenCloudProvider(cp);

    commandContext.setData(Constants.WORKITEM, workItem);

    Map<String, OneData> oneDataRequirements = new HashMap<>();
    OneData onedata =
        OneData.builder().token("token").space("space").path("path").providers("providers").build();
    onedata.setSmartScheduling(true);
    oneDataRequirements.put("output", onedata);
    dm.setOneDataRequirements(oneDataRequirements);
    rankCloudProvidersMessage.setOneDataRequirements(oneDataRequirements);

    workItem.setParameter(WorkflowConstants.WF_PARAM_RANK_CLOUD_PROVIDERS_MESSAGE,
        rankCloudProvidersMessage);
    workItem.setParameter(WorkflowConstants.WF_PARAM_DEPLOYMENT_MESSAGE, dm);
    
    ExecutionResults expectedResult = new ExecutionResults();

    expectedResult = new ExecutionResults();
    expectedResult.setData(Constants.RESULT_STATUS, "OK");
    expectedResult.setData(Constants.OK_RESULT, false);
    expectedResult.setData(WorkflowConstants.WF_PARAM_DEPLOYMENT_MESSAGE, dm);
    updateDeployment.generateOneDataParameters(rankCloudProvidersMessage, dm);
    Assert.assertEquals(expectedResult.toString(),
        updateDeployment.customExecute(commandContext).toString());

  }


}
