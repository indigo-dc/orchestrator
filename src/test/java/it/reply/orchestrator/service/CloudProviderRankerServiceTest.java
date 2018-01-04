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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;

import com.google.common.collect.Lists;

import it.reply.orchestrator.config.properties.CprProperties;
import it.reply.orchestrator.dto.ranker.CloudProviderRankerRequest;
import it.reply.orchestrator.dto.ranker.RankedCloudProvider;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.utils.CommonUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

public class CloudProviderRankerServiceTest {

  @Mock
  private RestTemplate restTemplate;

  @Spy
  private CprProperties cprProperties;

  private CloudProviderRankerService cloudProviderRankerService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    cprProperties.setUrl(URI.create("http://test.com"));
    cloudProviderRankerService = new CloudProviderRankerServiceImpl(cprProperties, restTemplate);
  }

  private void mockTemplate(CloudProviderRankerRequest cprr,
      ResponseEntity<List<RankedCloudProvider>> response) {
    HttpEntity<CloudProviderRankerRequest> entity = new HttpEntity<>(cprr);
    Mockito.when(restTemplate.exchange(eq(URI.create(cprProperties.getUrl() + "/rank")), eq(HttpMethod.POST), eq(entity),
        eq(new ParameterizedTypeReference<List<RankedCloudProvider>>() {
        }))).thenReturn(response);
  }

  public static List<RankedCloudProvider> generateMockedRankedProviders() {
    return Lists.newArrayList(new RankedCloudProvider("provider-RECAS-BARI", 2.0f, true, ""),
        new RankedCloudProvider("provider-UPV-GRyCAP", 1.0f, false, "Some error reason"));
  }

  @Test
  public void doRankRequestSuccessfully() {
    CloudProviderRankerRequest cprr = CloudProviderRankerRequest.builder().build();
    List<RankedCloudProvider> providers = generateMockedRankedProviders();
    ResponseEntity<List<RankedCloudProvider>> response =
        new ResponseEntity<List<RankedCloudProvider>>(providers, HttpStatus.OK);
    mockTemplate(cprr, response);
    List<RankedCloudProvider> result = cloudProviderRankerService.getProviderRanking(cprr);
    assertEquals(providers, result);
  }

  @Test(expected = DeploymentException.class)
  public void doRankRequestWithError() {
    CloudProviderRankerRequest cprr = CloudProviderRankerRequest.builder().build();
    List<RankedCloudProvider> providers = generateMockedRankedProviders();
    ResponseEntity<List<RankedCloudProvider>> response =
        new ResponseEntity<List<RankedCloudProvider>>(providers, HttpStatus.INTERNAL_SERVER_ERROR);
    mockTemplate(cprr, response);
    cloudProviderRankerService.getProviderRanking(cprr);
  }

}
