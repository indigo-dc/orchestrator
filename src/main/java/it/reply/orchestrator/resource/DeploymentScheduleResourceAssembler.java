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

import it.reply.orchestrator.controller.DeploymentController;
import it.reply.orchestrator.controller.DeploymentScheduleController;
import it.reply.orchestrator.controller.ResourceController;
import it.reply.orchestrator.controller.TemplateController;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.DeploymentSchedule;
import it.reply.orchestrator.dal.entity.OidcEntity;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.utils.CommonUtils;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import org.springframework.hateoas.core.DummyInvocationUtils;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.stereotype.Component;

@Component
public class DeploymentScheduleResourceAssembler
    extends ResourceAssemblerSupport<DeploymentSchedule, DeploymentScheduleResource> {

  public DeploymentScheduleResourceAssembler() {
    super(DeploymentScheduleController.class, DeploymentScheduleResource.class);
  }

  @Override
  public DeploymentScheduleResource toResource(DeploymentSchedule entity) {
    return getDeploymentScheduleResource(entity);
  }

  private DeploymentScheduleResource getDeploymentScheduleResource(DeploymentSchedule entity) {

    DeploymentScheduleResource resource = DeploymentScheduleResource.builder()
        .uuid(entity.getId())
        .creationTime(entity.getCreatedAt())
        .updateTime(entity.getUpdatedAt())
        .status(entity.getStatus())
        .callback(entity.getCallback())
        .numberOfReplicas(entity.getNumberOfReplicas())
        .fileExpression(entity.getFileExpression())
        .replicationExpression(entity.getReplicationExpression())
        .build();

    Optional.ofNullable(entity.getOwner())
        .map(OidcEntity::getOidcEntityId)
        .ifPresent(resource::setCreatedBy);

    // add hateoas links only if we are inside of a HTTP request otherwise, due to
    // https://github.com/spring-projects/spring-hateoas/issues/408, we'd have an error
    if (CommonUtils.isInHttpRequest()) {
      resource.add(ControllerLinkBuilder.linkTo(
          DummyInvocationUtils.methodOn(DeploymentScheduleController.class).getDeploymentSchedule(entity.getId()))
          .withSelfRel());
//      resource.add(ControllerLinkBuilder
//          .linkTo(DummyInvocationUtils.methodOn(DeploymentScheduleController.class, entity.getId())
//              .getDeploymentScheduleEvents(entity.getId(), null, null, null))
//          .withRel("events"));
    }

    return resource;
  }

}
