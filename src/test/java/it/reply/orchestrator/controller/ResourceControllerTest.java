package it.reply.orchestrator.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;

import it.reply.orchestrator.config.WebAppConfigurationAware;

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
import org.springframework.web.context.WebApplicationContext;

import javax.annotation.Resource;

@DatabaseTearDown("/data/database-empty.xml")
@DatabaseSetup("/data/database-resource-init.xml")
public class ResourceControllerTest extends WebAppConfigurationAware {

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  @Resource
  private Environment env;
  @Rule
  public JUnitRestDocumentation restDocumentation =
      new JUnitRestDocumentation("target/generated-snippets");

  private final String deploymentId = "0748fbe9-6c1d-4298-b88f-06188734ab42";
  private final String resourceId = "mmd34483-d937-4578-bfdb-ebe196bf82dd";

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    mockMvc =
        MockMvcBuilders.webAppContextSetup(wac)
            .apply(documentationConfiguration(this.restDocumentation)).dispatchOptions(true)
            .build();
  }

  @Test
  @DatabaseSetup("/data/database-resource-init.xml")
  public void getResources() throws Exception {

    mockMvc
        .perform(
            get("/deployments/" + deploymentId + "/resources").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(2)))
        .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(2)))
        .andExpect(jsonPath("$.page.totalElements", equalTo(2)))
        .andExpect(jsonPath("$.links[0].rel", is("self"))).andExpect(
            jsonPath("$.links[0].href", endsWith("/deployments/" + deploymentId + "/resources")))

        .andDo(document("resources", preprocessResponse(prettyPrint()),
            responseFields(fieldWithPath("links[]").ignored(),
                fieldWithPath("content[].uuid").description("The unique identifier of a resource"),
                fieldWithPath("content[].creationTime").description(
                    "Creation date-time (http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14)"),
            fieldWithPath("content[].status").description(
                "The status of the deployment. (http://indigo-dc.github.io/orchestrator/apidocs/it/reply/orchestrator/enums/Status.html)"),
            fieldWithPath("content[].statusReason").optional()
                .description("The description of the state"),
            fieldWithPath("content[].toscaNodeType").optional()
                .description("The type of the represented TOSCA node"),
            fieldWithPath("content[].requiredBy")
                .description("A list of nodes that require this resource"),
            fieldWithPath("content[].links[]").ignored(), fieldWithPath("page").ignored())));

  }

  @Test
  public void getResourcesNotFoundNotDeployment() throws Exception {
    mockMvc.perform(get("/deployments/aaaaaaaa-bbbb-ccccc-dddd-eeeeeeeeeeee/resources"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code", is(404))).andExpect(jsonPath("$.title", is("Not Found")))
        .andExpect(jsonPath("$.message",
            is("The deployment <aaaaaaaa-bbbb-ccccc-dddd-eeeeeeeeeeee> doesn't exist")));
  }

  @Test
  @DatabaseSetup("/data/database-resource-init.xml")
  public void getResourceByIdAndDeploymentIdSuccesfully() throws Exception {
    mockMvc.perform(get("/deployments/" + deploymentId + "/resources/" + resourceId))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.uuid", equalTo(resourceId)))
        .andExpect(jsonPath("$.links[1].rel", equalTo("self")))
        .andExpect(jsonPath("$.links[1].href",
            endsWith("/deployments/" + deploymentId + "/resources/" + resourceId)))
        .andDo(document("get-resource", preprocessResponse(prettyPrint()),
            responseFields(fieldWithPath("uuid").description("The unique identifier of a resource"),
                fieldWithPath("creationTime").description(
                    "Creation date-time (http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14)"),
            fieldWithPath("status").description(
                "The status of the deployment. (http://indigo-dc.github.io/orchestrator/apidocs/it/reply/orchestrator/enums/Status.html)"),
            fieldWithPath("statusReason").optional().description("The description of the state"),
            fieldWithPath("toscaNodeType").optional()
                .description("The type of the represented TOSCA node"),
            fieldWithPath("requiredBy").description("A list of nodes that require this resource"),
            fieldWithPath("links[]").ignored())));
  }
}
