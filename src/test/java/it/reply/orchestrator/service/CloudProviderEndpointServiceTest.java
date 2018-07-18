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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import it.reply.orchestrator.controller.ControllerTestUtils;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.CloudProviderEndpoint.CloudProviderEndpointBuilder;
import it.reply.orchestrator.dto.CloudProviderEndpoint.IaaSType;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.CloudServiceData;
import it.reply.orchestrator.dto.cmdb.Type;
import it.reply.orchestrator.dto.deployment.PlacementPolicy;
import it.reply.orchestrator.dto.ranker.RankedCloudProvider;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.DeploymentType;
import it.reply.orchestrator.exception.service.DeploymentException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class CloudProviderEndpointServiceTest {

  CloudProviderEndpointServiceImpl cloudProviderEndpointServiceImpl;

  @Before
  public void init() {
    cloudProviderEndpointServiceImpl = new CloudProviderEndpointServiceImpl();
  }

  @Test
  public void chooseCloudProviderSuccesful() {
    Deployment deployment = ControllerTestUtils.createDeployment();
    RankCloudProvidersMessage rcpm = new RankCloudProvidersMessage();

    rcpm.getRankedCloudProviders().add(RankedCloudProvider
        .builder()
        .name("provider1")
        .rank(100)
        .ranked(true)
        .build());
    rcpm.getRankedCloudProviders().add(RankedCloudProvider
        .builder()
        .name("provider2") // the good one
        .rank(400)
        .ranked(true)
        .build());
    rcpm.getRankedCloudProviders().add(RankedCloudProvider
        .builder()
        .name("provider3")
        .rank(800)
        .ranked(false)
        .build());

    RankedCloudProvider chosedProvider =
        cloudProviderEndpointServiceImpl.chooseCloudProvider(deployment, rcpm);
    assertThat(chosedProvider).isNotNull();
    assertThat(chosedProvider.getName()).isEqualTo("provider2");
  }

  @Test
  public void chooseCloudProviderNoneRanked() {
    Deployment deployment = ControllerTestUtils.createDeployment();
    RankCloudProvidersMessage rcpm = new RankCloudProvidersMessage();

    rcpm.getRankedCloudProviders().add(RankedCloudProvider
        .builder()
        .name("provider1")
        .rank(100)
        .ranked(false)
        .build());

    assertThatCode(
        () -> cloudProviderEndpointServiceImpl.chooseCloudProvider(deployment, rcpm))
            .isInstanceOf(DeploymentException.class)
            .hasMessage("No Cloud Provider suitable for deploy found");
  }

  @Test
  public void getCloudProviderEndpointFailBecauseOfNoComputeService() {
    CloudProvider chosenCloudProvider = CloudProvider.builder().id("provider-RECAS-BARI").build();
    Map<String, PlacementPolicy> placementPolicies = new HashMap<>();

    assertThatCode(
        () -> cloudProviderEndpointServiceImpl.getCloudProviderEndpoint(chosenCloudProvider,
            placementPolicies, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                    "No compute Service Available for Cloud Provider : " + chosenCloudProvider);
  }

  @Test
  @Parameters(method = "getCloudProviderEndpointSuccesfulParams")
  public void getCloudProviderEndpointSuccesful(String serviceType, boolean hybrid,
      IaaSType expectedIaaSType, boolean expectedWithImEndpoint) {

    CloudService cloudService = CloudService
        .builder()
        .id(UUID.randomUUID().toString())
        .data(CloudServiceData
            .builder()
            .serviceType(serviceType)
            .endpoint("www.example.com")
            .type(Type.COMPUTE)
            .build())
        .build();

    CloudProvider chosenCloudProvider = CloudProvider.builder().id("provider-RECAS-BARI").build();
    chosenCloudProvider.getCmdbProviderServices().put(cloudService.getId(), cloudService);

    CloudProviderEndpointBuilder expected = CloudProviderEndpoint.builder();
    expected
        .cpEndpoint(cloudService.getData().getEndpoint())
        .cpComputeServiceId(cloudService.getId())
        .iaasType(expectedIaaSType);
    if (hybrid) {
      expected.iaasHeaderId(cloudService.getId());
    } else {
      expected.iaasHeaderId(null);
    }
    if (expectedWithImEndpoint) {
      expected.imEndpoint(cloudService.getData().getEndpoint());
    } else {
      expected.imEndpoint(null);
    }

    CloudProviderEndpoint actual = cloudProviderEndpointServiceImpl
        .getCloudProviderEndpoint(chosenCloudProvider, new HashMap<>(), hybrid);
    assertThat(expected.build()).isEqualTo(actual);
  }

  public Object[] getCloudProviderEndpointSuccesfulParams() {
    return new Object[] {
        new Object[] { "com.amazonaws.ec2", false, IaaSType.AWS, false },
        new Object[] { "com.amazonaws.ec2", true, IaaSType.AWS, false },
        new Object[] { "com.microsoft.azure", false, IaaSType.AZURE, false },
        new Object[] { "com.microsoft.azure", true, IaaSType.AZURE, false },
        new Object[] { "eu.egi.cloud.vm-management.occi", false, IaaSType.OCCI, false },
        new Object[] { "eu.egi.cloud.vm-management.occi", true, IaaSType.OCCI, false },
        new Object[] { "eu.egi.cloud.vm-management.opennebula", false, IaaSType.OPENNEBULA, false },
        new Object[] { "eu.egi.cloud.vm-management.opennebula", true, IaaSType.OPENNEBULA, false },
        new Object[] { "eu.indigo-datacloud.im-tosca.opennebula", false, IaaSType.OPENNEBULA,
            true },
        new Object[] { "eu.indigo-datacloud.im-tosca.opennebula", true, IaaSType.OPENNEBULA,
            false },
        new Object[] { "eu.egi.cloud.vm-management.openstack", false, IaaSType.OPENSTACK, false },
        new Object[] { "eu.egi.cloud.vm-management.openstack", true, IaaSType.OPENSTACK, false }
    };
  }

  @Test
  public void getCloudProviderEndpointUnknownServiceType() {
    CloudService cloudService = CloudService
        .builder()
        .id(UUID.randomUUID().toString())
        .data(CloudServiceData
            .builder()
            .serviceType("unknownType")
            .endpoint("www.example.com")
            .type(Type.COMPUTE)
            .build())
        .build();

    CloudProvider chosenCloudProvider = CloudProvider.builder().id("provider-RECAS-BARI").build();
    chosenCloudProvider.getCmdbProviderServices().put(cloudService.getId(), cloudService);

    assertThatCode(() -> cloudProviderEndpointServiceImpl
        .getCloudProviderEndpoint(chosenCloudProvider, new HashMap<>(), false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unknown Cloud Provider type: " + cloudService);
  }

  @Test
  @Parameters({ "CHRONOS|CHRONOS", "MARATHON|MARATHON", "TOSCA|IM" })
  public void getDeploymentProviderSuccesful(DeploymentType deploymentType,
      DeploymentProvider expectedDeploymentProvider) {
    assertThat(cloudProviderEndpointServiceImpl.getDeploymentProvider(deploymentType, null))
        .isEqualTo(expectedDeploymentProvider);
  }
}
