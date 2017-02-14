package it.reply.orchestrator.service;

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

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.exception.http.NotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateServiceImpl implements TemplateService {

  @Autowired
  private DeploymentRepository deploymentRepository;

  @Override
  @Transactional(readOnly = true)
  public String getTemplate(String uuid) {
    Deployment deployment = deploymentRepository.findOne(uuid);
    if (deployment != null) {
      return deployment.getTemplate();
    } else {
      throw new NotFoundException("The deployment <" + uuid + "> doesn't exist");
    }
  }

}
