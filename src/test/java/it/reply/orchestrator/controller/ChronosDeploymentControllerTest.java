package it.reply.orchestrator.controller;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.github.springtestdbunit.annotation.DatabaseTearDown;

import it.reply.orchestrator.config.WebAppConfigurationAware;
import it.reply.orchestrator.dto.request.DeploymentRequest;
import it.reply.orchestrator.util.TestUtil;
import it.reply.orchestrator.utils.CommonUtils;

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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

@DatabaseTearDown("/data/database-empty.xml")
public class ChronosDeploymentControllerTest extends WebAppConfigurationAware {

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
        .apply(documentationConfiguration(this.restDocumentation)).dispatchOptions(true).build();
  }

  @Test
  @Transactional
  public void createDeploymentSuccessfully() throws Exception {

    DeploymentRequest request = new DeploymentRequest();
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("cpus", 1);
    request.setParameters(parameters);
    request.setTemplate(CommonUtils
        .getFileContentAsString("./src/test/resources/tosca/chronos_tosca_minimal.yaml"));
    request.setCallback("http://localhost:8080/callback");

    mockMvc.perform(post("/deployments").contentType(MediaType.APPLICATION_JSON)
        .content(TestUtil.convertObjectToJsonBytes(request)))

        .andDo(document("create-deployment", preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint()),

            requestFields(
                fieldWithPath("template")
                    .description("A string containing a TOSCA YAML-formatted template"),
                fieldWithPath("parameters").optional()
                    .description("The input parameters of the deployment(Map of String, Object)"),
                fieldWithPath("callback").description("The deployment callback URL")),

            responseFields(fieldWithPath("links[]").ignored(),
                fieldWithPath("uuid").description("The unique identifier of a resource"),
                fieldWithPath("creationTime").description(
                    "Creation date-time (http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14)"),
                fieldWithPath("status").description(
                    "The status of the deployment. (http://indigo-dc.github.io/orchestrator/apidocs/it/reply/orchestrator/enums/Status.html)"),
                fieldWithPath("task").description(
                    "The current step of the deployment process. (http://indigo-dc.github.io/orchestrator/apidocs/it/reply/orchestrator/enums/Task.html)"),
                fieldWithPath("outputs").description("The outputs of the TOSCA document"),
                fieldWithPath("callback").ignored(), fieldWithPath("links[]").ignored())));

  }

}
