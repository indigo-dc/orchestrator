package it.reply.orchestrator.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

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
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;

import es.upv.i3m.grycap.file.FileIO;
import it.reply.orchestrator.config.WebAppConfigurationAware;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dto.im.InfrastructureStatus;
import it.reply.orchestrator.dto.request.DeploymentRequest;
import it.reply.orchestrator.resource.DeploymentResource;
import it.reply.orchestrator.service.DeploymentService;
import it.reply.orchestrator.util.TestUtil;

@DatabaseTearDown("/data/database-empty.xml")
public class DeploymentControllerTest extends WebAppConfigurationAware {

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  @Resource
  private Environment env;

  @Autowired
  private DeploymentController deploymentController;

  @Autowired
  private DeploymentService deploymentService;

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
  public void getDeploymentSuccessfully() throws Exception {

    mockMvc.perform(get("/deployments/mmd34483-d937-4578-bfdb-ebe196bf82dd"))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.uuid", is("mmd34483-d937-4578-bfdb-ebe196bf82dd")));

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
  public void createDeploymentSuccessfully() throws Exception {

    DeploymentRequest request = new DeploymentRequest();
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
