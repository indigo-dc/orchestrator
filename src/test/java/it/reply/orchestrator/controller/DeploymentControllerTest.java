package it.reply.orchestrator.controller;

import static org.hamcrest.Matchers.is;
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

import es.upv.i3m.grycap.file.Utf8File;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import it.reply.orchestrator.config.WebAppConfigurationAware;
import it.reply.orchestrator.dto.request.DeploymentRequest;
import it.reply.orchestrator.util.TestUtil;

@DatabaseTearDown("/data/database-empty.xml")
public class DeploymentControllerTest extends WebAppConfigurationAware {

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  @Resource
  private Environment env;

  @Rule
  public JUnitRestDocumentation restDocumentation =
      new JUnitRestDocumentation("target/generated-snippets");

  /**
   * Set up test context.
   */
  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    mockMvc = MockMvcBuilders.webAppContextSetup(wac)
        .apply(documentationConfiguration(this.restDocumentation))
        .dispatchOptions(true).build();
  }

  @Test
  @DatabaseSetup("/data/database-init.xml")
  public void getDeployments() throws Exception {

    mockMvc.perform(get("/deployments").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andDo(document("deployments", preprocessResponse(prettyPrint()),

            responseFields(fieldWithPath("links[]").ignored(),

                fieldWithPath("content[].uuid")
                    .description("The unique identifier of a resource"),
                fieldWithPath("content[].creationTime").description(
                    "Creation date-time (http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14)"),
                fieldWithPath("content[].updateTime")
                    .description("Update date-time"),
                fieldWithPath("content[].status").description(
                    "The status of the deployment. (http://indigo-dc.github.io/orchestrator/apidocs/it/reply/orchestrator/enums/Status.html)"),
                fieldWithPath("content[].task").description(
                    "The current step of the deployment process. (http://indigo-dc.github.io/orchestrator/apidocs/it/reply/orchestrator/enums/Task.html)"),
                fieldWithPath("content[].callback").description(
                    "The endpoint used by the orchestrator to notify the progress of the deployment process. (http://endpoint:port)"),
                fieldWithPath("content[].outputs")
                    .description("The outputs of the TOSCA document"),
                fieldWithPath("content[].links[]").ignored(),
                fieldWithPath("page").ignored())));
  }

  @Test
  @DatabaseSetup("/data/database-init-pagination.xml")
  public void getPagedDeployments() throws Exception {

    mockMvc
        .perform(get("/deployments?page=1&size=2")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andDo(document("deployment-paged", preprocessResponse(prettyPrint()),
            links(atomLinks(),
                linkWithRel("first").description("Hyperlink to the first page"),
                linkWithRel("prev")
                    .description("Hyperlink to the previous page"),
                linkWithRel("self").description("Self-referencing hyperlink"),
                linkWithRel("next").description("Self-referencing hyperlink"),
                linkWithRel("last").description("Hyperlink to the last page")),
            responseFields(fieldWithPath("links[]").ignored(),
                fieldWithPath("content").ignored(),
                fieldWithPath("page.").ignored())));

    // .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)));
  }

  @Test
  @DatabaseSetup("/data/database-init.xml")
  public void deploymentsPagination() throws Exception {

    mockMvc.perform(get("/deployments")).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andDo(
            document("deployment-pagination", preprocessResponse(prettyPrint()),
                responseFields(fieldWithPath("links[]").ignored(),
                    fieldWithPath("content[].links[]").ignored(),

                    fieldWithPath("page.size")
                        .description("The size of the page"),
                    fieldWithPath("page.totalElements")
                        .description("The total number of elements"),
                    fieldWithPath("page.totalPages")
                        .description("The total number of the page"),
                    fieldWithPath("page.number")
                        .description("The current page"),
                    fieldWithPath("content[].uuid").ignored(),
                    fieldWithPath("content[].creationTime").ignored(),
                    fieldWithPath("content[].updateTime").ignored(),
                    fieldWithPath("content[].status").ignored(),
                    fieldWithPath("content[].outputs").ignored(),
                    fieldWithPath("content[].task").ignored(),
                    fieldWithPath("content[].callback").ignored())));
  }

  @Test
  @DatabaseSetup("/data/database-init.xml")
  public void getDeploymentSuccessfully() throws Exception {

    mockMvc.perform(get("/deployments/mmd34483-d937-4578-bfdb-ebe196bf82dd"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(
            jsonPath("$.uuid", is("mmd34483-d937-4578-bfdb-ebe196bf82dd")));
  }

  @Test
  @DatabaseSetup("/data/database-init.xml")
  public void deploymentHypermedia() throws Exception {

    mockMvc.perform(get("/deployments/mmd34483-d937-4578-bfdb-ebe196bf82dd"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andDo(document("deployment-hypermedia",
            preprocessResponse(prettyPrint()),
            links(atomLinks(),
                linkWithRel("self").description("Self-referencing hyperlink"),
                linkWithRel("template")
                    .description("Template reference hyperlink"),
                linkWithRel("resources")
                    .description("Resources reference hyperlink")),
            responseFields(
                fieldWithPath("links[].rel").description(
                    "means relationship. In this case, it's a self-referencing hyperlink. More complex systems might include other relationships."),
                fieldWithPath("links[].href").description(
                    "Is a complete URL that uniquely defines the resource."),
                fieldWithPath("uuid").ignored(),
                fieldWithPath("creationTime").ignored(),
                fieldWithPath("updateTime").ignored(),
                fieldWithPath("status").ignored(),
                fieldWithPath("outputs").ignored(),
                fieldWithPath("task").ignored(),
                fieldWithPath("callback").ignored())));
  }

  @Test
  @DatabaseSetup("/data/database-init.xml")
  public void getDeploymentWithOutputSuccessfully() throws Exception {

    mockMvc.perform(get("/deployments/mmd34483-d937-4578-bfdb-ebe196bf82dd"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(
            jsonPath("$.outputs", Matchers.hasEntry("server_ip", "10.0.0.1")))

        .andDo(document("deployment", preprocessResponse(prettyPrint()),

            responseFields(fieldWithPath("links[]").ignored(),

                fieldWithPath("uuid")
                    .description("The unique identifier of a resource"),
                fieldWithPath("creationTime").description(
                    "Creation date-time (http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14)"),
                fieldWithPath("updateTime").description("Update date-time"),
                fieldWithPath("status").description(
                    "The status of the deployment. (http://indigo-dc.github.io/orchestrator/apidocs/it/reply/orchestrator/enums/Status.html)"),
                fieldWithPath("task").description(
                    "The current step of the deployment process. (http://indigo-dc.github.io/orchestrator/apidocs/it/reply/orchestrator/enums/Task.html)"),
                fieldWithPath("callback").description(
                    "The endpoint used by the orchestrator to notify the progress of the deployment process. (http://endpoint:port)"),
                fieldWithPath("outputs")
                    .description("The outputs of the TOSCA document"),
                fieldWithPath("links[]").ignored())));
  }

  @Test
  @DatabaseSetup("/data/database-init.xml")
  public void getDeploymentNotFound() throws Exception {

    mockMvc.perform(get("/deployments/deploymentId"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code", is(404))).andDo(
            document("deployment-not-found", preprocessResponse(prettyPrint()),
                responseFields(
                    fieldWithPath("code").description("The HTTP status code"),
                    fieldWithPath("title").description("The HTTP status name"),
                    fieldWithPath("message").description(
                        "A displayable message describing the error"))));
    // andExpect(jsonPath("$.title", is("Not Found")))
    // .andExpect(jsonPath("$.message", is("The deployment <not-found> doesn't
    // exist")));
  }

  @Test
  @Transactional
  public void createDeploymentSuccessfully() throws Exception {

    DeploymentRequest request = new DeploymentRequest();
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("cpus", 1);
    request.setParameters(parameters);
    request.setTemplate(
        new Utf8File("./src/test/resources/tosca/compute_tosca.yaml").read());
    request.setCallback("http://localhost:8080/callback");
    mockMvc.perform(post("/deployments").contentType(MediaType.APPLICATION_JSON)
        .content(TestUtil.convertObjectToJsonBytes(request)))

        .andDo(document("create-deployment", preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint()),
            requestFields(
                fieldWithPath("template").description(
                    "A string containing a TOSCA YAML-formatted template"),
                fieldWithPath("parameters").optional().description(
                    "The input parameters of the deployment(Map of String, Object)"),
                fieldWithPath("callback")
                    .description("The deployment callback URL")),
            responseFields(fieldWithPath("links[]").ignored(),
                fieldWithPath("uuid")
                    .description("The unique identifier of a resource"),
                fieldWithPath("creationTime").description(
                    "Creation date-time (http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14)"),
                fieldWithPath("status").description(
                    "The status of the deployment. (http://indigo-dc.github.io/orchestrator/apidocs/it/reply/orchestrator/enums/Status.html)"),
                fieldWithPath("task").description(
                    "The current step of the deployment process. (http://indigo-dc.github.io/orchestrator/apidocs/it/reply/orchestrator/enums/Task.html)"),
                fieldWithPath("outputs")
                    .description("The outputs of the TOSCA document"),
                fieldWithPath("callback").ignored(),
                fieldWithPath("links[]").ignored())));

    // .andExpect(status().isCreated())
    // .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    // .andExpect(jsonPath("$.links[0].rel", is("self")));
  }

  @Test
  @Transactional
  @DatabaseSetup("/data/database-init.xml")
  public void updateDeploymentSuccessfully() throws Exception {

    DeploymentRequest request = new DeploymentRequest();
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("cpus", 1);
    request.setParameters(parameters);
    request.setTemplate(
        new Utf8File("./src/test/resources/tosca/galaxy_tosca.yaml").read());
    request.setCallback("http://localhost:8080/callback");
    mockMvc.perform(put("/deployments/mmd34483-d937-4578-bfdb-ebe196bf82dd")
        .contentType(MediaType.APPLICATION_JSON)
        .content(TestUtil.convertObjectToJsonBytes(request)))

        .andDo(document("update-deployment", preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint()),
            requestFields(
                fieldWithPath("template").description(
                    "A string containing a TOSCA YAML-formatted template"),
                fieldWithPath("parameters").optional().description(
                    "The input parameters of the deployment (Map of String, Object)"),
                fieldWithPath("callback")
                    .description("The deployment callback URL"))));

  }

  @Test
  @Transactional
  public void createDeploymentWithCallbackSuccessfully() throws Exception {

    DeploymentRequest request = new DeploymentRequest();
    String callback = "http://localhost";
    request.setCallback(callback);
    request.setTemplate(
        new Utf8File("./src/test/resources/tosca/galaxy_tosca.yaml").read());
    mockMvc
        .perform(post("/deployments").contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(request)))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.callback", is(callback)))
        .andExpect(jsonPath("$.links[0].rel", is("self")));
  }

  @Test
  @Transactional
  public void createDeploymentWithCallbackUnsuccessfully() throws Exception {
    DeploymentRequest request = new DeploymentRequest();
    String callback = "httptest.com";
    request.setCallback(callback);
    request.setTemplate(
        new Utf8File("./src/test/resources/tosca/galaxy_tosca.yaml").read());
    mockMvc
        .perform(post("/deployments").contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void createDeploymentBadRequest() throws Exception {

    mockMvc
        .perform(post("/deployments").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DatabaseSetup("/data/database-init.xml")
  public void deleteDeployment() throws Exception {

    mockMvc.perform(delete("/deployments/mmd34483-d937-4578-bfdb-ebe196bf82dd"))
        .andExpect(status().isNoContent())
        .andDo(document("delete-deployment", preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint())));

  }

  @Test
  public void deleteDeploymentNotFound() throws Exception {

    mockMvc.perform(delete("/deployments/not-found"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code", is(404)))
        .andExpect(jsonPath("$.title", is("Not Found"))).andExpect(jsonPath(
            "$.message", is("The deployment <not-found> doesn't exist")));
  }
}
