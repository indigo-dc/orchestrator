package it.reply.orchestrator.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.github.springtestdbunit.annotation.DatabaseSetup;

import es.upv.i3m.grycap.file.FileIO;
import it.reply.orchestrator.config.WebAppConfigurationAware;
import it.reply.orchestrator.dto.request.Deployment;
import it.reply.orchestrator.util.TestUtil;

public class DeploymentControllerTest extends WebAppConfigurationAware {

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  @Resource
  private Environment env;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    mockMvc = MockMvcBuilders.webAppContextSetup(wac).dispatchOptions(true).build();
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
  public void getDeployment() throws Exception {

    mockMvc.perform(get("/deployments/mmd34483-d937-4578-bfdb-ebe196bf82dd"))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.uuid", is("mmd34483-d937-4578-bfdb-ebe196bf82dd")))
        .andExpect(jsonPath("$.uuid", is("mmd34483-d937-4578-bfdb-ebe196bf82dd")));

  }

  @Test
  public void createDeploymentSuccessfully() throws Exception {

    Deployment request = new Deployment();
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put("test-key", "test-value");
    request.setParameters(parameters);
    request.setTemplate(FileIO.readUTF8File("./src/test/resources/tosca/galaxy_tosca.yaml"));
    mockMvc
        .perform(post("/deployments").contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(request)))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.links[0].rel", is("self")));

  }
}
