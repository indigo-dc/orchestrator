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

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.collect.Lists;

import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.CloudServiceData;
import it.reply.orchestrator.dto.cmdb.Type;
import it.reply.orchestrator.dto.dynafed.Dynafed;
import it.reply.orchestrator.dto.dynafed.Dynafed.File;
import it.reply.orchestrator.dto.dynafed.Metalink;
import it.reply.orchestrator.dto.dynafed.Metalink.Url;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.function.ThrowingFunction;
import it.reply.orchestrator.service.security.OAuth2TokenService;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpStatusCodeException;

import junitparams.JUnitParamsRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RunWith(JUnitParamsRunner.class)
@RestClientTest(DynafedServiceImpl.class)
public class DynafedServiceTest {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Autowired
  private DynafedServiceImpl dynafedService;

  @Autowired
  private MockRestServiceServer mockServer;

  private static XmlMapper mapper = new XmlMapper();

  @MockBean
  private OAuth2TokenService oauth2TokenService;

  private static final OidcTokenId oidcTokenId = new OidcTokenId();

  private final static String dynafedBasePath = "http://dynafed.example.com/";

  private final static String goodStorageHostname = "storage1.example.com";
  private final static String goodStorageBasePath = "http://" + goodStorageHostname + "/";

  private final static String badStorageHostname = "storage2.example.com";
  private final static String badStorageBasePath = "http://" + badStorageHostname + "/";

  private final static String accessToken = "AccessToken";
  private final static String fileName = "fileName";

  @Before
  public void setup() throws Exception {

    when(oauth2TokenService.executeWithClientForResult(eq(oidcTokenId), any(), any()))
        .then(a -> ((ThrowingFunction) a.getArguments()[1]).apply(accessToken));
  }

  @Test
  public void testFailPopulateDynafedHttpError() {
    Dynafed dyanfedRequirement = Dynafed
        .builder()
        .files(Lists.newArrayList(File
            .builder()
            .endpoint(dynafedBasePath + fileName)
            .build()))
        .build();

    mockServer
        .expect(requestTo(dynafedBasePath + fileName + "?metalink"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer " + accessToken))
        .andRespond(withBadRequest());

    Map<String, CloudProvider> cloudProviders = generateCloudProviders();

    assertThatCode(
        () -> dynafedService.populateDyanfedData(dyanfedRequirement, cloudProviders, oidcTokenId))
        .isInstanceOf(DeploymentException.class)
        .hasMessageStartingWith("Error retrieving metalink of file " + dynafedBasePath + fileName)
        .hasCauseInstanceOf(HttpStatusCodeException.class);

    mockServer.verify();
  }

  @Test
  public void testSuccessPopulateDynafed() throws IOException {
    Dynafed dyanfedRequirement = Dynafed
        .builder()
        .files(Lists.newArrayList(File
            .builder()
            .endpoint(dynafedBasePath + fileName)
            .build()))
        .build();
    Metalink metalink = Metalink
        .builder()
        .files(Lists.newArrayList(Metalink.File
            .builder()
            .name(fileName)
            .size(1L)
            .urls(Lists.newArrayList(Url
                .builder()
                .type("http")
                .value(URI.create(goodStorageBasePath + fileName))
                .build()))
            .build()))
        .build();

    mockServer
        .expect(requestTo(dynafedBasePath + fileName + "?metalink"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer " + accessToken))
        .andRespond(withSuccess(mapper.writeValueAsString(metalink), MediaType.APPLICATION_XML));

    Map<String, CloudProvider> cloudProviders = generateCloudProviders();

    dynafedService.populateDyanfedData(dyanfedRequirement, cloudProviders, oidcTokenId);
    assertThat(dyanfedRequirement.getFiles()).hasSize(1);
    File file = dyanfedRequirement.getFiles().get(0);
    assertThat(file.getSize()).isEqualTo(1L);
    assertThat(file.getEndpoint()).isEqualTo(dynafedBasePath + fileName);
    assertThat(file.getResources()).hasSize(1);
    assertThat(file.getResources().get(0).getEndpoint()).isEqualTo(goodStorageBasePath + fileName);
    assertThat(file.getResources().get(0).getCloudProviderId()).isEqualTo("cloud-provider-id");
    assertThat(file.getResources().get(0).getCloudServiceId()).isEqualTo("storage-service-id");

    mockServer.verify();
  }

  @Test
  public void testFailPopulateDynafedServiceNotFound() throws IOException {
    Dynafed dyanfedRequirement = Dynafed
        .builder()
        .files(Lists.newArrayList(File
            .builder()
            .endpoint(dynafedBasePath + fileName)
            .build()))
        .build();
    Metalink metalink = Metalink
        .builder()
        .files(Lists.newArrayList(Metalink.File
            .builder()
            .name(fileName)
            .size(1L)
            .urls(Lists.newArrayList(Url
                .builder()
                .type("http")
                .value(URI.create(badStorageBasePath + fileName))
                .build()))
            .build()))
        .build();

    mockServer
        .expect(requestTo(dynafedBasePath + fileName + "?metalink"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer " + accessToken))
        .andRespond(withSuccess(mapper.writeValueAsString(metalink), MediaType.APPLICATION_XML));

    Map<String, CloudProvider> cloudProviders = generateCloudProviders();

    assertThatCode(
        () -> dynafedService.populateDyanfedData(dyanfedRequirement, cloudProviders, oidcTokenId))
        .isInstanceOf(DeploymentException.class)
        .hasMessage("No registered storage service available for file %s",
            dynafedBasePath + fileName);

    mockServer.verify();
  }

  private Map<String, CloudProvider> generateCloudProviders() {
    Map<String, CloudService> cloudServices = new HashMap<>();

    CloudService storageService = CloudService
        .builder()
        .id("storage-service-id")
        .data(CloudServiceData
            .builder()
            .providerId("cloud-provider-id")
            .endpoint(goodStorageBasePath)
            .serviceType(CloudService.CDMI_STORAGE_SERVICE)
            .type(Type.STORAGE)
            .hostname(goodStorageHostname)
            .build())
        .build();
    cloudServices.put(storageService.getId(), storageService);

    CloudService oneProviderService = CloudService
        .builder()
        .id("oneprovider-service-id")
        .data(CloudServiceData
            .builder()
            .providerId("cloud-provider-id")
            .endpoint(goodStorageBasePath)
            .serviceType(CloudService.ONEPROVIDER_STORAGE_SERVICE)
            .type(Type.STORAGE)
            .hostname(goodStorageHostname)
            .build())
        .build();
    cloudServices.put(oneProviderService.getId(), oneProviderService);

    CloudService computeService = CloudService
        .builder()
        .id("compute-service-id")
        .data(CloudServiceData
            .builder()
            .providerId("cloud-provider-id")
            .endpoint(goodStorageBasePath)
            .serviceType(CloudService.OPENSTACK_COMPUTE_SERVICE)
            .type(Type.COMPUTE)
            .hostname(goodStorageHostname)
            .build())
        .build();
    cloudServices.put(oneProviderService.getId(), computeService);

    Map<String, CloudProvider> cloudProviders = new HashMap<>();
    CloudProvider cloudProvider = CloudProvider
        .builder()
        .id("cloud-provider-id")
        .cmdbProviderServices(cloudServices)
        .build();
    cloudProviders.put(cloudProvider.getId(), cloudProvider);
    return cloudProviders;
  }

}
