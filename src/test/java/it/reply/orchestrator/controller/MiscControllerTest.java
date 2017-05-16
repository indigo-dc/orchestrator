/*
 * Copyright © 2015-2017 Santer Reply S.p.A.
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

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentation;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(MockitoJUnitRunner.class)
public class MiscControllerTest {

  private MockMvc mockMvc;

  @InjectMocks
  private MiscController miscController = new MiscController();

  @Rule
  public RestDocumentation restDocumentation = new RestDocumentation("target/generated-snippets");

  /**
   * Set up test context.
   */
  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    mockMvc = MockMvcBuilders.standaloneSetup(miscController)
        .apply(documentationConfiguration(this.restDocumentation))
        .dispatchOptions(true)
        .build();
  }

  @Test
  public void getOrchestrator() throws Exception {
    mockMvc.perform(get("/").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
  }

  @Test
  public void getInfo() throws Exception {
    mockMvc.perform(get("/info").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
  }

}
