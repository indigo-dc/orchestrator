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

package it.reply.orchestrator.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.reply.orchestrator.dal.entity.DeploymentScheduler;
import it.reply.orchestrator.dto.request.SchedulerRequest;
import it.reply.orchestrator.service.DeploymentSchedulerService;
import it.reply.orchestrator.utils.JsonUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = DeploymentSchedulerController.class, secure = false)
@AutoConfigureRestDocs("target/generated-snippets")
public class DeploymentSchedulerControllerTest {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private DeploymentSchedulerService deploymentSchedulerService;

  private String storagePath = "http://www.site1.com/storagepath/*";

  @Test
  public void addStoragePathExisting() throws Exception {

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("cpus", 1);

    SchedulerRequest schedulerRequest = SchedulerRequest
        .builder()
        .parameters(parameters)
        .userStoragePath(storagePath)
        .template("template")
        .callback("http://localhost:8080/callback")
        .build();

    Mockito.when(deploymentSchedulerService.addDeploymentScheduler(schedulerRequest))
    .thenReturn(null);

    MvcResult result =
        mockMvc.perform(post("/scheduler")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.serialize(schedulerRequest))
                .header(HttpHeaders.AUTHORIZATION,
                    OAuth2AccessToken.BEARER_TYPE + " <access token>"))
            .andExpect(status().is(500))
            .andExpect(jsonPath("$.message", is("already exists")))
            .andReturn();

  }

  @Test
  public void addStoragePathNew() throws Exception {

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("cpus", 1);

    SchedulerRequest schedulerRequest = SchedulerRequest
        .builder()
        .parameters(parameters)
        .userStoragePath(storagePath)
        .template("template")
        .callback("http://localhost:8080/callback")
        .build();

    Mockito.when(deploymentSchedulerService.addDeploymentScheduler(schedulerRequest))
    .thenReturn(createDeploymentScheduler(UUID.randomUUID().toString()));

    MvcResult result =
        mockMvc.perform(post("/scheduler")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.serialize(schedulerRequest))
                .header(HttpHeaders.AUTHORIZATION,
                    OAuth2AccessToken.BEARER_TYPE + " <access token>"))
            .andExpect(status().is(201))
            .andExpect(jsonPath("$.userStoragePath", is(storagePath)))
            .andExpect(jsonPath("$.template", is("tosca_definitions_version: tosca_simple_yaml_1_0\ntopology_template:")))
            .andReturn();

  }

  private DeploymentScheduler createDeploymentScheduler(String id) {
    DeploymentScheduler deploymentScheduler = new DeploymentScheduler();
    deploymentScheduler.setId(id);
    deploymentScheduler.setCreatedAt(new Date());
    deploymentScheduler.setUpdatedAt(new Date());
    deploymentScheduler.setVersion(0L);
    deploymentScheduler.setUserStoragePath(storagePath);
    deploymentScheduler.setCallback("http://localhost");
    deploymentScheduler.setTemplate("tosca_definitions_version: tosca_simple_yaml_1_0\ntopology_template:");

    return deploymentScheduler;
  }

}
