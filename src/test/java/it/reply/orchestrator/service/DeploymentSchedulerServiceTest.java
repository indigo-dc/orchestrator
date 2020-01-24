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

package it.reply.orchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;

import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.dal.entity.DeploymentScheduler;
import it.reply.orchestrator.dal.repository.DeploymentSchedulerRepository;
import it.reply.orchestrator.dto.request.SchedulerRequest;
import it.reply.orchestrator.service.security.OAuth2TokenService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

public class DeploymentSchedulerServiceTest {


  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Mock
  DeploymentSchedulerRepository deploymentSchedulerRepository;

  @Mock
  private OAuth2TokenService oauth2TokenService;

  @Mock
  private OidcProperties oidcProperties;

  @InjectMocks
  DeploymentSchedulerService deploymentSchedulerService = new DeploymentSchedulerServiceImpl();

  @Test
  public void addStoragePathNew() throws Exception {

    Map<String, Object> parameters = new HashMap<>();

    Mockito.when(deploymentSchedulerRepository.findByUserStoragePath("stringUrl1"))
    .thenReturn(null);

    Mockito.when(deploymentSchedulerRepository.save(Mockito.any(DeploymentScheduler.class)))
    .thenAnswer(y -> {
      DeploymentScheduler deploymentScheduler = (DeploymentScheduler) y.getArguments()[0];
      deploymentScheduler.setId(UUID.randomUUID().toString());
      return deploymentScheduler;
    });

    SchedulerRequest schedulerRequest = SchedulerRequest
        .builder()
        .parameters(parameters)
        .userStoragePath("stringUrl1")
        .template("template")
        .callback("http://localhost:8080/callback")
        .build();

    assertThat(deploymentSchedulerService.addDeploymentScheduler(schedulerRequest)!=(null));

  }

  @Test
  public void addStoragePathExisting() throws Exception {

    Map<String, Object> parameters = new HashMap<>();

    Mockito.when(deploymentSchedulerRepository.findByUserStoragePath("stringUrl1"))
    .thenReturn(null);

    Mockito.when(deploymentSchedulerRepository.save(Mockito.any(DeploymentScheduler.class)))
    .thenAnswer(y -> {
      DeploymentScheduler deploymentScheduler = (DeploymentScheduler) y.getArguments()[0];
      deploymentScheduler.setId(UUID.randomUUID().toString());
      return deploymentScheduler;
    });

    SchedulerRequest schedulerRequest = SchedulerRequest
        .builder()
        .parameters(parameters)
        .userStoragePath("stringUrl1")
        .template("template")
        .callback("http://localhost:8080/callback")
        .build();

    assertThat(deploymentSchedulerService.addDeploymentScheduler(schedulerRequest) == null);

  }

}
