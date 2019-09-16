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

import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.CloudProviderEndpoint.CloudProviderEndpointBuilder;
import it.reply.orchestrator.dto.CloudProviderEndpoint.IaaSType;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.CloudProvider;
import it.reply.orchestrator.dto.cmdb.CloudServiceType;
import it.reply.orchestrator.dto.ranker.RankedCloudService;
import it.reply.orchestrator.dto.workflow.CloudServicesOrderedIterator;
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
        "provider-1", CloudProvider
            .builder()
            .id("provider-1")
            .name("provider-1")
            .services(ImmutableMap.of(
                "provider-1-service-1", CloudService
                    .builder()
                    .id("provider-1-service-1")
                    .serviceType("unknownType")
                    .endpoint("www.example.com")
                    .providerId("provider-1")
                    .type(CloudServiceType.COMPUTE)
                    .hostname("example.com")
                    .build(),
                "provider-1-service-2", CloudService
                    .builder()
                    .id("provider-1-service-2")
                    .serviceType("unknownType")
                    .endpoint("www.example.com")
                    .providerId("provider-1")
                    .type(CloudServiceType.COMPUTE)
                    .hostname("example.com")
                    .build())
            ).build(),
        "provider-2", CloudProvider
            .builder()
            .id("provider-2")
            .name("provider-2")
            .services(ImmutableMap.of(
                "provider-2-service-1", CloudService
                    .builder()
                    .id("provider-2-service-1")
                    .serviceType("unknownType")
                    .endpoint("www.example.com")
                    .providerId("provider-1")
                    .type(CloudServiceType.COMPUTE)
                    .hostname("example.com")
                    .build(),
                "provider-2-service-2", CloudService
                    .builder()
                    .id("provider-2-service-2")
                    .serviceType("unknownType")
                    .endpoint("www.example.com")
                    .providerId("provider-1")
                    .type(CloudServiceType.COMPUTE)
                    .hostname("example.com")
                    .build())
            ).build()
    );
  }

  @Parameters({"null", "1", "2", "3"})
  @Test
  public void chooseCloudProviderSuccessfull(@Nullable Integer maxCpRetries) {
    RankCloudProvidersMessage rcpm = new RankCloudProvidersMessage();
    rcpm.setCloudProviders(generateCloudProviders());
    rcpm.getRankedCloudServices().add(RankedCloudService
        .builder()
        .provider("provider-1")
        .serviceId("provider-1-service-1")
        .rank(100)
        .ranked(true)
        .build());
    rcpm.getRankedCloudServices().add(RankedCloudService
        .builder()
        .provider("provider-1")
        .serviceId("provider-1-service-2")
        .rank(400)
        .ranked(true)
        .build());
    rcpm.getRankedCloudServices().add(RankedCloudService
        .builder()
        .provider("provider-2")
        .serviceId("provider-2-service-1")
        .rank(800)
        .ranked(false)
        .build());
    rcpm.getRankedCloudServices().add(RankedCloudService
        .builder()
        .provider("provider-2")
        .serviceId("provider-2-service-2")
        .rank(200)
        .ranked(true)
        .build());

    CloudServicesOrderedIterator providersOrderedIterator =
        cloudProviderEndpointServiceImpl.generateCloudProvidersOrderedIterator(rcpm, maxCpRetries);

    if (maxCpRetries == null) {
      maxCpRetries = Integer.MAX_VALUE;
    }
    assertThat(providersOrderedIterator.getSize()).isEqualTo(Math.min(3, maxCpRetries));
    assertThat(providersOrderedIterator.next().getCloudService().getId())
        .isEqualTo("provider-1-service-2");
    if (maxCpRetries > 1) {
      assertThat(providersOrderedIterator.next().getCloudService().getId())
          .isEqualTo("provider-2-service-2");
    }
  }

  @Test
  @Parameters({"MARATHON|1", "CHRONOS|1", "TOSCA|2"})
  public void chooseCloudProviderSuccesfulByDeploymentType(DeploymentType deploymentType,
      int expectedSized) {
    RankCloudProvidersMessage rcpm = new RankCloudProvidersMessage();
    rcpm.setDeploymentType(deploymentType);
    rcpm.setCloudProviders(generateCloudProviders());
    rcpm.getRankedCloudServices().add(RankedCloudService
        .builder()
        .provider("provider-1")
        .serviceId("provider-1-service-1")
        .rank(100)
        .ranked(true)
        .build());
    rcpm.getRankedCloudServices().add(RankedCloudService
        .builder()
        .provider("provider-1")
        .serviceId("provider-1-service-2")
        .rank(400)
        .ranked(true)
        .build());
    rcpm.getRankedCloudServices().add(RankedCloudService
        .builder()
        .provider("provider-2")
        .serviceId("provider-2-service-1")
        .rank(800)
        .ranked(false)
        .build());
    rcpm.getRankedCloudServices().add(RankedCloudService
        .builder()
        .provider("provider-2")
        .serviceId("provider-2-service-2")
        .rank(800)
        .ranked(false)
        .build());

    CloudServicesOrderedIterator providersOrderedIterator =
        cloudProviderEndpointServiceImpl.generateCloudProvidersOrderedIterator(rcpm, null);
    assertThat(providersOrderedIterator.getSize()).isEqualTo(expectedSized);
  }

  @Test
  public void chooseCloudProviderNoneRanked() {
    RankCloudProvidersMessage rcpm = new RankCloudProvidersMessage();
    rcpm.setCloudProviders(generateCloudProviders());

    rcpm.getRankedCloudServices().add(RankedCloudService
        .builder()
        .provider("provider-1")
        .serviceId("provider-1-service-1")
        .rank(100)
        .ranked(false)
        .build());
    CloudServicesOrderedIterator providersOrderedIterator = cloudProviderEndpointServiceImpl
        .generateCloudProvidersOrderedIterator(rcpm, null);
    assertThat(providersOrderedIterator).isEmpty();

  }

  @Test
  @Parameters(method = "getCloudProviderEndpointSuccesfulParams")
  public void getCloudProviderEndpointSuccesful(String serviceType, boolean hybrid,
      IaaSType expectedIaaSType, boolean expectedWithImEndpoint) {

    CloudService cloudService = CloudService
        .builder()
        .id(UUID.randomUUID().toString())
        .serviceType(serviceType)
        .endpoint("www.example.com")
        .providerId("provider-RECAS-BARI")
        .type(CloudServiceType.COMPUTE)
        .hostname("example.com")
        .build();

    CloudProviderEndpointBuilder expected = CloudProviderEndpoint.builder();
    expected
        .cpEndpoint(cloudService.getEndpoint())
        .cpComputeServiceId(cloudService.getId())
        .iaasType(expectedIaaSType);
    if (hybrid) {
      expected.iaasHeaderId(cloudService.getId());
    } else {
      expected.iaasHeaderId(null);
    }
    if (expectedWithImEndpoint) {
      expected.imEndpoint(cloudService.getEndpoint());
    } else {
      expected.imEndpoint(null);
    }

    CloudProviderEndpoint actual = cloudProviderEndpointServiceImpl
        .getCloudProviderEndpoint(cloudService, new HashMap<>(), hybrid);
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
        .serviceType("unknownType")
        .endpoint("www.example.com")
        .providerId("provider-RECAS-BARI")
        .type(CloudServiceType.COMPUTE)
        .hostname("example.com")
        .build();

    assertThatCode(() -> cloudProviderEndpointServiceImpl
        .getCloudProviderEndpoint(cloudService, new HashMap<>(), false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unknown Cloud Provider type: " + cloudService);
  }

  @Test
  @Parameters({"CHRONOS|CHRONOS", "MARATHON|MARATHON", "TOSCA|IM"})
  public void getDeploymentProviderSuccesful(DeploymentType deploymentType,
      DeploymentProvider expectedDeploymentProvider) {
    assertThat(cloudProviderEndpointServiceImpl.getDeploymentProvider(deploymentType, null))
        .isEqualTo(expectedDeploymentProvider);
  }
}
