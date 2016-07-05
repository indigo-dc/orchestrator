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

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.exception.GlobalControllerExceptionHandler;
import it.reply.orchestrator.exception.http.NotFoundException;
import it.reply.orchestrator.resource.BaseResourceAssembler;
import it.reply.orchestrator.service.ResourceService;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PagedResourcesAssemblerArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

public class ResourceControllerTest {

  private MockMvc mockMvc;

  @InjectMocks
  private ResourceController resourceController = new ResourceController();

  @Mock
  private ResourceService resourceService;

  @Spy
  private HateoasPageableHandlerMethodArgumentResolver pageableArgumentResolver;

  @Spy
  private BaseResourceAssembler baseResourceAssembler;

  @Spy
  private PagedResourcesAssemblerArgumentResolver pagedResourcesAssemblerArgumentResolver =
      new PagedResourcesAssemblerArgumentResolver(pageableArgumentResolver, null);

  @Spy
  private GlobalControllerExceptionHandler globalControllerExceptionHandler;

  @Rule
  public JUnitRestDocumentation restDocumentation =
      new JUnitRestDocumentation("target/generated-snippets");

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    mockMvc = MockMvcBuilders.standaloneSetup(resourceController)
        .setControllerAdvice(globalControllerExceptionHandler)
        .setCustomArgumentResolvers(pageableArgumentResolver,
            pagedResourcesAssemblerArgumentResolver)
        .apply(documentationConfiguration(this.restDocumentation)).dispatchOptions(true).build();
  }

  @Test
  public void getResources() throws Exception {
    Pageable pageable = ControllerTestUtils.createDefaultPageable();
    Deployment deployment = ControllerTestUtils.createDeployment();
    List<Resource> resources = ControllerTestUtils.createResources(deployment, 2);
    Mockito.when(resourceService.getResources(deployment.getId(), pageable))
        .thenReturn(new PageImpl<Resource>(resources));

    mockMvc
        .perform(get("/deployments/" + deployment.getId() + "/resources")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(2)))
        .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(2)))
        .andExpect(jsonPath("$.page.totalElements", equalTo(2)))
        .andExpect(jsonPath("$.links[0].rel", is("self"))).andExpect(jsonPath("$.links[0].href",
            endsWith("/deployments/" + deployment.getId() + "/resources")))

        .andDo(document("resources", preprocessResponse(prettyPrint()),
            responseFields(fieldWithPath("links[]").ignored(),
                fieldWithPath("content[].uuid").description("The unique identifier of a resource"),
                fieldWithPath("content[].creationTime").description(
                    "Creation date-time (http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14)"),
                fieldWithPath("content[].state").description(
                    "The status of the resource. (http://indigo-dc.github.io/orchestrator/apidocs/it/reply/orchestrator/enums/NodeStates.html)"),
                fieldWithPath("content[].toscaNodeType").optional()
                    .description("The type of the represented TOSCA node"),
                fieldWithPath("content[].requiredBy")
                    .description("A list of nodes that require this resource"),
                fieldWithPath("content[].links[]").ignored(), fieldWithPath("page").ignored())));

  }

  @Test
  public void getResourcesNotFoundNotDeployment() throws Exception {
    Pageable pageable = ControllerTestUtils.createDefaultPageable();
    Deployment deployment = ControllerTestUtils.createDeployment();
    Mockito.when(resourceService.getResources(deployment.getId(), pageable)).thenThrow(
        new NotFoundException("The deployment <" + deployment.getId() + "> doesn't exist"));

    mockMvc.perform(get("/deployments/" + deployment.getId() + "/resources"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code", is(404))).andExpect(jsonPath("$.title", is("Not Found")))
        .andExpect(
            jsonPath("$.message", is("The deployment <" + deployment.getId() + "> doesn't exist")));
  }

  @Test
  public void getResourceByIdAndDeploymentIdSuccesfully() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment();
    Resource resource = ControllerTestUtils.createResource(deployment);
    Mockito.when(resourceService.getResource(resource.getId(), deployment.getId()))
        .thenReturn(resource);

    mockMvc.perform(get("/deployments/" + deployment.getId() + "/resources/" + resource.getId()))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.uuid", equalTo(resource.getId())))
        .andExpect(jsonPath("$.links[1].rel", equalTo("self")))
        .andExpect(jsonPath("$.links[1].href",
            endsWith("/deployments/" + deployment.getId() + "/resources/" + resource.getId())))
        .andDo(document("get-resource", preprocessResponse(prettyPrint()), responseFields(
            fieldWithPath("uuid").description("The unique identifier of a resource"),
            fieldWithPath("creationTime").description(
                "Creation date-time (http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14)"),
            fieldWithPath("state").description(
                "The status of the resource. (http://indigo-dc.github.io/orchestrator/apidocs/it/reply/orchestrator/enums/NodeStates.html)"),
            fieldWithPath("toscaNodeType").optional()
                .description("The type of the represented TOSCA node"),
            fieldWithPath("requiredBy").description("A list of nodes that require this resource"),
            fieldWithPath("links[]").ignored())));
  }
}
