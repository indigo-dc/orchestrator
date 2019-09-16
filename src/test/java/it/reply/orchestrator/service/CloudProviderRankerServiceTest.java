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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.google.common.collect.Lists;

import com.fasterxml.jackson.core.JsonProcessingException;

import it.reply.orchestrator.config.properties.CprProperties;
import it.reply.orchestrator.dto.ranker.CloudProviderRankerRequest;
import it.reply.orchestrator.dto.ranker.RankedCloudService;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.utils.JsonUtils;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.DefaultResponseCreator;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.List;

@RestClientTest(CloudProviderRankerService.class)
public class CloudProviderRankerServiceTest {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Autowired
  private CloudProviderRankerService cloudProviderRankerService;

  @Autowired
  private CprProperties cprProperties;

  @Autowired
  private MockRestServiceServer mockServer;

  private void mockRequest(CloudProviderRankerRequest cprr, DefaultResponseCreator response)
      throws JsonProcessingException {
    mockServer
        .expect(requestTo(cprProperties.getUrl() + cprProperties.getRankPath()))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().string(JsonUtils.serialize(cprr)))
        .andRespond(response);
  }

  public static List<RankedCloudService> generateMockedRankedServices() {
    return Lists.newArrayList(
        RankedCloudService
            .builder()
            .provider("provider-RECAS-BARI")
            .serviceId("provider-RECAS-BARI-service-1")
            .rank(2)
            .ranked(true)
            .build(),
        RankedCloudService
            .builder()
            .provider("provider-RECAS-BARI")
            .serviceId("provider-RECAS-BARI-service-2")
            .rank(3)
            .ranked(true)
            .build(),
        RankedCloudService
            .builder()
            .provider("provider-UPV-GRyCAP")
            .serviceId("provider-UPV-GRyCAP-service-1")
            .rank(1)
            .ranked(false)
            .errorReason("Some error reason")
            .build());
  }

  @Test
  public void doRankRequestSuccessfully() throws JsonProcessingException {
    CloudProviderRankerRequest cprr = CloudProviderRankerRequest.builder().build();
    List<RankedCloudService> services = generateMockedRankedServices();
    mockRequest(cprr, withSuccess(JsonUtils.serialize(services), MediaType.APPLICATION_JSON));

    List<RankedCloudService> result = cloudProviderRankerService.getProviderServicesRanking(cprr);

    assertThat(result).isEqualTo(services);
    mockServer.verify();
  }

  @Test
  public void doRankRequestWithError() throws JsonProcessingException {
    CloudProviderRankerRequest cprr = CloudProviderRankerRequest.builder().build();
    mockRequest(cprr, withServerError());

    assertThatCode(() -> cloudProviderRankerService.getProviderServicesRanking(cprr))
        .isInstanceOf(DeploymentException.class)
        .hasCauseInstanceOf(HttpStatusCodeException.class)
        .hasMessage("Error retrieving cloud provider ranking data; nested exception is "
            + "org.springframework.web.client.HttpServerErrorException: 500 Internal Server Error");
    mockServer.verify();
  }

}
