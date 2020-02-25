/*
 * Copyright © 2015-2020 Santer Reply S.p.A.
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

import static org.mockito.Mockito.doReturn;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import com.google.common.collect.Lists;

import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.util.Config;
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
import it.reply.orchestrator.dto.cmdb.KubernetesService;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.onedata.OneData.OneDataProviderInfo;
import it.reply.orchestrator.dto.workflow.CloudServicesOrderedIterator;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.function.ThrowingFunction;
import it.reply.orchestrator.service.ToscaService;
import it.reply.orchestrator.service.ToscaServiceTest;
import it.reply.orchestrator.service.VaultService;
import it.reply.orchestrator.util.TestUtil;
import junitparams.JUnitParamsRunner;

@RunWith(JUnitParamsRunner.class)
public class KubernetesServiceTest extends ToscaParserAwareTest {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Spy
  @InjectMocks
  private KubernetesServiceImpl kubernetesServiceImpl;

  @SpyBean
  @Autowired
  protected ToscaService toscaService;

  @MockBean
  private ResourceRepository resourceRepository;

  @MockBean
  private DeploymentRepository deploymentRepository;

  @MockBean
  private VaultService vaultService;

  @SpyBean
  private Config config;

  private static final String defaultVaultEndpoint = "https://default.vault.com:8200";

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    Mockito
        .when(oauth2tokenService.executeWithClientForResult(
            Mockito.any(), Mockito.any(), Mockito.any()))
        .thenAnswer(y -> ((ThrowingFunction) y.getArguments()[1]).apply("token"));

  }

  @Test
  public void testDoDeploy() throws IOException, URISyntaxException {
    Deployment deployment = generateDeployment();
    DeploymentMessage dm = generateDeployDm(deployment);

    KubernetesService cs = buildService();

    CloudServicesOrderedIterator csi = new CloudServicesOrderedIterator(Lists.newArrayList(cs));
    csi.next();
    dm.setCloudServicesOrderedIterator(csi);


    Mockito
        .when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);
    ;

    doReturn(new AppsV1Api(Config.defaultClient())).when(kubernetesServiceImpl).connectApi(dm);

//    Mockito
//        .when(toscaService.parseAndValidateTemplate(deployment.getTemplate(), inputs))
//        .thenReturn(deployment);
    Assertions
        .assertThat(kubernetesServiceImpl.doDeploy(dm))
        .isTrue();
  }

  private DeploymentMessage generateDeployDmKuber(Deployment deployment) {
    DeploymentMessage dm = new DeploymentMessage();
    dm.setDeploymentId(deployment.getId());
    CloudProviderEndpoint chosenCloudProviderEndpoint = CloudProviderEndpoint
        .builder()
        .cpComputeServiceId(UUID.randomUUID().toString())
        .cpEndpoint("http://www.example.com/api")
        .iaasType(IaaSType.KUBERNETES)
        .build();
    dm.setChosenCloudProviderEndpoint(chosenCloudProviderEndpoint);
    deployment.setCloudProviderEndpoint(chosenCloudProviderEndpoint);
    return dm;
  }

  private DeploymentMessage generateDeployDm(Deployment deployment) {
    DeploymentMessage dm = new DeploymentMessage();
    dm.setDeploymentId(deployment.getId());
    CloudProviderEndpoint chosenCloudProviderEndpoint = CloudProviderEndpoint
        .builder()
        .cpComputeServiceId(UUID.randomUUID().toString())
        .cpEndpoint("http://example.com")
        .iaasType(IaaSType.KUBERNETES)
        .build();
    dm.setChosenCloudProviderEndpoint(chosenCloudProviderEndpoint);
    deployment.setCloudProviderEndpoint(chosenCloudProviderEndpoint);
    Map<String, OneData> oneDataParameters = new HashMap<>();
    OneDataProviderInfo providerInfo = OneDataProviderInfo
        .builder()
        .cloudProviderId("provider-1")
        .cloudServiceId(UUID.randomUUID().toString())
        .endpoint("http://example.onedata.com")
        .id("test")
        .build();
    List<OneDataProviderInfo> oneproviders = new ArrayList<OneDataProviderInfo>();
    oneproviders.add(providerInfo);
    OneData parameter = OneData
        .builder()
        .oneproviders(oneproviders)
        .onezone("test")
        .path("/tmp/")
        .selectedOneprovider(providerInfo)
        .serviceSpace(true)
        .smartScheduling(false)
        .space("test")
        .token("0123456789-onedata-token")
        .build();
    oneDataParameters.put("provider-1", parameter);
    dm.setOneDataParameters(oneDataParameters);
    return dm;
  }

  private Deployment generateDeployment() throws IOException {
    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.setCloudProviderEndpoint(CloudProviderEndpoint.builder()
        .cpComputeServiceId(UUID.randomUUID().toString())
        .cpEndpoint("example.com")
        .iaasType(IaaSType.KUBERNETES)
        .build());
    deployment.setTemplate(
        TestUtil.getFileContentAsString(ToscaServiceTest.TEMPLATES_BASE_DIR + "kubernetes_app.yaml"));

    Resource runtime = new Resource();
    runtime.setDeployment(deployment);
    runtime.setId("1");
    runtime.setState(NodeStates.INITIAL);
    runtime.setToscaNodeName("Docker");
    runtime.setToscaNodeType("tosca.nodes.indigo.Container.Runtime.Docker");
    deployment.getResources().add(runtime);

    Resource app = new Resource();
    app.setDeployment(deployment);
    app.setId("2");
    app.setState(NodeStates.INITIAL);
    app.setToscaNodeName("kubernetes");
    app.setToscaNodeType("tosca.nodes.indigo.Container.Application.Docker.Kubernetes");
    app.addRequiredResource(runtime);
    deployment.getResources().add(app);

    Map<String, Object> paramMap = new HashMap();
    paramMap.put("replicas", 1);

    deployment.setParameters(paramMap);

    Mockito
        .when(resourceRepository.findByToscaNodeNameAndDeployment_id("Docker",
            deployment.getId()))
        .thenReturn(Lists.newArrayList(runtime));

    Mockito
        .when(resourceRepository.findByToscaNodeNameAndDeployment_id("kubernetes",
            deployment.getId()))
        .thenReturn(Lists.newArrayList(app));
    return deployment;
  }

  private KubernetesService buildService() {
    KubernetesService cs = KubernetesService
        .kubernetesBuilder()
        .endpoint("http://localhost:8080")
        .serviceType(CloudService.KUBERNETES_COMPUTE_SERVICE)
        .hostname("localhost")
        .providerId("RECAS-BARI")
        .id("http://localhost:8080")
        .type(CloudServiceType.COMPUTE)
        .iamEnabled(false)
        .publicService(true)
        .build();
    return cs;
  }
}
