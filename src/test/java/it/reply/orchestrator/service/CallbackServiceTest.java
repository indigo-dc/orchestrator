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

package it.reply.orchestrator.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import it.reply.orchestrator.controller.ControllerTestUtils;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.resource.DeploymentResourceAssembler;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.client.MockRestServiceServer;

@RestClientTest(CallbackService.class)
public class CallbackServiceTest {

  private static final String CALLBACK_URL = "http://example.com";

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Autowired
  private CallbackService callbackService;

  @Autowired
  private MockRestServiceServer mockServer;

  @MockBean
  private DeploymentRepository deploymentRepository;

  @SpyBean
  private DeploymentResourceAssembler deploymentResourceAssembler;

  private boolean doCallback(String url, HttpStatus status) {
    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.setCallback(url);

    when(deploymentRepository.findOne(deployment.getId()))
        .thenReturn(deployment);

    if (status != null) {
      mockServer
          .expect(requestTo(url))
          .andExpect(method(HttpMethod.POST))
          .andRespond(withStatus(status));
    }

    boolean result = callbackService.doCallback(deployment.getId());
    mockServer.verify();
    return result;
  }

  @Test
  public void doCallbackSuccessfully() {
    boolean result = doCallback(CALLBACK_URL, HttpStatus.OK);
    assertThat(result).isTrue();
  }

  @Test
  public void doCallbackError400() {
    boolean result = doCallback(CALLBACK_URL, HttpStatus.BAD_REQUEST);
    assertThat(result).isFalse();
  }

  @Test
  public void doCallbackError500() {
    boolean result = doCallback(CALLBACK_URL, HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(result).isFalse();
  }

  @Test
  public void doCallbackWithoutUrlSuccessfully() {
    boolean result = doCallback(null, null);
    assertThat(result).isFalse();
  }

}
