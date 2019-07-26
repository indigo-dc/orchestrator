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

package it.reply.orchestrator.controller;

import it.reply.orchestrator.exception.http.NotFoundException;
import it.reply.orchestrator.service.TemplateService;
import it.reply.orchestrator.util.TestUtil;

import java.util.UUID;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TemplateController.class, secure = false)
@AutoConfigureRestDocs("target/generated-snippets")
public class TemplateControllerTest {

  private static final MediaType TEXT_PLAIN_UTF8 =
      MediaType.parseMediaType(MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8");

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private TemplateService templateService;

  private final String templatePath = "./src/test/resources/tosca/galaxy_tosca.yaml";

  @Test
  public void getTemplate() throws Exception {

    String deploymentUuid = UUID.randomUUID().toString();

    String expectedtemplate = TestUtil.getFileContentAsString(templatePath);

    when(templateService.getTemplate(deploymentUuid))
        .thenReturn(expectedtemplate);

    MvcResult result =
        mockMvc
            .perform(get("/deployments/" + deploymentUuid + "/template")
                .header(HttpHeaders.AUTHORIZATION,
                    OAuth2AccessToken.BEARER_TYPE + " <access token>"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(TEXT_PLAIN_UTF8))
            .andDo(document("get-template"))
            .andReturn();

    String returnedTemplate = result.getResponse().getContentAsString();
    assertThat(returnedTemplate).isEqualTo(expectedtemplate);

  }

  @Test
  public void getTemplateNotFoundNotDeployment() throws Exception {

    String deploymentUuid = UUID.randomUUID().toString();

    Exception expectedException =
        new NotFoundException("The deployment <" + deploymentUuid + "> doesn't exist");

    when(templateService.getTemplate(deploymentUuid))
        .thenThrow(expectedException);

    mockMvc
        .perform(get("/deployments/" + deploymentUuid + "/template"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(jsonPath("$.code", is(HttpStatus.NOT_FOUND.value())))
        .andExpect(jsonPath("$.title", is(HttpStatus.NOT_FOUND.getReasonPhrase())))
        .andExpect(
            jsonPath("$.message", is(expectedException.getMessage())));
  }
}
