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

package it.reply.orchestrator.service.commands;

import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.config.properties.SlamProperties;
import it.reply.orchestrator.dal.repository.OidcEntityRepository;
import it.reply.orchestrator.service.SlamServiceImpl;
import it.reply.orchestrator.workflow.RankCloudProvidersWorkflowIT;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.net.URI;

public class GetSlamTest extends BaseRankCloudProvidersCommandTest<GetSlam> {

  @InjectMocks
  private GetSlam getSLAMCommand;

  @Spy
  @InjectMocks
  private SlamServiceImpl slamService;

  @Spy
  private SlamProperties slamProperties;

  @Spy
  private OidcProperties oidcProperties;

  @Mock
  private OidcEntityRepository entityRepository;

  private final String endpoint = "https://www.example.com";

  public GetSlamTest() {
    super(new GetSlam());
  }

  @Before
  public void setup() {
    slamProperties.setUrl(URI.create(endpoint));
  }

  @Test
  public void doexecuteSuccesfully() throws Exception {

    RankCloudProvidersWorkflowIT.mockSlam(mockServer, slamProperties.getUrl());

    String serializedRankCloudProvidersMessage =
        "{\"deploymentId\":\"mmd34483-d937-4578-bfdb-ebe196bf82dd\"}";

    Assertions
        .assertThatCode(() -> execute(serializedRankCloudProvidersMessage))
        .doesNotThrowAnyException();

  }

}
