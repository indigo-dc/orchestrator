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
import it.reply.orchestrator.exception.service.WorkflowException;
import it.reply.orchestrator.service.CloudProviderEndpointServiceImpl;
import it.reply.orchestrator.service.OneDataService;
import it.reply.orchestrator.service.deployment.providers.DeploymentStatusHelper;
import it.reply.orchestrator.util.TestUtil;
import it.reply.orchestrator.utils.WorkflowConstants;

import org.assertj.core.api.Assertions;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

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

  @Test
  public void testCustomExecuteFail() throws Exception {
    ExecutionEntity execution = new ExecutionEntityBuilder().build();
    Assertions
        .assertThatExceptionOfType(WorkflowException.class)
        .isThrownBy(() -> updateDeployment.execute(execution))
        .withCauseInstanceOf(IllegalArgumentException.class);
  }

  public void baseTestCustomExecuteSuccess(Map<String, OneData> oneDataRequirements)
      throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment();
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    RankCloudProvidersMessage rankCloudProvidersMessage = new RankCloudProvidersMessage();
    rankCloudProvidersMessage.setDeploymentId(deployment.getId());

    CloudProvider cp = new CloudProvider("provider-RECAS-BARI");
    Map<String, CloudProvider> map = new HashMap<>();
    map.put("name", cp);
    rankCloudProvidersMessage.setCloudProviders(map);

    RankedCloudProvider chosenCp = new RankedCloudProvider();
    chosenCp.setName("name");
    dm.setChosenCloudProvider(cp);

    dm.setOneDataRequirements(oneDataRequirements);
    rankCloudProvidersMessage.setOneDataRequirements(oneDataRequirements);

    Mockito.when(cloudProviderEndpointServiceImpl.chooseCloudProvider(Mockito.any(Deployment.class),
        Mockito.any(RankCloudProvidersMessage.class))).thenReturn(chosenCp);
    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    Mockito.doNothing().when(deploymentStatusHelper).updateOnError(Mockito.anyString(),
        Mockito.any(Exception.class));

    Mockito
        .when(cloudProviderEndpointServiceImpl.getCloudProviderEndpoint(cp,
            rankCloudProvidersMessage.getPlacementPolicies(), false))
        .thenReturn(dm.getChosenCloudProviderEndpoint());

    ExecutionEntity execution = new ExecutionEntityBuilder()
        .withMockedVariable(WorkflowConstants.Param.RANK_CLOUD_PROVIDERS_MESSAGE,
            rankCloudProvidersMessage)
        .withMockedVariable(WorkflowConstants.Param.DEPLOYMENT_MESSAGE, dm)
        .build();

    serviceSpaceProperties.setOneproviderUrl(URI.create("http://example.com"));

    updateDeployment.generateOneDataParameters(rankCloudProvidersMessage, dm);

    Assertions
        .assertThatCode(() -> updateDeployment.execute(execution))
        .doesNotThrowAnyException();
    // TODO do some real test here
  }

  @Test
  public void testCustomExecuteSuccess() throws Exception {
    Map<String, OneData> oneDataRequirements = new HashMap<>();
    this.baseTestCustomExecuteSuccess(oneDataRequirements);
  }

  @Test
  public void testCustomExecuteSuccessWithInputData() throws Exception {
    Map<String, OneData> oneDataRequirements = new HashMap<>();
    OneData onedata =
        OneData.builder().token("token").space("space").path("path").providers("providers").build();
    onedata.setSmartScheduling(true);
    oneDataRequirements.put("input", onedata);
    this.baseTestCustomExecuteSuccess(oneDataRequirements);
  }

  @Test
  public void testCustomExecuteSuccessWithOutputData() throws Exception {
    Map<String, OneData> oneDataRequirements = new HashMap<>();
    OneData onedata =
        OneData.builder().token("token").space("space").path("path").providers("providers").build();
    onedata.setSmartScheduling(true);
    oneDataRequirements.put("output", onedata);
    this.baseTestCustomExecuteSuccess(oneDataRequirements);
  }

  @Test
  public void testExecuteSuccessWithInputAndOutputData() throws Exception {
    Map<String, OneData> oneDataRequirements = new HashMap<>();
    OneData onedata =
        OneData.builder().token("token").space("space").path("path").providers("providers").build();
    onedata.setSmartScheduling(true);
    oneDataRequirements.put("input", onedata);
    onedata =
        OneData.builder().token("token").space("space").path("path").providers("providers").build();
    onedata.setSmartScheduling(true);
    oneDataRequirements.put("output", onedata);
    this.baseTestCustomExecuteSuccess(oneDataRequirements);
  }

}
