package it.reply.orchestrator.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collection;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;

import it.reply.orchestrator.config.WebAppConfigurationAware;
import it.reply.orchestrator.dto.im.InfrastructureStatus;
import it.reply.orchestrator.resource.BaseResource;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.MediaType;
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

  private final String deploymentId = "0748fbe9-6c1d-4298-b88f-06188734ab42";
  private final String resourceId = "mmd34483-d937-4578-bfdb-ebe196bf82dd";

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    mockMvc = MockMvcBuilders.webAppContextSetup(wac).dispatchOptions(true).build();
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
            jsonPath("$.links[0].href", endsWith("/deployments/" + deploymentId + "/resources")));

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
            endsWith("/deployments/" + deploymentId + "/resources/" + resourceId)));
  }
}
