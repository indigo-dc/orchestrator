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

package it.reply.orchestrator.controller;

import groovy.util.ResourceException;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.DeploymentRequester;
import it.reply.orchestrator.dal.entity.DeploymentScheduler;
import it.reply.orchestrator.dto.request.DeploymentRequest;
import it.reply.orchestrator.service.DeploymentRequesterService;
import it.reply.orchestrator.service.DeploymentSchedulerService;
import it.reply.orchestrator.service.DeploymentService;
import it.reply.orchestrator.utils.MdcUtils;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AmqpListener implements MessageListener {

  @Autowired
  private DeploymentSchedulerService deploymentSchedulerService;

  @Autowired
  private DeploymentRequesterService deploymentRequesterServcie;

  @Autowired
  private DeploymentService deploymentService;

  @Override
  public void onMessage(Message message) {
    System.out.println("Received Message onMessage: " + new String(message.getBody()));

    //TODO watching path may finish with *.csv or xxx.csv

    String path = new String(message.getBody());
    String superPath = "";
    if (path.contains("/")) {
      superPath = path.substring(0, path.lastIndexOf("/"));
      superPath += "/*";
    }

    DeploymentScheduler entity = null;
    try {
      entity = deploymentSchedulerService.getEntityByPath(superPath);
    } catch (Exception e) {
      LOG.error("Get Entity by path: {} Is Exited with error : {}",
          path, e);
    }

    if (entity != null) {
      DeploymentRequest request = DeploymentRequest
          .builder()
          .parameters(entity.getParameters())
          .template(entity.getTemplate())
          .callback(entity.getCallback())
          .build();
      MdcUtils.setRequestId(UUID.randomUUID().toString());
      Deployment deployment = deploymentService.createDeploymentWithOidc(
          request,
          entity.getOwner(),
          entity.getRequestedWithToken());
      DeploymentRequester requester = new DeploymentRequester();
      requester.setStoragePath(path);
      requester.setDeployment(deployment);
      requester.setDeploymentScheduler(entity);

      try {
        deploymentRequesterServcie.addDeploymentRequester(requester);
      } catch (ResourceException e) {
        LOG.error("Adding deployment requester with storage path: {} Exited with error : {}",
            requester.getStoragePath(), e);
      }
    }

  }
}
