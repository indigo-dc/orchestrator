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

package it.reply.orchestrator.service;

import com.google.common.collect.ImmutableMap;

import it.reply.orchestrator.controller.ControllerTestUtils;
//import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.CloudProviderEndpoint.CloudProviderEndpointBuilder;
import it.reply.orchestrator.dto.CloudProviderEndpoint.IaaSType;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.CloudServiceData;
import it.reply.orchestrator.dto.cmdb.Type;
import it.reply.orchestrator.dto.policies.ToscaPolicy;
import it.reply.orchestrator.dto.ranker.RankedCloudProvider;
import it.reply.orchestrator.dto.workflow.CloudProvidersOrderedIterator;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.DeploymentType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@RunWith(JUnitParamsRunner.class)
public class CloudProviderEndpointServiceTest {

  CloudProviderEndpointServiceImpl cloudProviderEndpointServiceImpl;

  @Before
  public void init() {
    cloudProviderEndpointServiceImpl = new CloudProviderEndpointServiceImpl();
  }

  private Map<String, CloudProvider> generateCloudProviders() {
    return ImmutableMap.of(
        "provider1", CloudProvider.builder().id("provider1").build(),
        "provider2", CloudProvider.builder().id("provider2").build(),
        "provider3", CloudProvider.builder().id("provider3").build());
  }

  @Parameters({"null", "1", "2", "3"})
  @Test
  public void chooseCloudProviderSuccesful(@Nullable Integer maxCpRetries) {
    /*Deployment deployment =*/ ControllerTestUtils.createDeployment();
    RankCloudProvidersMessage rcpm = new RankCloudProvidersMessage();
    rcpm.setCloudProviders(generateCloudProviders());
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

    CloudProvidersOrderedIterator providersOrderedIterator =
        cloudProviderEndpointServiceImpl.generateCloudProvidersOrderedIterator(rcpm, maxCpRetries);

    if (maxCpRetries == null) {
      maxCpRetries = Integer.MAX_VALUE;
    }
    assertThat(providersOrderedIterator.getSize()).isEqualTo(Math.min(2, maxCpRetries));
    assertThat(providersOrderedIterator.next().getId()).isEqualTo("provider2");
    if (maxCpRetries > 1) {
      assertThat(providersOrderedIterator.next().getId()).isEqualTo("provider1");
    }
  }

  @Test
  @Parameters({"MARATHON|1", "CHRONOS|1", "TOSCA|2"})
  public void chooseCloudProviderSuccesfulByDeploymentType(DeploymentType deploymentType,
      int expectedSized) {
    /*Deployment deployment =*/ ControllerTestUtils.createDeployment();
    RankCloudProvidersMessage rcpm = new RankCloudProvidersMessage();
    rcpm.setDeploymentType(deploymentType);
    rcpm.setCloudProviders(generateCloudProviders());
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

    CloudProvidersOrderedIterator providersOrderedIterator =
        cloudProviderEndpointServiceImpl.generateCloudProvidersOrderedIterator(rcpm, null);
    assertThat(providersOrderedIterator.getSize()).isEqualTo(expectedSized);
  }

  @Test
  public void chooseCloudProviderNoneRanked() {
    /*Deployment deployment =*/ ControllerTestUtils.createDeployment();
    RankCloudProvidersMessage rcpm = new RankCloudProvidersMessage();
    rcpm.setCloudProviders(generateCloudProviders());

    rcpm.getRankedCloudProviders().add(RankedCloudProvider
        .builder()
        .name("provider1")
        .rank(100)
        .ranked(false)
        .build());
    CloudProvidersOrderedIterator providersOrderedIterator = cloudProviderEndpointServiceImpl
        .generateCloudProvidersOrderedIterator(rcpm, null);
    assertThat(providersOrderedIterator).isEmpty();

  }

  @Test
  public void getCloudProviderEndpointFailBecauseOfNoComputeService() {
    CloudProvider chosenCloudProvider = CloudProvider.builder().id("provider-RECAS-BARI").build();
    Map<String, ToscaPolicy> placementPolicies = new HashMap<>();

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
            .providerId("provider-RECAS-BARI")
            .type(Type.COMPUTE)
            .hostname("example.com")
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
    return new Object[]{
        new Object[]{"com.amazonaws.ec2", false, IaaSType.AWS, false},
        new Object[]{"com.amazonaws.ec2", true, IaaSType.AWS, false},
        new Object[]{"com.microsoft.azure", false, IaaSType.AZURE, false},
        new Object[]{"com.microsoft.azure", true, IaaSType.AZURE, false},
        new Object[]{"eu.otc.compute", false, IaaSType.OTC, false},
        new Object[]{"eu.otc.compute", true, IaaSType.OTC, false},
        new Object[]{"eu.egi.cloud.vm-management.occi", false, IaaSType.OCCI, false},
        new Object[]{"eu.egi.cloud.vm-management.occi", true, IaaSType.OCCI, false},
        new Object[]{"eu.egi.cloud.vm-management.opennebula", false, IaaSType.OPENNEBULA, false},
        new Object[]{"eu.egi.cloud.vm-management.opennebula", true, IaaSType.OPENNEBULA, false},
        new Object[]{"eu.indigo-datacloud.im-tosca.opennebula", false, IaaSType.OPENNEBULA, true},
        new Object[]{"eu.indigo-datacloud.im-tosca.opennebula", true, IaaSType.OPENNEBULA, false},
        new Object[]{"eu.egi.cloud.vm-management.openstack", false, IaaSType.OPENSTACK, false},
        new Object[]{"eu.egi.cloud.vm-management.openstack", true, IaaSType.OPENSTACK, false},
        new Object[]{"eu.indigo-datacloud.marathon", false, IaaSType.MARATHON, false},
        new Object[]{"eu.indigo-datacloud.chronos", false, IaaSType.CHRONOS, false},
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
            .providerId("provider-RECAS-BARI")
            .type(Type.COMPUTE)
            .hostname("example.com")
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
