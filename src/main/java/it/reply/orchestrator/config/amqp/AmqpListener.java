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

package it.reply.orchestrator.config.amqp;


import it.reply.orchestrator.dal.entity.StoragePathEntity;
import it.reply.orchestrator.dto.request.DeploymentRequest;
import it.reply.orchestrator.service.DeploymentService;
import it.reply.orchestrator.service.StorageService;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AmqpListener implements MessageListener {
  
  @Autowired
  private StorageService storageService;
  
  @Autowired
  private DeploymentService deploymentService;

  @Override
  public void onMessage(Message message) {
    System.out.println("Received Message onMessage: " + new String(message.getBody()));
    
    //TODO watching path may finish with *.csv or xxx.csv 
    StoragePathEntity entity = storageService.getEntityByPath(new String(message.getBody()));
    
    if( entity!=null ) {
    
      DeploymentRequest request = DeploymentRequest
          .builder()
          .parameters(entity.getParameters())
          .template(entity.getTemplate())
          .callback(entity.getCallback())
          .build();
      String temp = "";
      //deploymentService.createDeployment(request);
    }
    
  }
}
