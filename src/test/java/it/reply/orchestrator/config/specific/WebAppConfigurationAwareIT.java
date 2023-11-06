/*
 * Copyright © 2015-2021 I.N.F.N.
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

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import it.reply.orchestrator.IntegrationTest;
import it.reply.orchestrator.annotation.SpringTestProfile;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mitre.openid.connect.client.service.ClientConfigurationService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@ActiveProfiles(SpringTestProfile.PROFILE_QUALIFIER)
@SpringBootTest(properties = "alien4cloud.elasticSearch.clusterName=es-cluster-it-test")
@Category(IntegrationTest.class)
@Transactional
@TestExecutionListeners(listeners = { DbUnitTestExecutionListener.class },
    mergeMode = MergeMode.MERGE_WITH_DEFAULTS)
public abstract class WebAppConfigurationAwareIT {
  @MockBean
  private ClientConfigurationService staticClientConfigurationService;
}
