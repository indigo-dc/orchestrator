/*
 * Copyright © 2015-2020 Santer Reply S.p.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    pathRequest.setTemplate("template1");

    Mockito.when(storageService.addStoragePath("http://www.site1.com/storagepath/*", "template1"))
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
    pathRequest.setTemplate("template");

    Mockito.when(storageService.addStoragePath("http://www.site1.com/storagepath/*", "template"))
    .thenReturn(new StoragePathEntity("http://www.site1.com/storagepath/*", "template", null, null));

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
