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

package it.reply.orchestrator.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.exception.http.NotFoundException;
import it.reply.orchestrator.resource.BaseResourceAssembler;
import it.reply.orchestrator.service.ResourceService;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.config.HateoasAwareSpringDataWebConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;

@WebMvcTest(controllers = ResourceController.class, secure = false)
@AutoConfigureRestDocs("target/generated-snippets")
@Import(HateoasAwareSpringDataWebConfiguration.class)
public class ResourceControllerTest {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();
  
  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ResourceService resourceService;
  
  @SpyBean
  private BaseResourceAssembler baseResourceAssembler;

  @Test
  public void getResources() throws Exception {
    Pageable pageable = ControllerTestUtils.createDefaultPageable();
    Deployment deployment = ControllerTestUtils.createDeployment();
    List<Resource> resources = ControllerTestUtils.createResources(deployment, 2, true);
    Mockito.when(resourceService.getResources(deployment.getId(), pageable))
        .thenReturn(new PageImpl<Resource>(resources, pageable, resources.size()));

    mockMvc
        .perform(get("/deployments/" + deployment.getId() + "/resources")
            .accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION,
                OAuth2AccessToken.BEARER_TYPE + " <access token>"))
        .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.page.totalElements", equalTo(2)))
        .andExpect(jsonPath("$.links[0].rel", is("self"))).andExpect(jsonPath("$.links[0].href",
            endsWith("/deployments/" + deployment.getId() + "/resources?page=0&size=10&sort=createdAt,desc")))

        .andDo(document("resources", preprocessResponse(prettyPrint()),
            responseFields(fieldWithPath("links[]").ignored(),
                fieldWithPath("content[].uuid").description("The unique identifier of a resource"),
                fieldWithPath("content[].creationTime").description(
                    "Creation date-time (http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14)"),
                fieldWithPath("content[].updateTime").description(
                    "Update date-time (http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14)"),
                fieldWithPath("content[].state").description(
                    "The status of the resource. (http://indigo-dc.github.io/orchestrator/apidocs/it/reply/orchestrator/enums/NodeStates.html)"),
                fieldWithPath("content[].toscaNodeType").optional()
                    .description("The type of the represented TOSCA node"),
                fieldWithPath("content[].toscaNodeName").optional()
                    .description("The name of the represented TOSCA node"),
                fieldWithPath("content[].requiredBy")
                    .description("A list of nodes that require this resource"),
                fieldWithPath("content[].links[]").ignored(), fieldWithPath("page").ignored())));

  }

  @Test
  public void getResourcesNotFoundNotDeployment() throws Exception {
    Pageable pageable = ControllerTestUtils.createDefaultPageable();
    Deployment deployment = ControllerTestUtils.createDeployment();
    Mockito.when(resourceService.getResources(deployment.getId(), pageable)).thenThrow(
        new NotFoundException("The deployment <" + deployment.getId() + "> doesn't exist"));

    mockMvc.perform(get("/deployments/" + deployment.getId() + "/resources"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code", is(404))).andExpect(jsonPath("$.title", is("Not Found")))
        .andExpect(
            jsonPath("$.message", is("The deployment <" + deployment.getId() + "> doesn't exist")));
  }

  @Test
  public void getResourceByIdAndDeploymentIdSuccesfully() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment();
    Resource resource = ControllerTestUtils.createResource(deployment);
    Mockito.when(resourceService.getResource(resource.getId(), deployment.getId()))
        .thenReturn(resource);

    mockMvc
        .perform(get("/deployments/" + deployment.getId() + "/resources/" + resource.getId())
            .header(HttpHeaders.AUTHORIZATION, OAuth2AccessToken.BEARER_TYPE + " <access token>"))
        .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.uuid", equalTo(resource.getId())))
        .andExpect(jsonPath("$.links[1].rel", equalTo("self")))
        .andExpect(jsonPath("$.links[1].href",
            endsWith("/deployments/" + deployment.getId() + "/resources/" + resource.getId())))
        .andDo(document("get-resource", preprocessResponse(prettyPrint()), responseFields(
            fieldWithPath("uuid").description("The unique identifier of a resource"),
            fieldWithPath("creationTime").description(
                "Creation date-time (http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14)"),
            fieldWithPath("updateTime").description(
                "Update date-time (http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14)"),
            fieldWithPath("state").description(
                "The status of the resource. (http://indigo-dc.github.io/orchestrator/apidocs/it/reply/orchestrator/enums/NodeStates.html)"),
            fieldWithPath("toscaNodeType").optional()
                .description("The type of the represented TOSCA node"),
            fieldWithPath("toscaNodeName").optional()
                .description("The name of the represented TOSCA node"),
            fieldWithPath("requiredBy").description("A list of nodes that require this resource"),
            fieldWithPath("links[]").ignored())));
  }
}
