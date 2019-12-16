/*
 * Copyright © 2019 I.N.F.N.
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

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;

import it.reply.orchestrator.dto.SystemEndpoints;
import it.reply.orchestrator.service.ConfigurationService;
import junitparams.JUnitParamsRunner;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.HateoasAwareSpringDataWebConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.servlet.MockMvc;


@WebMvcTest(controllers = ConfigurationController.class, secure = false)
@RunWith(JUnitParamsRunner.class)
@AutoConfigureRestDocs("target/generated-snippets")
@Import(HateoasAwareSpringDataWebConfiguration.class)
public class ConfigurationControllerTest {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ConfigurationService configurationService;

  @Test
  public void getConfiguration() throws Exception {

    Mockito.when(configurationService.getConfiguration())
    .thenReturn(SystemEndpoints
        .builder()
        .cprUrl(URI.create("http://deep-cpr.test.infn.it:8080"))
        .slamUrl(URI.create("https://deep-slam.test.infn.it:8443/rest/slam"))
        .cmdbUrl(URI.create("https://deep-paas-dev.test.infn.it/cmdb"))
        .imUrl(URI.create("https://deep-paas.test.infn.it/im"))
        .monitoringUrl(URI.create("https://deep-paas.test.infn.it/monitoring"))
        .vaultUrl(URI.create("https://vault.test.infn.it:8200"))
        .build());

    this.mockMvc.perform(get("/configuration")
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(document("configuration", preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint()),
            responseFields(
                fieldWithPath("cpr_url").description(
                    "The URI of the Cloud Provider Ranker"),
                fieldWithPath("slam_url").description(
                    "The URI of the SLAM (Service Level Agreement Manager)"),
                fieldWithPath("cmdb_url").description(
                    "The URI of the CMDB (Change Management DataBase)"),
                fieldWithPath("im_url").description(
                    "The URI of the IM (Infrastructure Manager)"),
                fieldWithPath("monitoring_url").description(
                    "The URI of the Monitoring Service"),
                fieldWithPath("vault_url").description(
                    "The URI of the Vault Server (optional)"))));

  }

}
