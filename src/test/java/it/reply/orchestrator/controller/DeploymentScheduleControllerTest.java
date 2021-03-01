/*
 * Copyright © 2015-2021 I.N.F.N.
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
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.dal.entity.DeploymentSchedule;
import it.reply.orchestrator.dal.entity.DeploymentScheduleEvent;
import it.reply.orchestrator.dal.entity.OidcEntity;
import it.reply.orchestrator.dal.entity.OidcEntityId;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.request.DeploymentScheduleRequest;
import it.reply.orchestrator.enums.DeploymentScheduleStatus;
import it.reply.orchestrator.resource.DeploymentResourceAssembler;
import it.reply.orchestrator.resource.DeploymentScheduleEventResourceAssembler;
import it.reply.orchestrator.resource.DeploymentScheduleResourceAssembler;
import it.reply.orchestrator.service.DeploymentScheduleServiceImpl;
import it.reply.orchestrator.service.DeploymentService;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.orchestrator.utils.JsonUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import junitparams.JUnitParamsRunner;
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

@WebMvcTest(controllers = DeploymentScheduleController.class, secure = false)
@RunWith(JUnitParamsRunner.class)
@AutoConfigureRestDocs("target/generated-snippets")
@Import(HateoasAwareSpringDataWebConfiguration.class)
public class DeploymentScheduleControllerTest {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private OAuth2TokenService oauth2Tokenservice;

  @MockBean
  private OidcProperties oidcProperties;

  @MockBean
  private DeploymentScheduleServiceImpl deploymentScheduleService;

  @MockBean
  private DeploymentService deploymentService;

  @MockBean
  private ResourceRepository resourceRepository;

  @SpyBean
  private DeploymentResourceAssembler deploymentResourceAssembler;

  @SpyBean
  private DeploymentScheduleResourceAssembler deploymentScheduleResourceAssembler;

  @SpyBean
  private DeploymentScheduleEventResourceAssembler deploymentScheduleEventResourceAssembler;

  @Test
  public void getSchedules() throws Exception {

    OidcEntityId ownerId = new OidcEntityId();
    ownerId.setSubject(UUID.randomUUID().toString());
    ownerId.setIssuer("https://iam-test.indigo-datacloud.eu/");
    String ownerIdString = ownerId.getSubject()+"@"+ownerId.getIssuer();
    OidcEntity owner = new OidcEntity();
    owner.setOidcEntityId(ownerId);
    List<DeploymentSchedule> deploymentSchedules = ControllerTestUtils.createDeploymentSchedules(2);
    deploymentSchedules.forEach(deployment -> deployment.setOwner(owner));
    deploymentSchedules.get(1).setStatus(DeploymentScheduleStatus.SUSPENDED);
    Pageable pageable = ControllerTestUtils.createDefaultPageable();
    Mockito.when(deploymentScheduleService.getDeploymentSchedules(pageable, ownerIdString))
        .thenReturn(new PageImpl<DeploymentSchedule>(deploymentSchedules, pageable, deploymentSchedules.size()));

    mockMvc
        .perform(get("/schedules?createdBy=" + ownerIdString).accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, OAuth2AccessToken.BEARER_TYPE + " <access token>"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andDo(document("authentication", requestHeaders(
            headerWithName(HttpHeaders.AUTHORIZATION).description("OAuth2 bearer token"))))
        .andDo(document("get-deployment-schedules", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
            requestParameters(parameterWithName("createdBy").description(
                "Optional parameter to filter the deployments based on who created them. The following values can be used:\n\n* `*OIDC_subject@OIDC_issuer*`: to ask for the deployments of a generic user\n* `*me*`: shortcut to ask for the deployments created by the user making the request")),

            responseFields(fieldWithPath("links[]").ignored(),

                fieldWithPath("content[].uuid").description("The unique identifier of a resource"),
                fieldWithPath("content[].replicationExpression").description("The replication expression of the main replication rule"),
                fieldWithPath("content[].fileExpression").description("The file expression to match newly created files"),
                fieldWithPath("content[].numberOfReplicas").description("The number of replicas of the main replication rule"),
                fieldWithPath("content[].creationTime").description(
                    "Creation date-time (http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14)"),
                fieldWithPath("content[].updateTime").description("Update date-time"),
                fieldWithPath("content[].status").description(
                    "The status of the deployment schedule. (http://indigo-dc.github.io/orchestrator/apidocs/it/reply/orchestrator/enums/DeploymentScheduleStatus.html)"),
                fieldWithPath("content[].createdBy").description(
                    "The OIDC info of the deployment schedule's creator."),
                fieldWithPath("content[].callback").description(
                    "The endpoint used by the orchestrator to notify the progress of the deployment schedule event process."),
                fieldWithPath("content[].links[]").ignored(), fieldWithPath("page").ignored())));
  }

  @Test
  public void getPagedDeploymentSchedules() throws Exception {

    List<DeploymentSchedule> deploymentSchedules = ControllerTestUtils.createDeploymentSchedules(5);
    Pageable pageable =
        new PageRequest(1, 2, new Sort(Direction.DESC, "createdAt"));
    Mockito.when(deploymentScheduleService.getDeploymentSchedules(pageable, null))
        .thenReturn(new PageImpl<DeploymentSchedule>(deploymentSchedules, pageable, deploymentSchedules.size()));

    mockMvc
        .perform(get("/schedules?page=1&size=2").accept(MediaType.APPLICATION_JSON)
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

  @Test
  public void deploymentSchedulesPagination() throws Exception {

    List<DeploymentSchedule> deploymentSchedules = ControllerTestUtils.createDeploymentSchedules(5);
    Pageable pageable = ControllerTestUtils.createDefaultPageable();
    Mockito.when(deploymentScheduleService.getDeploymentSchedules(pageable, null))
        .thenReturn(new PageImpl<DeploymentSchedule>(deploymentSchedules, pageable, deploymentSchedules.size()));

    mockMvc
        .perform(get("/schedules").header(HttpHeaders.AUTHORIZATION,
            OAuth2AccessToken.BEARER_TYPE + " <access token>"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andDo(document("deployment-schedule-pagination", preprocessResponse(prettyPrint()), responseFields(
            fieldWithPath("links[]").ignored(), fieldWithPath("content[].links[]").ignored(),

            fieldWithPath("page.size").description("The size of the page"),
            fieldWithPath("page.totalElements").description("The total number of elements"),
            fieldWithPath("page.totalPages").description("The total number of the page"),
            fieldWithPath("page.number").description("The current page"),
            fieldWithPath("content[].uuid").ignored(),
            fieldWithPath("content[].creationTime").ignored(),
            fieldWithPath("content[].updateTime").ignored(),
            fieldWithPath("content[].status").ignored(),
            fieldWithPath("content[].replicationExpression").ignored(),
            fieldWithPath("content[].fileExpression").ignored(),
            fieldWithPath("content[].numberOfReplicas").ignored(),
            fieldWithPath("content[].callback").ignored())));
  }

  @Test
  public void deleteAllDeploymentSchedulesNotAllowed() throws Exception {

    mockMvc.perform(delete("/schedules")).andExpect(status().isMethodNotAllowed());
  }

  @Test
  public void getDeploymentScheduleSuccessfully() throws Exception {
    String deploymentScheduleId = "mmd34483-d937-4578-bfdb-ebe196bf82dd";
    DeploymentSchedule deploymentSchedule = ControllerTestUtils.createDeploymentSchedule(deploymentScheduleId);
    Mockito.when(deploymentScheduleService.getDeploymentSchedule(deploymentScheduleId)).thenReturn(deploymentSchedule);

    mockMvc
        .perform(get("/schedules/" + deploymentScheduleId).header(HttpHeaders.AUTHORIZATION,
            OAuth2AccessToken.BEARER_TYPE + " <access token>"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

        .andDo(document("get-deployment-schedule", preprocessResponse(prettyPrint()),

            responseFields(fieldWithPath("links[]").ignored(),

                fieldWithPath("uuid").description("The unique identifier of a resource"),
                fieldWithPath("replicationExpression").description("The replication expression of the main replication rule"),
                fieldWithPath("fileExpression").description("The file expression to match newly created files"),
                fieldWithPath("numberOfReplicas").description("The number of replicas of the main replication rule"),
                fieldWithPath("creationTime").description(
                    "Creation date-time (http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14)"),
                fieldWithPath("updateTime").description("Update date-time"),
                fieldWithPath("status").description(
                    "The status of the deployment schedule. (http://indigo-dc.github.io/orchestrator/apidocs/it/reply/orchestrator/enums/DeploymentScheduleStatus.html)"),
                fieldWithPath("callback").description(
                    "The endpoint used by the orchestrator to notify the progress of the deployment schedule event process."),
                fieldWithPath("links[]").ignored())));
  }

  @Test
  public void createDeploymentScheduleSuccessfully() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("cpus", 1);
    DeploymentScheduleRequest request = DeploymentScheduleRequest
        .deploymentScheduleBuilder()
        .parameters(parameters)
        .template("template")
        .callback("http://localhost:8080/callback")
        .keepLastAttempt(false)
        .maxProvidersRetry(1)
        .timeoutMins(5)
        .providerTimeoutMins(10)
        .fileExpression("scope:name*")
        .numberOfReplicas(1)
        .replicationExpression("RSE_RECAS")
        .build();

    DeploymentSchedule deploymentSchedule = ControllerTestUtils.createDeploymentSchedule();
    deploymentSchedule.setCallback(request.getCallback());
    Mockito.when(deploymentScheduleService.createDeploymentSchedule(request)).thenReturn(deploymentSchedule);

    mockMvc.perform(post("/schedules").contentType(MediaType.APPLICATION_JSON)
        .content(JsonUtils.serialize(request))
        .header(HttpHeaders.AUTHORIZATION, OAuth2AccessToken.BEARER_TYPE + " <access token>"))

        .andDo(document("create-deployment-schedule", preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint()),
            requestFields(
                fieldWithPath("template")
                    .description("A string containing a TOSCA YAML-formatted template"),
                fieldWithPath("parameters").optional()
                    .description("The input parameters of the deployment(Map of String, Object)"),
                fieldWithPath("callback").description("The deployment callback URL (optional)"),
                fieldWithPath("maxProvidersRetry").description(
                    "The maximum number Cloud providers on which attempt to create the deployment (Optional, default unbounded)"),
                fieldWithPath("timeoutMins").description(
                    "Overall timeout value, if provided, must be at least of 1 minute (Optional, default infinite)"),
                fieldWithPath("providerTimeoutMins").description(
                    "Provider timeout value, if provided, must be at least of 1 minute and equal or less than timeoutMins (Optional, default 14400 mins"),
                fieldWithPath("keepLastAttempt").description(
                    "Whether the Orchestrator, in case of failure, will keep the resources of the last deploy attempt or not (Optional, default false)"),
                fieldWithPath("replicationExpression").description("The replication expression of the main replication rule"),
                fieldWithPath("fileExpression").description("The file expression to match newly created files"),
                fieldWithPath("numberOfReplicas").description("The number of replicas of the main replication rule")),
                responseFields(fieldWithPath("links[]").ignored(),
                fieldWithPath("replicationExpression").description("The replication expression of the main replication rule"),
                fieldWithPath("fileExpression").description("The file expression to match newly created files"),
                fieldWithPath("numberOfReplicas").description("The number of replicas of the main replication rule"),
                fieldWithPath("uuid").description("The unique identifier of a resource"),
                fieldWithPath("creationTime").description(
                    "Creation date-time (http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14)"),
                fieldWithPath("updateTime").description(
                    "Update date-time (http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14)"),
                fieldWithPath("status").description(
                    "The status of the deployment. (http://indigo-dc.github.io/orchestrator/apidocs/it/reply/orchestrator/enums/Status.html)"),
                fieldWithPath("callback").description(
                    "The endpoint used by the orchestrator to notify the progress of the deployment process."),
                fieldWithPath("links[]").ignored())));

  }

  @Test
  public void getDeploymentScheduleEventSuccessfully() throws Exception {
    String deploymentScheduleId = "mmd34483-d937-4578-bfdb-ebe196bf82dd";
    List<DeploymentScheduleEvent> deploymentScheduleEvents = ControllerTestUtils.createDeploymentScheduleEvents(deploymentScheduleId, 2);
    Pageable pageable = ControllerTestUtils.createDefaultPageable();
    Mockito.when(deploymentScheduleService.getDeploymentScheduleEvents(deploymentScheduleId, pageable))
        .thenReturn(new PageImpl<>(deploymentScheduleEvents, pageable, deploymentScheduleEvents.size()));

    mockMvc
        .perform(get("/schedules/" + deploymentScheduleId + "/events").header(HttpHeaders.AUTHORIZATION,
            OAuth2AccessToken.BEARER_TYPE + " <access token>"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

        .andDo(document("get-deployment-schedule-events", preprocessResponse(prettyPrint()),

            responseFields(
                fieldWithPath("page").ignored(),
                fieldWithPath("links").ignored(),
                fieldWithPath("content[*].links").ignored(),
                fieldWithPath("content[*].deployment").description("The deployment created because of this event"),
                fieldWithPath("content[*].uuid").description("The unique identifier of the event"),
                fieldWithPath("content[*].creationTime").description("Creation date-time (http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14)"),
                fieldWithPath("content[*].updateTime").description("Update date-time (http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14)"),
                fieldWithPath("content[*].scope").description("The scope of the file triggering the event"),
                fieldWithPath("content[*].name").description("The name of the file triggering the event"),
                fieldWithPath("content[*].replicationStatus").description("The replication status of the main replication rule"))));
  }
}
