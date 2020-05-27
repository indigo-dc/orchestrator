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

package it.reply.orchestrator.resource;

import it.reply.orchestrator.controller.DeploymentScheduleController;
import it.reply.orchestrator.dal.entity.DeploymentScheduleEvent;
import it.reply.orchestrator.dal.entity.OidcEntity;
import it.reply.orchestrator.dal.entity.ReplicationRule;
import it.reply.orchestrator.utils.CommonUtils;
import java.util.Objects;
import java.util.Optional;
import org.springframework.hateoas.core.DummyInvocationUtils;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.stereotype.Component;

@Component
public class DeploymentScheduleEventResourceAssembler
    extends ResourceAssemblerSupport<DeploymentScheduleEvent, DeploymentScheduleEventResource> {

  private final DeploymentResourceAssembler deploymentResourceAssembler;
  public DeploymentScheduleEventResourceAssembler(DeploymentResourceAssembler deploymentResourceAssembler) {
    super(DeploymentScheduleController.class, DeploymentScheduleEventResource.class);
    this.deploymentResourceAssembler = deploymentResourceAssembler;
  }

  @Override
  public DeploymentScheduleEventResource toResource(DeploymentScheduleEvent entity) {
    return getDeploymentScheduleResource(entity);
  }

  private DeploymentScheduleEventResource getDeploymentScheduleResource(DeploymentScheduleEvent entity) {
    String replicationStatus = Optional.ofNullable(entity.getMainReplicationRule()).map(ReplicationRule::getStatus).map(Objects::toString).orElse(null);
    DeploymentScheduleEventResource resource = DeploymentScheduleEventResource.builder()
        .uuid(entity.getId())
        .creationTime(entity.getCreatedAt())
        .updateTime(entity.getUpdatedAt())
        .name(entity.getName())
        .scope(entity.getScope())
        .replicationStatus(replicationStatus)
        .deployment(deploymentResourceAssembler.getDeploymentResource(entity.getDeployment(), true))
        .build();
    return resource;
  }

}
