package it.reply.orchestrator.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.reply.orchestrator.dal.entity.StoragePathEntity;
import it.reply.orchestrator.dto.request.PathRequest;
import it.reply.orchestrator.service.StorageService;
import it.reply.orchestrator.utils.JsonUtils;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = StorageController.class, secure = false)
@AutoConfigureRestDocs("target/generated-snippets")
public class StorageControllerTest {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private StorageService storageService;

  @Test
  public void addStoragePathExisting() throws Exception {

    PathRequest pathRequest = new PathRequest();
    pathRequest.setStoragePath("http://www.site1.com/storagepath/*");

    Mockito.when(storageService.addStoragePath("http://www.site1.com/storagepath/*"))
    .thenReturn(null);

    MvcResult result =
        mockMvc.perform(post("/storage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.serialize(pathRequest))
                .header(HttpHeaders.AUTHORIZATION,
                    OAuth2AccessToken.BEARER_TYPE + " <access token>"))
            .andExpect(status().is(500))
            .andExpect(jsonPath("$.message", is("already exists")))
            .andReturn();

  }

  @Test
  public void addStoragePathNew() throws Exception {

    PathRequest pathRequest = new PathRequest();
    pathRequest.setStoragePath("http://www.site1.com/storagepath/*");

    Mockito.when(storageService.addStoragePath("http://www.site1.com/storagepath/*"))
    .thenReturn(new StoragePathEntity("http://www.site1.com/storagepath/*"));

    MvcResult result =
        mockMvc.perform(post("/storage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.serialize(pathRequest))
                .header(HttpHeaders.AUTHORIZATION,
                    OAuth2AccessToken.BEARER_TYPE + " <access token>"))
            .andExpect(status().is(201))
            .andExpect(jsonPath("$.storagePath", is("http://www.site1.com/storagepath/*")))
            .andReturn();

  }

}
