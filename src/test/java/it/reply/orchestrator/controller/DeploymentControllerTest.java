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

import static org.hamcrest.CoreMatchers.containsString;
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
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.OidcEntity;
import it.reply.orchestrator.dal.entity.OidcEntityId;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.request.DeploymentRequest;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.exception.http.ConflictException;
import it.reply.orchestrator.exception.http.NotFoundException;
import it.reply.orchestrator.resource.DeploymentResourceAssembler;
import it.reply.orchestrator.service.DeploymentService;
import it.reply.orchestrator.utils.JsonUtils;

import java.sql.SQLTransientException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import org.apache.ibatis.exceptions.PersistenceException;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.hamcrest.Matchers;
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
import org.springframework.dao.TransientDataAccessException;
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

@WebMvcTest(controllers = DeploymentController.class, secure = false)
@RunWith(JUnitParamsRunner.class)
@AutoConfigureRestDocs("target/generated-snippets")
@Import(HateoasAwareSpringDataWebConfiguration.class)
public class DeploymentControllerTest {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private DeploymentService deploymentService;

  @MockBean
  private ResourceRepository resourceRepository;

  @SpyBean
  private DeploymentResourceAssembler deploymentResourceAssembler;

  @Test
  public void getDeployments() throws Exception {

    OidcEntityId ownerId = new OidcEntityId();
    ownerId.setSubject(UUID.randomUUID().toString());
    ownerId.setIssuer("https://iam-test.indigo-datacloud.eu/");
    String ownerIdString = ownerId.getSubject()+"@"+ownerId.getIssuer();
    OidcEntity owner = new OidcEntity();
    owner.setOidcEntityId(ownerId);
    List<Deployment> deployments = ControllerTestUtils.createDeployments(2);
    deployments.forEach(deployment -> deployment.setOwner(owner));
    deployments.get(0).setStatus(Status.CREATE_FAILED);
    deployments.get(0).setStatusReason("Some reason");
    deployments.get(1).setStatus(Status.CREATE_COMPLETE);
    Pageable pageable = ControllerTestUtils.createDefaultPageable();
    Mockito.when(deploymentService.getDeployments(pageable, ownerIdString))
        .thenReturn(new PageImpl<Deployment>(deployments, pageable, deployments.size()));

    mockMvc
        .perform(get("/deployments?createdBy=" + ownerIdString).accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, OAuth2AccessToken.BEARER_TYPE + " <access token>"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andDo(document("authentication", requestHeaders(
            headerWithName(HttpHeaders.AUTHORIZATION).description("OAuth2 bearer token"))))
        .andDo(document("deployments", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
            requestParameters(parameterWithName("createdBy").description(
                "Optional parameter to filter the deployments based on who created them. The following values can be used:\n\n* `*OIDC_subject@OIDC_issuer*`: to ask for the deployments of a generic user\n* `*me*`: shortcut to ask for the deployments created by the user making the request")),

            responseFields(fieldWithPath("links[]").ignored(),

                fieldWithPath("content[].uuid").description("The unique identifier of a resource"),
                fieldWithPath("content[].creationTime").description(
                    "Creation date-time (http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14)"),
                fieldWithPath("content[].updateTime").description("Update date-time"),
                fieldWithPath("content[].status").description(
                    "The status of the deployment. (http://indigo-dc.github.io/orchestrator/apidocs/it/reply/orchestrator/enums/Status.html)"),
                fieldWithPath("content[].statusReason").description(
                    "Verbose explanation of reason that lead to the deployment status (Present only if the deploy is in some error status)"),
                fieldWithPath("content[].task").description(
                    "The current step of the deployment process. (http://indigo-dc.github.io/orchestrator/apidocs/it/reply/orchestrator/enums/Task.html)"),
                fieldWithPath("content[].createdBy").description(
                    "The OIDC info of the deployment's creator."),
                fieldWithPath("content[].callback").description(
                    "The endpoint used by the orchestrator to notify the progress of the deployment process."),
                fieldWithPath("content[].outputs").description("The outputs of the TOSCA document"),
                fieldWithPath("content[].links[]").ignored(), fieldWithPath("page").ignored())));
  }

  @Test
  public void getPagedDeployments() throws Exception {

    List<Deployment> deployments = ControllerTestUtils.createDeployments(5);
    Pageable pageable =
        new PageRequest(1, 2, new Sort(Direction.DESC, "createdAt"));
    Mockito.when(deploymentService.getDeployments(pageable, null))
        .thenReturn(new PageImpl<Deployment>(deployments, pageable, deployments.size()));

    mockMvc
        .perform(get("/deployments?page=1&size=2").accept(MediaType.APPLICATION_JSON)
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

    // .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)));
  }

  @Test
  public void deploymentsPagination() throws Exception {

    List<Deployment> deployments = ControllerTestUtils.createDeployments(5);
    Pageable pageable = ControllerTestUtils.createDefaultPageable();
    Mockito.when(deploymentService.getDeployments(pageable, null))
        .thenReturn(new PageImpl<Deployment>(deployments, pageable, deployments.size()));

    mockMvc
        .perform(get("/deployments").header(HttpHeaders.AUTHORIZATION,
            OAuth2AccessToken.BEARER_TYPE + " <access token>"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andDo(document("deployment-pagination", preprocessResponse(prettyPrint()), responseFields(
            fieldWithPath("links[]").ignored(), fieldWithPath("content[].links[]").ignored(),

            fieldWithPath("page.size").description("The size of the page"),
            fieldWithPath("page.totalElements").description("The total number of elements"),
            fieldWithPath("page.totalPages").description("The total number of the page"),
            fieldWithPath("page.number").description("The current page"),
            fieldWithPath("content[].uuid").ignored(),
            fieldWithPath("content[].creationTime").ignored(),
            fieldWithPath("content[].updateTime").ignored(),
            fieldWithPath("content[].status").ignored(),
            fieldWithPath("content[].outputs").ignored(), fieldWithPath("content[].task").ignored(),
            fieldWithPath("content[].callback").ignored())));
  }

  @Test
  public void deleteAlldeploymentsNotAllowed() throws Exception {

    mockMvc.perform(delete("/deployments")).andExpect(status().isMethodNotAllowed());
  }

  @Test
  public void getDeploymentSuccessfully() throws Exception {

    String deploymentId = "mmd34483-d937-4578-bfdb-ebe196bf82dd";
    Deployment deployment = ControllerTestUtils.createDeployment(deploymentId);
    Mockito.when(deploymentService.getDeployment(deploymentId)).thenReturn(deployment);

    mockMvc.perform(get("/deployments/" + deploymentId))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.uuid", is(deploymentId)));
  }

  @Test
  public void getDeploymentExtendedInfo() throws Exception {
    String deploymentId = "mmd34483-d937-4578-bfdb-ebe196bf82dd";
    String result = "{\"vmProperties\":[{\"class\":\"network\",\"id\":\"pub_network\",\"outbound\":\"yes\",\"provider_id\":\"external\"},{\"class\":\"network\",\"id\":\"priv_network\",\"provider_id\":\"provider-2099\"},{\"class\":\"system\",\"id\":\"simple_node1\",\"instance_name\":\"simple_node1-158799480931\",\"disk.0.os.flavour\":\"ubuntu\",\"disk.0.image.url\":\"ost://api.cloud.test.com/f46f7387-a371-44ec-9a2d-16a8f2a85786\",\"cpu.count\":1,\"memory.size\":2097152000,\"instance_type\":\"m1.small\",\"net_interface.1.connection\":\"pub_network\",\"net_interface.0.connection\":\"priv_network\",\"cpu.arch\":\"x86_64\",\"disk.0.free_size\":10737418240,\"disk.0.os.credentials.username\":\"cloudadm\",\"provider.type\":\"OpenStack\",\"provider.host\":\"api.cloud.test.com\",\"provider.port\":5000,\"disk.0.os.credentials.private_key\":\"\",\"state\":\"configured\",\"instance_id\":\"11d647dc-97f1-4347-8ede-ec83e2b64976\",\"net_interface.0.ip\":\"192.168.1.1\",\"net_interface.1.ip\":\"1.2.3.4\"}]}";
    //Mockito.when(deploymentService.getDeploymentExtendedInfo(deploymentId, requestedWithToken)).thenReturn(result);

    mockMvc.perform(get("/deployments/" + deploymentId + "/extrainfo").header(HttpHeaders.AUTHORIZATION, OAuth2AccessToken.BEARER_TYPE + " <access token>"))
    .andExpect(status().isOk())
    .andExpect(content().string(containsString(result)))
    .andDo(document("get-deployment-extended-info",
        preprocessResponse(prettyPrint())));
  }

  @Test
  public void getDeploymentLog() throws Exception {
    String deploymentId = "mmd34483-d937-4578-bfdb-ebe196bf82dd";
    String result = "deployment log";
   // Mockito.when(deploymentService.getDeploymentLog(deploymentId, requestedWithToken)).thenReturn(result);

    mockMvc.perform(get("/deployments/" + deploymentId + "/log").header(HttpHeaders.AUTHORIZATION, OAuth2AccessToken.BEARER_TYPE + " <access token>"))
    .andExpect(status().isOk())
    .andExpect(content().string(containsString(result)))
    .andDo(document("get-deployment-log",
        preprocessResponse(prettyPrint())));
  }

  @Test
  public void deploymentHypermedia() throws Exception {

    String deploymentId = "mmd34483-d937-4578-bfdb-ebe196bf82dd";
    Deployment deployment = ControllerTestUtils.createDeployment(deploymentId);
    Mockito.when(deploymentService.getDeployment(deploymentId)).thenReturn(deployment);

    mockMvc
        .perform(get("/deployments/" + deploymentId).header(HttpHeaders.AUTHORIZATION,
            OAuth2AccessToken.BEARER_TYPE + " <access token>"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andDo(document("deployment-hypermedia", preprocessResponse(prettyPrint()),
            links(atomLinks(), linkWithRel("self").description("Self-referencing hyperlink"),
                linkWithRel("template").description("Template reference hyperlink"),
                linkWithRel("resources").description("Resources reference hyperlink")),
            responseFields(
                fieldWithPath("links[].rel").description(
                    "means relationship. In this case, it's a self-referencing hyperlink."
                        + "More complex systems might include other relationships."),
                fieldWithPath("links[].href")
                    .description("Is a complete URL that uniquely defines the resource."),
                fieldWithPath("uuid").ignored(), fieldWithPath("creationTime").ignored(),
                fieldWithPath("updateTime").ignored(), fieldWithPath("status").ignored(),
                fieldWithPath("outputs").ignored(), fieldWithPath("task").ignored(),
                fieldWithPath("callback").ignored())));
  }

  @Test
  public void getDeploymentWithOutputSuccessfully() throws Exception {

    String deploymentId = "mmd34483-d937-4578-bfdb-ebe196bf82dd";
    Deployment deployment = ControllerTestUtils.createDeployment(deploymentId);
    Map<String, Object> outputs = new HashMap<>();
    String key = "server_ip";
    String value = "10.0.0.1";
    outputs.put(key, value);
    deployment.setOutputs(outputs);
    deployment.setStatus(Status.CREATE_FAILED);
    deployment.setStatusReason("Some reason");
    Mockito.when(deploymentService.getDeployment(deploymentId)).thenReturn(deployment);

    mockMvc
        .perform(get("/deployments/" + deploymentId).header(HttpHeaders.AUTHORIZATION,
            OAuth2AccessToken.BEARER_TYPE + " <access token>"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.outputs", Matchers.hasEntry(key, value)))

        .andDo(document("deployment", preprocessResponse(prettyPrint()),

            responseFields(fieldWithPath("links[]").ignored(),

                fieldWithPath("uuid").description("The unique identifier of a resource"),
                fieldWithPath("creationTime").description(
                    "Creation date-time (http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14)"),
                fieldWithPath("updateTime").description("Update date-time"),
                fieldWithPath("status").description(
                    "The status of the deployment. (http://indigo-dc.github.io/orchestrator/apidocs/it/reply/orchestrator/enums/Status.html)"),
                fieldWithPath("statusReason").description(
                    "Verbose explanation of reason that lead to the deployment status (Present only if the deploy is in some error status)"),
                fieldWithPath("task").description(
                    "The current step of the deployment process. (http://indigo-dc.github.io/orchestrator/apidocs/it/reply/orchestrator/enums/Task.html)"),
                fieldWithPath("callback").description(
                    "The endpoint used by the orchestrator to notify the progress of the deployment process."),
                fieldWithPath("outputs").description("The outputs of the TOSCA document"),
                fieldWithPath("links[]").ignored())));
  }

  @Test
  public void getDeploymentNotFound() throws Exception {

    String deploymentId = "mmd34483-d937-4578-bfdb-ebe196bf82dd";
    Mockito.when(deploymentService.getDeployment(deploymentId))
        .thenThrow(new NotFoundException("Message"));

    mockMvc
        .perform(get("/deployments/" + deploymentId).header(HttpHeaders.AUTHORIZATION,
            OAuth2AccessToken.BEARER_TYPE + " <access token>"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code", is(404)))
        .andDo(document("deployment-not-found", preprocessResponse(prettyPrint()), responseFields(
            fieldWithPath("code").description("The HTTP status code"),
            fieldWithPath("title").description("The HTTP status name"),
            fieldWithPath("message").description("A displayable message describing the error"))))
        .andExpect(jsonPath("$.title", is("Not Found")))
        .andExpect(jsonPath("$.message", is("Message")));
  }

  @Test
  public void createDeploymentUnsupportedMediaType() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("cpus", 1);
    DeploymentRequest request = DeploymentRequest
        .builder()
        .parameters(parameters)
        .template("template")
        .callback("http://localhost:8080/callback")
        .build();
    mockMvc
        .perform(post("/deployments").contentType(MediaType.TEXT_PLAIN)
            .content(JsonUtils.serialize(request)))
        .andExpect(status().isUnsupportedMediaType());

  }

  @Test
  public void createDeploymentSuccessfully() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("cpus", 1);
    DeploymentRequest request = DeploymentRequest
        .builder()
        .parameters(parameters)
        .template("template")
        .callback("http://localhost:8080/callback")
        .keepLastAttempt(false)
        .maxProvidersRetry(1)
        .timeoutMins(5)
        .providerTimeoutMins(10)
        .build();

    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.setCallback(request.getCallback());
    deployment.setStatus(Status.CREATE_IN_PROGRESS);
    Mockito.when(deploymentService.createDeployment(request)).thenReturn(deployment);

    mockMvc.perform(post("/deployments").contentType(MediaType.APPLICATION_JSON)
        .content(JsonUtils.serialize(request))
        .header(HttpHeaders.AUTHORIZATION, OAuth2AccessToken.BEARER_TYPE + " <access token>"))

        .andDo(document("create-deployment", preprocessRequest(prettyPrint()),
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
                    "Whether the Orchestrator, in case of failure, will keep the resources of the last deploy attempt or not (Optional, default false)")),
                responseFields(fieldWithPath("links[]").ignored(),
                fieldWithPath("uuid").description("The unique identifier of a resource"),
                fieldWithPath("creationTime").description(
                    "Creation date-time (http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14)"),
                fieldWithPath("updateTime").description(
                    "Update date-time (http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14)"),
                fieldWithPath("status").description(
                    "The status of the deployment. (http://indigo-dc.github.io/orchestrator/apidocs/it/reply/orchestrator/enums/Status.html)"),
                fieldWithPath("task").description(
                    "The current step of the deployment process. (http://indigo-dc.github.io/orchestrator/apidocs/it/reply/orchestrator/enums/Task.html)"),
                fieldWithPath("outputs").description("The outputs of the TOSCA document"),
                fieldWithPath("callback").description(
                    "The endpoint used by the orchestrator to notify the progress of the deployment process."),
                fieldWithPath("links[]").ignored())));

  }

  @Test
  public void updateDeploymentNotExists() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("cpus", 1);
    DeploymentRequest request = DeploymentRequest
        .builder()
        .parameters(parameters)
        .template("template")
        .callback("http://localhost:8080/callback")
        .build();

    String deploymentId = "mmd34483-d937-4578-bfdb-ebe196bf82dd";
    Mockito.doThrow(new NotFoundException("Message"))
        .when(deploymentService)
        .updateDeployment(deploymentId, request, requestedWithToken);

    mockMvc
        .perform(put("/deployments/" + deploymentId).contentType(MediaType.APPLICATION_JSON)
            .content(JsonUtils.serialize(request)))
        .andExpect(jsonPath("$.code", is(404)))
        .andExpect(jsonPath("$.title", is("Not Found")))
        .andExpect(jsonPath("$.message", is("Message")));
  }

  @Test
  public void updateDeploymentDeleteInProgress() throws Exception {
    DeploymentRequest request = DeploymentRequest
        .builder()
        .template("template")
        .build();

    String deploymentId = "mmd34483-d937-4578-bfdb-ebe196bf82dd";
    Mockito.doThrow(new ConflictException("Cannot update a deployment in DELETE_IN_PROGRESS state"))
        .when(deploymentService)
        .updateDeployment(deploymentId, request, requestedWithToken);

    mockMvc
        .perform(put("/deployments/" + deploymentId).contentType(MediaType.APPLICATION_JSON)
            .content(JsonUtils.serialize(request)))
        .andExpect(jsonPath("$.code", is(409)))
        .andExpect(jsonPath("$.title", is("Conflict")))
        .andExpect(
            jsonPath("$.message", is("Cannot update a deployment in DELETE_IN_PROGRESS state")));
  }

  public Object[] generateTransientPersistenceExceptions() {
    return new Object[]{
        Mockito.mock(TransientDataAccessException.class),
        new FlowableOptimisticLockingException(""),
        new PersistenceException(new SQLTransientException(""))
    };
  }

  @Test
  @Parameters(method = "generateTransientPersistenceExceptions")
  public void updateDeploymentConcurrentTransientException(Exception ex)
      throws Exception {
    DeploymentRequest request = DeploymentRequest
        .builder()
        .template("template")
        .build();

    String deploymentId = "mmd34483-d937-4578-bfdb-ebe196bf82dd";
    Mockito.doThrow(ex)
        .when(deploymentService)
        .updateDeployment(deploymentId, request, requestedWithToken);

    mockMvc
        .perform(put("/deployments/" + deploymentId).contentType(MediaType.APPLICATION_JSON)
            .content(JsonUtils.serialize(request)))
        .andExpect(header().string(HttpHeaders.RETRY_AFTER, "0"))
        .andExpect(jsonPath("$.code", is(409)))
        .andExpect(jsonPath("$.title", is("Conflict")))
        .andExpect(
            jsonPath("$.message",
                is("The request couldn't be fulfilled because of a concurrent update. Please retry later")));
  }

  @Test
  @Parameters(method = "generateTransientPersistenceExceptions")
  public void deleteDeploymentConcurrentTransientException(Exception ex)
      throws Exception {

    String deploymentId = "mmd34483-d937-4578-bfdb-ebe196bf82dd";
    Mockito.doThrow(ex)
        .when(deploymentService)
        .deleteDeployment(deploymentId, requestedWithToken);

    mockMvc
        .perform(delete("/deployments/" + deploymentId))
        .andExpect(header().string(HttpHeaders.RETRY_AFTER, "0"))
        .andExpect(jsonPath("$.code", is(409)))
        .andExpect(jsonPath("$.title", is("Conflict")))
        .andExpect(
            jsonPath("$.message",
                is("The request couldn't be fulfilled because of a concurrent update. Please retry later")));
  }

  @Test
  public void updateDeploymentSuccessfully() throws Exception {

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("cpus", 1);
    DeploymentRequest request = DeploymentRequest
        .builder()
        .parameters(parameters)
        .template("template")
        .callback("http://localhost:8080/callback")
        .keepLastAttempt(false)
        .maxProvidersRetry(1)
        .timeoutMins(5)
        .providerTimeoutMins(10)
        .build();

    String deploymentId = "mmd34483-d937-4578-bfdb-ebe196bf82dd";
    Mockito.doNothing().when(deploymentService).updateDeployment(deploymentId, request, requestedWithToken);

    mockMvc.perform(put("/deployments/" + deploymentId).contentType(MediaType.APPLICATION_JSON)
        .content(JsonUtils.serialize(request))
        .header(HttpHeaders.AUTHORIZATION, OAuth2AccessToken.BEARER_TYPE + " <access token>"))

        .andDo(document("update-deployment", preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint()),
            requestFields(
                fieldWithPath("template")
                    .description("A string containing a TOSCA YAML-formatted template"),
                fieldWithPath("parameters").optional()
                    .description("The input parameters of the deployment (Map of String, Object)"),
                fieldWithPath("callback").description("The deployment callback URL (optional)"),
                fieldWithPath("maxProvidersRetry").description(
                    "The maximum number Cloud providers on which attempt to update the hybrid deployment update (Optional, default unbounded)"),
                fieldWithPath("timeoutMins").description(
                        "Overall timeout value, if provided, must be at least of 1 minute (Optional, default infinite)"),
                fieldWithPath("providerTimeoutMins").description(
                        "Provider timeout value, if provided, must be at least of 1 minute and equal or less than timeoutMins (Optional, default 14400 mins"),
                fieldWithPath("keepLastAttempt").description(
                    "Whether the Orchestrator, in case of failure, will keep the resources of the last update attempt or not (Optional, default false)"))));

  }

  @Test
  public void createDeploymentWithoutCallbackSuccessfully() throws Exception {

    DeploymentRequest request = DeploymentRequest
        .builder()
        .template("template")
        .build();

    Mockito.when(deploymentService.createDeployment(request))
        .thenReturn(ControllerTestUtils.createDeployment());

    mockMvc
        .perform(post("/deployments").contentType(MediaType.APPLICATION_JSON)
            .content(JsonUtils.serialize(request)))
        .andExpect(status().isCreated())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.links[0].rel", is("self")));
  }

  @Test
  public void createDeploymentWithCallbackUnsuccessfully() throws Exception {
    DeploymentRequest request = DeploymentRequest
        .builder()
        .callback("httptest.com")
        .template("template")
        .build();
    mockMvc
        .perform(post("/deployments").contentType(MediaType.APPLICATION_JSON)
            .content(JsonUtils.serialize(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void createDeploymentBadRequest() throws Exception {

    mockMvc.perform(post("/deployments").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void deleteDeployment() throws Exception {

    String deploymentId = "mmd34483-d937-4578-bfdb-ebe196bf82dd";
    Mockito.doNothing().when(deploymentService).deleteDeployment(deploymentId, requestedWithToken);

    mockMvc
        .perform(delete("/deployments/" + deploymentId).header(HttpHeaders.AUTHORIZATION,
            OAuth2AccessToken.BEARER_TYPE + " <access token>"))
        .andExpect(status().isNoContent())
        .andDo(document("delete-deployment", preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint())));

  }

  @Test
  public void deleteDeploymentWithConflict() throws Exception {

    String deploymentId = "mmd34483-d937-4578-bfdb-ebe196bf82dd";
    Mockito.doThrow(new ConflictException("Cannot delete a deployment in DELETE_IN_PROGRESS state"))
        .when(deploymentService)
        .deleteDeployment(deploymentId, requestedWithToken);

    mockMvc.perform(delete("/deployments/" + deploymentId)).andExpect(status().isConflict());
  }

  @Test
  public void deleteDeploymentNotFound() throws Exception {

    String deploymentId = "mmd34483-d937-4578-bfdb-ebe196bf82dd";
    Mockito.doThrow(new NotFoundException("The deployment <not-found> doesn't exist"))
        .when(deploymentService)
        .deleteDeployment(deploymentId, requestedWithToken);

    mockMvc.perform(delete("/deployments/" + deploymentId))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code", is(404)))
        .andExpect(jsonPath("$.title", is("Not Found")))
        .andExpect(jsonPath("$.message", is("The deployment <not-found> doesn't exist")));
  }

}
