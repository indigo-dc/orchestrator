package it.reply.orchestrator.service.commands.chronos;

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
import it.reply.orchestrator.controller.DeploymentController;
import it.reply.orchestrator.service.WorkflowConstants;
import it.reply.orchestrator.service.deployment.providers.ChronosServiceImpl;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderService;
import it.reply.workflowmanager.spring.orchestrator.bpm.ejbcommands.BaseCommand;

import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class CleanChronos extends BaseCommand {

  private static final Logger LOG = LoggerFactory.getLogger(DeploymentController.class);

  @Autowired
  @Qualifier("CHRONOS")
  private DeploymentProviderService chronosService;

  @Autowired
  private ChronosServiceImpl chronosServiceImpl;

  @Override
  protected ExecutionResults customExecute(CommandContext ctx) throws Exception {
    String deploymentId = getParameter(ctx, WorkflowConstants.WF_PARAM_DEPLOYMENT_ID);
    // boolean result = chronosServiceImpl.cleanChronos(deploymentId);
    // FIXME Remove if actualiy not used !
    LOG.warn("NO OP HERE !");
    return resultOccurred(true);
  }

}
