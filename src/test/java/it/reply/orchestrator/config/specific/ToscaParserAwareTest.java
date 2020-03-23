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

package it.reply.orchestrator.config.specific;

import it.reply.orchestrator.annotation.SpringTestProfile;
import it.reply.orchestrator.config.properties.OrchestratorProperties;
import it.reply.orchestrator.service.IndigoInputsPreProcessorService;
import it.reply.orchestrator.service.ToscaServiceImpl;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.orchestrator.tosca.NormativeLaxImportParser;
import it.reply.orchestrator.tosca.RemoteRepositoryServiceImpl;
import it.reply.orchestrator.tosca.TemplateParser;

import org.junit.ClassRule;
import org.junit.Rule;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

@ActiveProfiles(SpringTestProfile.PROFILE_QUALIFIER)
@SpringBootTest(
    classes = {
        AopAutoConfiguration.class,
        ToscaServiceImpl.class,
        NormativeLaxImportParser.class,
        RemoteRepositoryServiceImpl.class,
        TemplateParser.class
    }
)
@AutoConfigureWebClient
public abstract class ToscaParserAwareTest {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @MockBean
  protected OAuth2TokenService oauth2tokenService;

  @SpyBean
  protected IndigoInputsPreProcessorService indigoInputsPreProcessorService;

  @SpyBean
  protected OrchestratorProperties orchestratorProperties;

}
