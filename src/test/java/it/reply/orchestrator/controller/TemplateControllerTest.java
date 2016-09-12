package it.reply.orchestrator.controller;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import es.upv.i3m.grycap.file.NoNullOrEmptyFile;
import es.upv.i3m.grycap.file.Utf8File;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.exception.GlobalControllerExceptionHandler;
import it.reply.orchestrator.exception.http.NotFoundException;
import it.reply.orchestrator.service.TemplateService;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.Charset;
import java.nio.file.Paths;

public class TemplateControllerTest {

  private MockMvc mockMvc;

  @InjectMocks
  private TemplateController templateController = new TemplateController();

  @Mock
  private TemplateService templateService;

  @Spy
  private GlobalControllerExceptionHandler globalControllerExceptionHandler;

  @Rule
  public JUnitRestDocumentation restDocumentation = ControllerTestUtils.getRestDocumentationRule();

  private final MediaType applicationJsonUtf8 = new MediaType(MediaType.APPLICATION_JSON.getType(),
      MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

  private final String templatePath = "./src/test/resources/tosca/galaxy_tosca.yaml";

  /**
   * Test environment setup.
   */
  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    mockMvc = MockMvcBuilders.standaloneSetup(templateController)
        .setControllerAdvice(globalControllerExceptionHandler)
        .apply(documentationConfiguration(this.restDocumentation)).dispatchOptions(true).build();
  }

  @Test
  public void getTemplate() throws Exception {

    Deployment deployment = ControllerTestUtils.createDeployment();

    String template = new NoNullOrEmptyFile(new Utf8File(Paths.get(templatePath))).read();
    deployment.setTemplate(template);

    Mockito.when(templateService.getTemplate(deployment.getId()))
        .thenReturn(deployment.getTemplate());

    MvcResult result = mockMvc.perform(get("/deployments/" + deployment.getId() + "/template"))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.TEXT_PLAIN))
        .andDo(document("get-template")).andReturn();

    String content = result.getResponse().getContentAsString();
    assertEquals(content, template);

  }

  @Test
  public void getTemplateNotFoundNotDeployment() throws Exception {

    Deployment deployment = ControllerTestUtils.createDeployment();

    Mockito.when(templateService.getTemplate(deployment.getId())).thenThrow(
        new NotFoundException("The deployment <" + deployment.getId() + "> doesn't exist"));

    mockMvc.perform(get("/deployments/" + deployment.getId() + "/template"))
        .andExpect(status().isNotFound()).andExpect(content().contentType(applicationJsonUtf8))
        .andExpect(jsonPath("$.code", is(404))).andExpect(jsonPath("$.title", is("Not Found")))
        .andExpect(
            jsonPath("$.message", is("The deployment <" + deployment.getId() + "> doesn't exist")));
  }
}
