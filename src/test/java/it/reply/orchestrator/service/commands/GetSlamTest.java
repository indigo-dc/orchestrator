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

package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.config.properties.SlamProperties;
import it.reply.orchestrator.dal.repository.OidcEntityRepository;
import it.reply.orchestrator.function.ThrowingFunction;
import it.reply.orchestrator.service.SlamServiceImpl;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.orchestrator.util.IntegrationTestUtil;

import java.net.URI;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.mockito.Mockito.*;

public class GetSlamTest extends BaseRankCloudProvidersCommandTest<GetSlam> {

  @Spy
  @InjectMocks
  private SlamServiceImpl slamService;

  @Spy
  private SlamProperties slamProperties;

  @Spy
  private OidcProperties oidcProperties;

  @Mock
  private OidcEntityRepository entityRepository;

  public GetSlamTest() {
    super(new GetSlam());
  }

  @Mock
  private OAuth2TokenService oauth2TokenService;

  @Before
  public void setup() throws Exception {
    slamProperties.setUrl(URI.create("https://www.example.com"));
    when(oauth2TokenService.executeWithClientForResult(any(), any(), any()))
        .thenAnswer(y -> ((ThrowingFunction) y.getArguments()[1]).apply("token"));
    when(oauth2TokenService.getOrganization(any())).thenReturn("indigo-dc");
  }

  @Test
  public void doexecuteSuccesfully() throws Exception {

    IntegrationTestUtil.mockSlam(mockServer, slamProperties.getUrl());

    String serializedRankCloudProvidersMessage =
        "{\"deploymentId\":\"mmd34483-d937-4578-bfdb-ebe196bf82dd\"}";

    Assertions
        .assertThatCode(() -> execute(serializedRankCloudProvidersMessage))
        .doesNotThrowAnyException();

  }

}
