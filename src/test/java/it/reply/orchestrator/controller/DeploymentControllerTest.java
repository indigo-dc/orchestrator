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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

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

import es.upv.i3m.grycap.file.FileIO;
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
  public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation(
      "target/generated-snippets");

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    mockMvc = MockMvcBuilders.webAppContextSetup(wac).dispatchOptions(true)
        .apply(documentationConfiguration(this.restDocumentation))
        .alwaysDo(document("{method-name}/{step}/", preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint())))
        .build();
  }

  @Test
  public void orchestratorSetUp() throws Exception {
    mockMvc.perform(get("/")).andExpect(status().isOk()).andDo(document("index"));
  }

  @Test
  @DatabaseSetup("/data/database-init.xml")
  public void getDeployments() throws Exception {

    mockMvc.perform(get("/deployments").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)));
  }

  @Test
  @DatabaseSetup("/data/database-init.xml")
  public void getDeploymentSuccessfully() throws Exception {

    mockMvc.perform(get("/deployments/mmd34483-d937-4578-bfdb-ebe196bf82dd"))
        .andExpect(status().isOk())
        .andDo(document("deployment-get", preprocessResponse(prettyPrint()),
            links(atomLinks(), linkWithRel("self").description("This deployment"))));

    // .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    // .andExpect(jsonPath("$.uuid", is("mmd34483-d937-4578-bfdb-ebe196bf82dd")));
  }

  @Test
  @DatabaseSetup("/data/database-init.xml")
  public void getDeploymentWithOutputSuccessfully() throws Exception {

    mockMvc.perform(get("/deployments/mmd34483-d937-4578-bfdb-ebe196bf82dd"))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.outputs", Matchers.hasEntry("server_ip", "[10.0.0.1]")));
  }

  @Test
  @DatabaseSetup("/data/database-init.xml")
  public void getDeploymentNotFound() throws Exception {

    mockMvc.perform(get("/deployments/not-found")).andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code", is(404))).andExpect(jsonPath("$.title", is("Not Found")))
        .andExpect(jsonPath("$.message", is("The deployment <not-found> doesn't exist")));
  }

  @Test
  @Transactional
  public void createDeploymentSuccessfully() throws Exception {

    DeploymentRequest request = new DeploymentRequest();
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("test-string", "test-string");
    parameters.put("test-int", 1);
    request.setParameters(parameters);
    request.setTemplate(FileIO.readUTF8File("./src/test/resources/tosca/galaxy_tosca.yaml"));
    mockMvc
        .perform(post("/deployments").contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(request)))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.links[0].rel", is("self")));
  }

  @Test
  @Transactional
  public void createDeploymentWithCallbackSuccessfully() throws Exception {

    DeploymentRequest request = new DeploymentRequest();
    String callback = "http://localhost";
    request.setCallback(callback);
    request.setTemplate(FileIO.readUTF8File("./src/test/resources/tosca/galaxy_tosca.yaml"));
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
    request.setTemplate(FileIO.readUTF8File("./src/test/resources/tosca/galaxy_tosca.yaml"));
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
  public void deleteDeploymentNotFound() throws Exception {

    mockMvc.perform(delete("/deployments/not-found"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code", is(404))).andExpect(jsonPath("$.title", is("Not Found")))
        .andExpect(jsonPath("$.message", is("The deployment <not-found> doesn't exist")));
  }
}
