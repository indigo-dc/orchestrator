/*
 * Copyright © 2015-2019 Santer Reply S.p.A.
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import it.reply.orchestrator.config.properties.OneDataProperties;
import it.reply.orchestrator.config.properties.OneDataProperties.ServiceSpaceProperties;
import it.reply.orchestrator.controller.ControllerTestUtils;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.CloudProvider;
import it.reply.orchestrator.dto.cmdb.CloudServiceType;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.onedata.OneData.OneDataProviderInfo;
import it.reply.orchestrator.dto.workflow.CloudServicesOrderedIterator;
import it.reply.orchestrator.exception.service.WorkflowException;
import it.reply.orchestrator.service.CloudProviderEndpointServiceImpl;
import it.reply.orchestrator.service.OneDataService;
import it.reply.orchestrator.service.deployment.providers.DeploymentStatusHelper;
import it.reply.orchestrator.util.TestUtil;
import it.reply.orchestrator.utils.WorkflowConstants;

import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class UpdateDeploymentTest extends BaseDeployCommandTest<UpdateDeployment> {

  @Mock
  private DeploymentRepository deploymentRepository;

  @Mock
  private CloudProviderEndpointServiceImpl cloudProviderEndpointServiceImpl;

  @Mock
  private OneDataService oneDataService;

  @Spy
  private OneDataProperties oneDataProperties;

  @Spy
  private ServiceSpaceProperties serviceSpaceProperties;

  @Mock
  private DeploymentStatusHelper deploymentStatusHelper;

  public UpdateDeploymentTest() {
    super(new UpdateDeployment());
  }

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    oneDataProperties.setServiceSpace(serviceSpaceProperties);
  }

  @Test
  public void testCustomExecuteFail() {
    ExecutionEntity execution = new ExecutionEntityBuilder().build();
    assertThatExceptionOfType(WorkflowException.class)
        .isThrownBy(() -> command.execute(execution))
        .withCauseInstanceOf(IllegalArgumentException.class);
  }

  public DeploymentMessage baseTestCustomExecuteSuccess(Map<String, OneData> oneDataRequirements) {
    Deployment deployment = ControllerTestUtils.createDeployment();
    DeploymentMessage dm = TestUtil.generateDeployDm(deployment);

    RankCloudProvidersMessage rankCloudProvidersMessage = new RankCloudProvidersMessage();
    rankCloudProvidersMessage.setDeploymentId(deployment.getId());

    CloudService cs = CloudService
        .builder()
        .endpoint("http://example.com")
        .providerId("cloud-provider-id-1")
        .id("cloud-service-id-1")
        .type(CloudServiceType.COMPUTE)
        .endpoint("http://example.com")
        .serviceType("unknown")
        .hostname("example.com")
        .build();
    CloudProvider cp = CloudProvider
        .builder()
        .id("cloud-provider-id-1")
        .name("cloud-provider-name-1")
        .services(ImmutableMap.of("cloud-service-id-1", cs))
        .build();

    Map<String, CloudProvider> map = new HashMap<>();
    map.put(cp.getId(), cp);
    rankCloudProvidersMessage.setCloudProviders(map);

    dm.setOneDataRequirements(oneDataRequirements);
    rankCloudProvidersMessage.setOneDataRequirements(oneDataRequirements);

    when(cloudProviderEndpointServiceImpl
        .generateCloudProvidersOrderedIterator(rankCloudProvidersMessage, null))
        .thenReturn(new CloudServicesOrderedIterator(Lists.newArrayList(cs)));
    when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);
    doNothing()
        .when(deploymentStatusHelper)
        .updateOnError(anyString(), any(Exception.class));

    CloudProviderEndpoint chosenCloudProviderEndpoint = dm.getChosenCloudProviderEndpoint();
    dm.setChosenCloudProviderEndpoint(null);
    when(cloudProviderEndpointServiceImpl.getCloudProviderEndpoint(cs,
        rankCloudProvidersMessage.getPlacementPolicies(), false))
        .thenReturn(chosenCloudProviderEndpoint);

    ExecutionEntity execution = new ExecutionEntityBuilder()
        .withMockedVariable(WorkflowConstants.Param.RANK_CLOUD_PROVIDERS_MESSAGE,
            rankCloudProvidersMessage)
        .withMockedVariable(WorkflowConstants.Param.DEPLOYMENT_MESSAGE, dm)
        .build();

    command.execute(execution);
    return dm;
  }

  @Test
  public void testCustomExecuteSuccess() {
    DeploymentMessage dm = baseTestCustomExecuteSuccess(new HashMap<>());
    assertThat(dm.getCloudServicesOrderedIterator().getSize())
        .isEqualTo(1);
    assertThat(dm.getChosenCloudProviderEndpoint()).isNotNull();
  }

  @Test
  public void testExecuteSuccessWithOneData() throws Exception {
    Map<String, OneData> oneDataRequirements = new HashMap<>();
    OneData onedata = OneData
        .builder()
        .token("token")
        .space("space")
        .path("path")
        .smartScheduling(true)
        .oneproviders(Lists.newArrayList(
            OneDataProviderInfo.builder()
                .endpoint("2.example.com")
                .cloudProviderId("cloud-provider-id-2")
                .cloudServiceId("cloud-service-id-2")
                .id("oneprovider-id-2")
                .build(),
            OneDataProviderInfo.builder()
                .endpoint("1.example.com")
                .cloudProviderId("cloud-provider-id-1")
                .cloudServiceId("cloud-service-id-1")
                .id("oneprovider-id-1")
                .build()))
        .build();
    oneDataRequirements.put("space", onedata);

    Map<String, OneData> oneDataParameters = baseTestCustomExecuteSuccess(oneDataRequirements)
        .getOneDataParameters();
    assertThat(oneDataParameters).size().isEqualTo(1);
    assertThat(oneDataParameters.get("space").getSelectedOneprovider().getEndpoint())
        .isEqualTo("1.example.com");
  }

}
