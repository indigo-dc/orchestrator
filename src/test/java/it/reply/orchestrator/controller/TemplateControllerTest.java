package it.reply.orchestrator.controller;

/*
 * Copyright © 2015-2017 Santer Reply S.p.A.
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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import es.upv.i3m.grycap.file.NoNullOrEmptyFile;
import es.upv.i3m.grycap.file.Utf8File;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.exception.GlobalControllerExceptionHandler;
import it.reply.orchestrator.exception.http.NotFoundException;
import it.reply.orchestrator.service.TemplateService;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentation;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.Charset;
import java.nio.file.Paths;

public class TemplateControllerTest {

  private MockMvc mockMvc;

  @InjectMocks
  private TemplateController templateController = new TemplateController();

  @Mock
  private TemplateService templateService;

  @Spy
  private GlobalControllerExceptionHandler globalControllerExceptionHandler;

  @Rule
  public RestDocumentation restDocumentation = new RestDocumentation("target/generated-snippets");

  private final MediaType applicationJsonUtf8 = new MediaType(MediaType.APPLICATION_JSON.getType(),
      MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

  private final String templatePath = "./src/test/resources/tosca/galaxy_tosca.yaml";

  /**
   * Test environment setup.
   */
  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    mockMvc = MockMvcBuilders.standaloneSetup(templateController)
        .setControllerAdvice(globalControllerExceptionHandler)
        .apply(documentationConfiguration(this.restDocumentation)).dispatchOptions(true).build();
  }

  @Test
  public void getTemplate() throws Exception {

    Deployment deployment = ControllerTestUtils.createDeployment();

    String template = new NoNullOrEmptyFile(new Utf8File(Paths.get(templatePath))).read();
    deployment.setTemplate(template);

    Mockito.when(templateService.getTemplate(deployment.getId()))
        .thenReturn(deployment.getTemplate());

    MvcResult result =
        mockMvc
            .perform(get("/deployments/" + deployment.getId() + "/template").header(
                HttpHeaders.AUTHORIZATION, OAuth2AccessToken.BEARER_TYPE + " <access token>"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(new MediaType(MediaType.TEXT_PLAIN.getType(),
                MediaType.TEXT_PLAIN.getSubtype(), Charset.forName("ISO-8859-1"))))
            .andDo(document("get-template")).andReturn();

    String content = result.getResponse().getContentAsString();
    assertEquals(content, template);

  }

  @Test
  public void getTemplateNotFoundNotDeployment() throws Exception {

    Deployment deployment = ControllerTestUtils.createDeployment();

    Mockito.when(templateService.getTemplate(deployment.getId())).thenThrow(
        new NotFoundException("The deployment <" + deployment.getId() + "> doesn't exist"));

    mockMvc.perform(get("/deployments/" + deployment.getId() + "/template"))
        .andExpect(status().isNotFound()).andExpect(content().contentType(applicationJsonUtf8))
        .andExpect(jsonPath("$.code", is(404))).andExpect(jsonPath("$.title", is("Not Found")))
        .andExpect(
            jsonPath("$.message", is("The deployment <" + deployment.getId() + "> doesn't exist")));
  }
}
