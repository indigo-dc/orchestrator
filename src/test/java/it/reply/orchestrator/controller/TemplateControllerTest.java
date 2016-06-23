package it.reply.orchestrator.controller;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;

import es.upv.i3m.grycap.file.NoNullOrEmptyFile;
import es.upv.i3m.grycap.file.Utf8File;

import it.reply.orchestrator.config.specific.WebAppConfigurationAware;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.Charset;
import java.nio.file.Paths;

@DatabaseTearDown("/data/database-empty.xml")
public class TemplateControllerTest extends WebAppConfigurationAware {

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;
  @Rule
  public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation(
      "target/generated-snippets");

  private final MediaType applicationJsonUtf8 = new MediaType(MediaType.APPLICATION_JSON.getType(),
      MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

  private final String templatePath = "./src/test/resources/tosca/galaxy_tosca.yaml";
  private final String deploymentId = "0748fbe9-6c1d-4298-b88f-06188734ab42";

  /**
   * Test environment setup.
   */
  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    mockMvc = MockMvcBuilders.webAppContextSetup(wac)
        .apply(documentationConfiguration(this.restDocumentation)).dispatchOptions(true).build();
  }

  @Test
  @DatabaseSetup("/data/database-resource-init.xml")
  public void getTemplate() throws Exception {

    String template = new NoNullOrEmptyFile(new Utf8File(Paths.get(templatePath))).read();
    MvcResult result = mockMvc.perform(get("/deployments/" + deploymentId + "/template"))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.TEXT_PLAIN))
        .andDo(document("get-template")).andReturn();

    String content = result.getResponse().getContentAsString();
    assertEquals(content, template);

  }

  @Test
  @DatabaseSetup("/data/database-resource-init.xml")
  public void getTemplateNotFoundNotDeployment() throws Exception {
    mockMvc.perform(get("/deployments/aaaaaaaa-bbbb-ccccc-dddd-eeeeeeeeeeee/template"))
        .andExpect(status().isNotFound()).andExpect(content().contentType(applicationJsonUtf8))
        .andExpect(jsonPath("$.code", is(404))).andExpect(jsonPath("$.title", is("Not Found")))
        .andExpect(jsonPath("$.message",
            is("The deployment <aaaaaaaa-bbbb-ccccc-dddd-eeeeeeeeeeee> doesn't exist")));
  }
}
