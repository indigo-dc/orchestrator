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

import it.reply.orchestrator.controller.DeploymentSchedulerController;
import it.reply.orchestrator.dal.entity.DeploymentScheduler;
import it.reply.orchestrator.dal.entity.OidcEntity;
import it.reply.orchestrator.utils.CommonUtils;

import java.util.Optional;

import org.springframework.hateoas.core.DummyInvocationUtils;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.stereotype.Component;

@Component
public class SchedulerResourceAssembler
    extends ResourceAssemblerSupport<DeploymentScheduler, SchedulerResource> {

  public SchedulerResourceAssembler() {
    super(DeploymentSchedulerController.class, SchedulerResource.class);
  }

  @Override
  public SchedulerResource toResource(DeploymentScheduler entity) {
    return getSchedulerResource(entity);
  }

  private SchedulerResource getSchedulerResource(DeploymentScheduler scheduler) {

    SchedulerResource resource = SchedulerResource
        .builder()
        .creationTime(scheduler.getCreatedAt())
        .updateTime(scheduler.getUpdatedAt())
        .userStoragePath(scheduler.getUserStoragePath())
        .template(scheduler.getTemplate())
        .callback(scheduler.getCallback())
        .requestedWithToken(scheduler.getRequestedWithToken())
        .uuid(scheduler.getId())
        .build();

    Optional.ofNullable(scheduler.getOwner())
        .map(OidcEntity::getOidcEntityId)
        .ifPresent(resource::setCreatedBy);

    if (CommonUtils.isInHttpRequest()) {
      resource.add(ControllerLinkBuilder.linkTo(
          DummyInvocationUtils
          .methodOn(DeploymentSchedulerController.class)
          .getDeploymentScheduler(scheduler.getId()))
          .withSelfRel());
    }

    return resource;
  }

}
