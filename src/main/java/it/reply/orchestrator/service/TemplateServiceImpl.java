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

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.DeploymentSchedule;
import it.reply.orchestrator.dal.entity.DeploymentScheduleEvent;
import it.reply.orchestrator.utils.MdcUtils;

import lombok.AllArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class TemplateServiceImpl implements TemplateService {

  private DeploymentService deploymentservice;

  private DeploymentScheduleServiceImpl deploymentScheduleService;

  @Override
  @Transactional(readOnly = true)
  public String getDeploymentTemplate(String uuid) {
    // check if deploymentExists
    Deployment deployment = deploymentservice.getDeployment(uuid);
    MdcUtils.setDeploymentId(deployment.getId());
    return deployment.getTemplate();
  }

  @Override
  @Transactional(readOnly = true)
  public String getDeploymentScheduleTemplate(String uuid) {
    DeploymentSchedule deploymentSchedule = deploymentScheduleService.getDeploymentSchedule(uuid);
    return deploymentSchedule.getTemplate();
  }

//  @Override
//  @Transactional(readOnly = true)
//  public String getDeploymentScheduleEventTemplate(String deploymentScheduleId, String deploymentScheduleEventId) {
//    DeploymentScheduleEvent deploymentSchedule = deploymentScheduleService.getDeploymentScheduleEvent(deploymentScheduleId, deploymentScheduleEventId);
//    return deploymentSchedule.getDeployment().getTemplate();
//  }

}
