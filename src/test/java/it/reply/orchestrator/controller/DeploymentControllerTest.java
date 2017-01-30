package it.reply.orchestrator.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.atomLinks;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.reply.orchestrator.dal.entity.AbstractResourceEntity;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dto.request.DeploymentRequest;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.exception.GlobalControllerExceptionHandler;
import it.reply.orchestrator.exception.http.ConflictException;
import it.reply.orchestrator.exception.http.NotFoundException;
import it.reply.orchestrator.resource.DeploymentResourceAssembler;
import it.reply.orchestrator.service.DeploymentService;
import it.reply.orchestrator.util.TestUtil;
import it.reply.utils.json.JsonUtility;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PagedResourcesAssemblerArgumentResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentation;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jersey.repackaged.com.google.common.collect.Maps;

@RunWith(MockitoJUnitRunner.class)
public class DeploymentControllerTest {

  private MockMvc mockMvc;

  @InjectMocks
  private DeploymentController deploymentController = new DeploymentController();

  @Mock
  private DeploymentService deploymentService;

  @Spy
  private HateoasPageableHandlerMethodArgumentResolver pageableArgumentResolver;

  @Spy
  private DeploymentResourceAssembler deploymentResourceAssembler;

  @Spy
  private PagedResourcesAssemblerArgumentResolver pagedResourcesAssemblerArgumentResolver =
      new PagedResourcesAssemblerArgumentResolver(pageableArgumentResolver, null);

  @Spy
  private GlobalControllerExceptionHandler globalControllerExceptionHandler;

  @Rule
  public RestDocumentation restDocumentation = new RestDocumentation("target/generated-snippets");

  /**
   * Set up test context.
   */
  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    mockMvc = MockMvcBuilders.standaloneSetup(deploymentController)
        .setControllerAdvice(globalControllerExceptionHandler)
        .setCustomArgumentResolvers(pageableArgumentResolver,
            pagedResourcesAssemblerArgumentResolver)
        .apply(documentationConfiguration(this.restDocumentation)).dispatchOptions(true).build();
  }

  @Test
  public void getOrchestrator() throws Exception {
    mockMvc.perform(get("/").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
  }

  @Test
  public void getInfo() throws Exception {
    mockMvc.perform(get("/info").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
  }

  @Test
  public void getDeployments() throws Exception {

    List<Deployment> deployments = ControllerTestUtils.createDeployments(2, true);
    deployments.get(0).setStatus(Status.CREATE_FAILED);
    deployments.get(0).setStatusReason("Some reason");
    deployments.get(1).setStatus(Status.CREATE_COMPLETE);
    Pageable pageable = ControllerTestUtils.createDefaultPageable();
    Mockito.when(deploymentService.getDeployments(pageable))
        .thenReturn(new PageImpl<Deployment>(deployments));

    mockMvc
        .perform(get("/deployments").accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, OAuth2AccessToken.BEARER_TYPE + " <access token>"))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andDo(document("authentication",
            requestHeaders(
                headerWithName(HttpHeaders.AUTHORIZATION).description("OAuth2 bearer token"))))
        .andDo(document("deployments", preprocessResponse(prettyPrint()),

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
                fieldWithPath("content[].callback").description(
                    "The endpoint used by the orchestrator to notify the progress of the deployment process."),
                fieldWithPath("content[].outputs").description("The outputs of the TOSCA document"),
                fieldWithPath("content[].links[]").ignored(), fieldWithPath("page").ignored())));
  }

  @Test
  public void getPagedDeployments() throws Exception {

    List<Deployment> deployments = ControllerTestUtils.createDeployments(5, true);
    Pageable pageable =
        new PageRequest(1, 2, new Sort(Direction.DESC, AbstractResourceEntity.CREATED_COLUMN_NAME));
    Mockito.when(deploymentService.getDeployments(pageable))
        .thenReturn(new PageImpl<Deployment>(deployments, pageable, deployments.size()));

    mockMvc
        .perform(get("/deployments?page=1&size=2").accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, OAuth2AccessToken.BEARER_TYPE + " <access token>"))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
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

    List<Deployment> deployments = ControllerTestUtils.createDeployments(5, true);
    Pageable pageable = ControllerTestUtils.createDefaultPageable();
    Mockito.when(deploymentService.getDeployments(pageable))
        .thenReturn(new PageImpl<Deployment>(deployments));

    mockMvc
        .perform(get("/deployments").header(HttpHeaders.AUTHORIZATION,
            OAuth2AccessToken.BEARER_TYPE + " <access token>"))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
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

    mockMvc.perform(get("/deployments/" + deploymentId)).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.uuid", is(deploymentId)));
  }

  @Test
  public void deploymentHypermedia() throws Exception {

    String deploymentId = "mmd34483-d937-4578-bfdb-ebe196bf82dd";
    Deployment deployment = ControllerTestUtils.createDeployment(deploymentId);
    Mockito.when(deploymentService.getDeployment(deploymentId)).thenReturn(deployment);

    mockMvc
        .perform(get("/deployments/" + deploymentId).header(HttpHeaders.AUTHORIZATION,
            OAuth2AccessToken.BEARER_TYPE + " <access token>"))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
    Map<String, String> outputs = Maps.newHashMap();
    String key = "server_ip";
    String value = "10.0.0.1";
    outputs.put(key, JsonUtility.serializeJson(value));
    deployment.setOutputs(outputs);
    deployment.setStatus(Status.CREATE_FAILED);
    deployment.setStatusReason("Some reason");
    Mockito.when(deploymentService.getDeployment(deploymentId)).thenReturn(deployment);

    mockMvc
        .perform(get("/deployments/" + deploymentId).header(HttpHeaders.AUTHORIZATION,
            OAuth2AccessToken.BEARER_TYPE + " <access token>"))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code", is(404)))
        .andDo(document("deployment-not-found", preprocessResponse(prettyPrint()),
            responseFields(fieldWithPath("code").description("The HTTP status code"),
                fieldWithPath("title").description("The HTTP status name"),
                fieldWithPath("message")
                    .description("A displayable message describing the error"))))
        .andExpect(jsonPath("$.title", is("Not Found")))
        .andExpect(jsonPath("$.message", is("Message")));
  }

  @Test
  public void createDeploymentUnsupportedMediaType() throws Exception {
    DeploymentRequest request = new DeploymentRequest();
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("cpus", 1);
    request.setParameters(parameters);
    request.setTemplate("template");
    request.setCallback("http://localhost:8080/callback");
    mockMvc
        .perform(post("/deployments").contentType(MediaType.TEXT_PLAIN)
            .content(TestUtil.convertObjectToJsonBytes(request)))
        .andExpect(status().isUnsupportedMediaType());

  }

  @Test
  public void createDeploymentSuccessfully() throws Exception {

    DeploymentRequest request = new DeploymentRequest();
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("cpus", 1);
    request.setParameters(parameters);
    request.setTemplate("template");
    request.setCallback("http://localhost:8080/callback");

    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.setCallback(request.getCallback());
    deployment.setStatus(Status.CREATE_IN_PROGRESS);
    Mockito.when(deploymentService.createDeployment(request)).thenReturn(deployment);

    mockMvc.perform(post("/deployments").contentType(MediaType.APPLICATION_JSON)
        .content(TestUtil.convertObjectToJsonBytes(request))
        .header(HttpHeaders.AUTHORIZATION, OAuth2AccessToken.BEARER_TYPE + " <access token>"))

        .andDo(document("create-deployment", preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint()),
            requestFields(
                fieldWithPath("template")
                    .description("A string containing a TOSCA YAML-formatted template"),
                fieldWithPath("parameters").optional()
                    .description("The input parameters of the deployment(Map of String, Object)"),
                fieldWithPath("callback").description("The deployment callback URL (optional)")),
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
    DeploymentRequest request = new DeploymentRequest();
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("cpus", 1);
    request.setParameters(parameters);
    request.setTemplate("template");
    request.setCallback("http://localhost:8080/callback");

    String deploymentId = "mmd34483-d937-4578-bfdb-ebe196bf82dd";
    Mockito.doThrow(new NotFoundException("Message")).when(deploymentService)
        .updateDeployment(deploymentId, request);

    mockMvc
        .perform(put("/deployments/" + deploymentId).contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(request)))
        .andExpect(jsonPath("$.code", is(404))).andExpect(jsonPath("$.title", is("Not Found")))
        .andExpect(jsonPath("$.message", is("Message")));
  }

  @Test
  public void updateDeploymentDeleteInProgress() throws Exception {
    DeploymentRequest request = new DeploymentRequest();
    request.setTemplate("template");

    String deploymentId = "mmd34483-d937-4578-bfdb-ebe196bf82dd";
    Mockito.doThrow(new ConflictException("Cannot update a deployment in DELETE_IN_PROGRESS state"))
        .when(deploymentService).updateDeployment(deploymentId, request);

    mockMvc
        .perform(put("/deployments/" + deploymentId).contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(request)))
        .andExpect(jsonPath("$.code", is(409))).andExpect(jsonPath("$.title", is("Conflict")))
        .andExpect(
            jsonPath("$.message", is("Cannot update a deployment in DELETE_IN_PROGRESS state")));
  }

  @Test
  public void updateDeploymentSuccessfully() throws Exception {

    DeploymentRequest request = new DeploymentRequest();
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("cpus", 1);
    request.setParameters(parameters);
    request.setTemplate("template");
    request.setCallback("http://localhost:8080/callback");

    String deploymentId = "mmd34483-d937-4578-bfdb-ebe196bf82dd";
    Mockito.doNothing().when(deploymentService).updateDeployment(deploymentId, request);

    mockMvc.perform(put("/deployments/" + deploymentId).contentType(MediaType.APPLICATION_JSON)
        .content(TestUtil.convertObjectToJsonBytes(request))
        .header(HttpHeaders.AUTHORIZATION, OAuth2AccessToken.BEARER_TYPE + " <access token>"))

        .andDo(document("update-deployment", preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint()),
            requestFields(
                fieldWithPath("template")
                    .description("A string containing a TOSCA YAML-formatted template"),
                fieldWithPath("parameters").optional()
                    .description("The input parameters of the deployment (Map of String, Object)"),
                fieldWithPath("callback").description("The deployment callback URL (optional)"))));

  }

  @Test
  public void createDeploymentWithoutCallbackSuccessfully() throws Exception {

    DeploymentRequest request = new DeploymentRequest();
    request.setTemplate("template");

    Mockito.when(deploymentService.createDeployment(request))
        .thenReturn(ControllerTestUtils.createDeployment());

    mockMvc
        .perform(post("/deployments").contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(request)))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.links[0].rel", is("self")));
  }

  @Test
  public void createDeploymentWithCallbackUnsuccessfully() throws Exception {
    DeploymentRequest request = new DeploymentRequest();
    String callback = "httptest.com";
    request.setCallback(callback);
    request.setTemplate("template");
    mockMvc
        .perform(post("/deployments").contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(request)))
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
    Mockito.doNothing().when(deploymentService).deleteDeployment(deploymentId);

    mockMvc
        .perform(delete("/deployments/" + deploymentId).header(HttpHeaders.AUTHORIZATION,
            OAuth2AccessToken.BEARER_TYPE + " <access token>"))
        .andExpect(status().isNoContent()).andDo(document("delete-deployment",
            preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));

  }

  @Test
  public void deleteDeploymentWithConflict() throws Exception {

    String deploymentId = "mmd34483-d937-4578-bfdb-ebe196bf82dd";
    Mockito.doThrow(new ConflictException("Cannot delete a deployment in DELETE_IN_PROGRESS state"))
        .when(deploymentService).deleteDeployment(deploymentId);

    mockMvc.perform(delete("/deployments/" + deploymentId)).andExpect(status().isConflict());
  }

  @Test
  public void deleteDeploymentNotFound() throws Exception {

    String deploymentId = "mmd34483-d937-4578-bfdb-ebe196bf82dd";
    Mockito.doThrow(new NotFoundException("The deployment <not-found> doesn't exist"))
        .when(deploymentService).deleteDeployment(deploymentId);

    mockMvc.perform(delete("/deployments/" + deploymentId))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code", is(404))).andExpect(jsonPath("$.title", is("Not Found")))
        .andExpect(jsonPath("$.message", is("The deployment <not-found> doesn't exist")));
  }

}
