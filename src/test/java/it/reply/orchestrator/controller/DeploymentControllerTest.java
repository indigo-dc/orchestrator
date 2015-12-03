package it.reply.orchestrator.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import it.reply.orchestrator.config.WebAppConfigurationAware;
import it.reply.orchestrator.dto.common.Deployment;
import it.reply.orchestrator.dto.request.DeploymentRequest;
import it.reply.orchestrator.service.DeploymentService;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

public class DeploymentControllerTest extends WebAppConfigurationAware {

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  @InjectMocks
  private DeploymentController greetingController;

  @Autowired
  private DeploymentService deploymentService;

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);
    mockMvc = MockMvcBuilders.webAppContextSetup(wac).dispatchOptions(true).build();

    Collection<Deployment> deployments = deploymentService.getDeployments().values();
    for (Deployment d : deployments) {
      deploymentService.deleteDeployment(d.getId());
    }
    deploymentService.createDeployment(new DeploymentRequest());
    deploymentService.createDeployment(new DeploymentRequest());
  }

  @Test
  public void sayHello() throws Exception {

    mockMvc.perform(get("/").accept(MediaType.TEXT_PLAIN)).andExpect(status().isOk())
        .andExpect(content().string("hello"));

  }

  @Test
  public void getDeployments() throws Exception {

    mockMvc.perform(get("/deployments").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.deployments", org.hamcrest.Matchers.hasSize(2)));

  }

}
