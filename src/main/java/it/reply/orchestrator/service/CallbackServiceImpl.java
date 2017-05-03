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

package it.reply.orchestrator.service;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.resource.DeploymentResource;
import it.reply.orchestrator.resource.DeploymentResourceAssembler;

import lombok.AllArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
@AllArgsConstructor
public class CallbackServiceImpl implements CallbackService {

  private DeploymentRepository deploymentRepository;

  private DeploymentResourceAssembler deploymentResourceAssembler;

  private RestTemplate restTemplate;

  @Override
  @Transactional(readOnly = true)
  public boolean doCallback(String deploymentId) {
    Deployment deployment = deploymentRepository.findOne(deploymentId);
    return doCallback(deployment);
  }

  private boolean doCallback(Deployment deployment) {
    if (deployment.getCallback() != null) {
      DeploymentResource deploymentResource = deploymentResourceAssembler.toResource(deployment);
      ResponseEntity<?> response =
          restTemplate.postForEntity(deployment.getCallback(), deploymentResource, Object.class);
      return response.getStatusCode().is2xxSuccessful();
    }
    return false;
  }

}
