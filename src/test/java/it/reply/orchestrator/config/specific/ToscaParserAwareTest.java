/*
 * Copyright © 2015-2018 Santer Reply S.p.A.
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

import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.tosca.ArchiveParser;
import alien4cloud.tosca.ArchiveUploadService;
import alien4cloud.tosca.parser.ParsingException;

import it.reply.orchestrator.annotation.SpringTestProfile;
import it.reply.orchestrator.config.Alien4CloudConfig;
import it.reply.orchestrator.service.ToscaServiceImpl;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

@ActiveProfiles(SpringTestProfile.PROFILE_QUALIFIER)
@ContextConfiguration(classes = Alien4CloudConfig.class)
@SpringBootTest
public abstract class ToscaParserAwareTest {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Autowired
  @Spy
  private ApplicationContext ctx;

  @Autowired
  @Spy
  private ArchiveParser parser;

  @Autowired
  @Spy
  private ArchiveUploadService archiveUploadService;

  @Value("${directories.alien}/${directories.csar_repository}")
  private String alienRepoDir;

  @Value("${tosca.definitions.normative}")
  private String normativeLocalName;

  @Value("${tosca.definitions.indigo}")
  private String indigoLocalName;

  private static boolean setUpIsDone = false;

  @Before
  public void init() throws CSARVersionAlreadyExistsException, ParsingException, IOException {
    if (!setUpIsDone) {
      ToscaServiceImpl toscaServiceImpl = getToscaService();
      ReflectionTestUtils.setField(toscaServiceImpl, "alienRepoDir", alienRepoDir);
      ReflectionTestUtils.setField(toscaServiceImpl, "normativeLocalName", normativeLocalName);
      ReflectionTestUtils.setField(toscaServiceImpl, "indigoLocalName", indigoLocalName);
      toscaServiceImpl.init();
      setUpIsDone = true;
    }
  }

  protected abstract ToscaServiceImpl getToscaService();
}
