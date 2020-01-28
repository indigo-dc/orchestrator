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

import static org.hamcrest.Matchers.is;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.atomLinks;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.reply.orchestrator.dal.entity.DeploymentScheduler;
import it.reply.orchestrator.dal.entity.OidcEntity;
import it.reply.orchestrator.dal.entity.OidcEntityId;
import it.reply.orchestrator.dto.request.SchedulerRequest;
import it.reply.orchestrator.exception.http.NotFoundException;
import it.reply.orchestrator.resource.SchedulerResourceAssembler;
import it.reply.orchestrator.service.DeploymentSchedulerService;
import it.reply.orchestrator.utils.JsonUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.config.HateoasAwareSpringDataWebConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import junitparams.JUnitParamsRunner;

@WebMvcTest(controllers = DeploymentSchedulerController.class, secure = false)
@RunWith(JUnitParamsRunner.class)
@AutoConfigureRestDocs("target/generated-snippets")
@Import(HateoasAwareSpringDataWebConfiguration.class)
public class DeploymentSchedulerControllerTest {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private DeploymentSchedulerService deploymentSchedulerService;

  @SpyBean
  private SchedulerResourceAssembler schedulerResourceAssembler;

  private String storagePath = "http://www.site1.com/storagepath/*";

  private String schedulerId = "11ea41e0-8004-8129-89dd-d0577b460825";

  @Test
  public void addDeploymentSchedulerExisting() throws Exception {

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
  public void addDeploymentSchedulerNew() throws Exception {

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("cpus", 1);

    SchedulerRequest schedulerRequest = SchedulerRequest
        .builder()
        .parameters(parameters)
        .userStoragePath(storagePath)
        .template("template")
        .callback("http://localhost:8080/callback")
        .build();

    DeploymentScheduler ds = createDeploymentScheduler(UUID.randomUUID().toString());
    Mockito.when(deploymentSchedulerService.addDeploymentScheduler(schedulerRequest))
    .thenReturn(ds);

    MvcResult result =
        mockMvc.perform(post("/scheduler")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.serialize(schedulerRequest))
                .header(HttpHeaders.AUTHORIZATION,
                    OAuth2AccessToken.BEARER_TYPE + " <access token>"))
            .andExpect(status().is(201))
            .andExpect(jsonPath("$.userStoragePath", is(storagePath+ds.getId())))
            .andExpect(jsonPath("$.template", is("tosca_definitions_version: tosca_simple_yaml_1_0\ntopology_template:")))
            .andReturn();

  }

  @Test
  public void deleteDeploymentSchedulerExisting() throws Exception {

    Mockito.doNothing().when(deploymentSchedulerService).deleteDeploymentScheduler(schedulerId);

    mockMvc
        .perform(delete("/scheduler/" + schedulerId).header(HttpHeaders.AUTHORIZATION,
            OAuth2AccessToken.BEARER_TYPE + " <access token> "))
        .andExpect(status().isNoContent())
        .andDo(document("delete-deployment", preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint())));

  }

  @Test
  public void getSchedulers() throws Exception {

    OidcEntityId ownerId = new OidcEntityId();
    ownerId.setSubject(UUID.randomUUID().toString());
    ownerId.setIssuer("https://iam-test.indigo-datacloud.eu/");
    String ownerIdString = ownerId.getSubject()+"@"+ownerId.getIssuer();
    OidcEntity owner = new OidcEntity();
    owner.setOidcEntityId(ownerId);

    List<DeploymentScheduler> schedulers = createDeploymentSchedulerList(5);

    schedulers.forEach(scheduler -> scheduler.setOwner(owner));
    Pageable pageable = createDefaultPageable();
    Mockito.when(deploymentSchedulerService.getDeploymentSchedulers(pageable, ownerIdString))
        .thenReturn(new PageImpl<DeploymentScheduler>(schedulers, pageable, schedulers.size()));

    mockMvc
        .perform(get("/scheduler?createdBy=" + ownerIdString).accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, OAuth2AccessToken.BEARER_TYPE + " <access token>"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andDo(document("authentication", requestHeaders(
            headerWithName(HttpHeaders.AUTHORIZATION).description("OAuth2 bearer token"))))
        .andDo(document("schedulers", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
            requestParameters(parameterWithName("createdBy").description(
                "Optional parameter to filter the deployments based on who created them. The following values can be used:\n\n* `*OIDC_subject@OIDC_issuer*`: to ask for the deployments of a generic user\n* `*me*`: shortcut to ask for the deployments created by the user making the request")),

            responseFields(fieldWithPath("links[]").ignored(),

                fieldWithPath("content[].uuid").description("The unique identifier of a resource"),
                fieldWithPath("content[].creationTime").description(
                    "Creation date-time (http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14)"),
                fieldWithPath("content[].updateTime").description("Update date-time"),
                fieldWithPath("content[].createdBy").description(
                    "The OIDC info of the deployment's creator."),
                fieldWithPath("content[].userStoragePath").description(
                    "User Storage path"),
                fieldWithPath("content[].template").description("Template for deployment"),
                fieldWithPath("content[].callback").description(
                    "The endpoint used by the orchestrator to notify the progress of the deployment process."),
                fieldWithPath("content[].links[]").ignored(), fieldWithPath("page").ignored())));

  }

  @Test
  public void getDeploymentSchedulerSuccessfully() throws Exception {

    String schedulerId = "11ea41e0-8004-8129-89dd-d0577b460825";
    DeploymentScheduler deploymentScheduler = createDeploymentScheduler(schedulerId);
    Mockito.when(deploymentSchedulerService.getDeploymentScheduler(schedulerId)).thenReturn(deploymentScheduler);

    mockMvc.perform(get("/scheduler/" + schedulerId))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.uuid", is(schedulerId)));
  }

  @Test
  public void deleteDeploymentSchedulerNotExisting() throws Exception {

    Mockito.doThrow(new NotFoundException("The deployment <not-found> doesn't exist"))
        .when(deploymentSchedulerService)
        .deleteDeploymentScheduler(schedulerId);

    mockMvc.perform(delete("/scheduler/" + schedulerId))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code", is(404)))
        .andExpect(jsonPath("$.title", is("Not Found")))
        .andExpect(jsonPath("$.message", is("The deployment <not-found> doesn't exist")));

  }

  @Test
  public void schedulersPagination() throws Exception {

    List<DeploymentScheduler> schedulers = createDeploymentSchedulerList(5);

    Pageable pageable = createDefaultPageable();
    Mockito.when(deploymentSchedulerService.getDeploymentSchedulers(pageable, null))
        .thenReturn(new PageImpl<DeploymentScheduler>(schedulers, pageable, schedulers.size()));

    mockMvc.perform(get("/scheduler")
        .contentType(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.AUTHORIZATION,
            OAuth2AccessToken.BEARER_TYPE + " <access token>"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andDo(document("deploymentScheduler-pagination", preprocessResponse(prettyPrint()), responseFields(
            fieldWithPath("links[]").ignored(), fieldWithPath("content[].links[]").ignored(),

            fieldWithPath("page.size").description("The size of the page"),
            fieldWithPath("page.totalElements").description("The total number of elements"),
            fieldWithPath("page.totalPages").description("The total number of the page"),
            fieldWithPath("page.number").description("The current page"),
            fieldWithPath("content[].uuid").ignored(),
            fieldWithPath("content[].creationTime").ignored(),
            fieldWithPath("content[].updateTime").ignored(),
            fieldWithPath("content[].userStoragePath").ignored(),
            fieldWithPath("content[].template").ignored(),
            fieldWithPath("content[].callback").ignored())));
  }

  @Test
  public void getPagedSchedulers() throws Exception {

    List<DeploymentScheduler> schedulers = createDeploymentSchedulerList(5);
    Pageable pageable =
        new PageRequest(1, 2, new Sort(Direction.DESC, "createdAt"));
    Mockito.when(deploymentSchedulerService.getDeploymentSchedulers(pageable, null))
        .thenReturn(new PageImpl<DeploymentScheduler>(schedulers, pageable, schedulers.size()));

    mockMvc
        .perform(get("/scheduler?page=1&size=2").accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, OAuth2AccessToken.BEARER_TYPE + " <access token>"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andDo(document("deployment-paged", preprocessResponse(prettyPrint()),
            links(atomLinks(), linkWithRel("first").description("Hyperlink to the first page"),
                linkWithRel("prev").description("Hyperlink to the previous page"),
                linkWithRel("self").description("Self-referencing hyperlink"),
                linkWithRel("next").description("Self-referencing hyperlink"),
                linkWithRel("last").description("Hyperlink to the last page")),
            responseFields(fieldWithPath("links[]").ignored(), fieldWithPath("content").ignored(),
                fieldWithPath("page.").ignored())));

  }

  private static Pageable createDefaultPageable() {
    return new PageRequest(0, 10,
        new Sort(Direction.DESC, "createdAt"));
  }

  private List<DeploymentScheduler> createDeploymentSchedulerList(int n) {
    List<DeploymentScheduler> schedulers = new ArrayList<DeploymentScheduler>();
    IntStream.range(0, 10).forEach(
        nbr -> schedulers.add(createDeploymentScheduler("id"+nbr))
      );
    return schedulers;
  }
  private DeploymentScheduler createDeploymentScheduler(String id) {
    DeploymentScheduler deploymentScheduler = new DeploymentScheduler();
    deploymentScheduler.setId(id);
    deploymentScheduler.setCreatedAt(new Date());
    deploymentScheduler.setUpdatedAt(new Date());
    deploymentScheduler.setVersion(0L);
    deploymentScheduler.setUserStoragePath(storagePath+id);
    deploymentScheduler.setCallback("http://localhost");
    deploymentScheduler.setTemplate("tosca_definitions_version: tosca_simple_yaml_1_0\ntopology_template:");

    return deploymentScheduler;
  }

}
