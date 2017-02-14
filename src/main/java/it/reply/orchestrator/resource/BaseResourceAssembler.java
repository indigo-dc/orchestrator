package it.reply.orchestrator.resource;

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
import it.reply.orchestrator.controller.ResourceController;
import it.reply.orchestrator.dal.entity.Resource;

import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.stereotype.Component;

@Component
public class BaseResourceAssembler extends ResourceAssemblerSupport<Resource, BaseResource> {

  public BaseResourceAssembler() {
    super(ResourceController.class, BaseResource.class);
  }

  @Override
  public BaseResource toResource(Resource entity) {
    return getBaseResource(entity);
  }

  private BaseResource getBaseResource(Resource entity) {
    BaseResource resource = new BaseResource();
    resource.setUuid(entity.getId());
    resource.setCreationTime(entity.getCreated());
    resource.setState(entity.getState());
    resource.setToscaNodeType(entity.getToscaNodeType());
    resource.setToscaNodeName(entity.getToscaNodeName());
    resource.add(ControllerLinkBuilder.linkTo(DeploymentController.class).slash("deployments")
        .slash(entity.getDeployment().getId()).withRel("deployment"));
    resource.add(ControllerLinkBuilder.linkTo(DeploymentController.class).slash("deployments")
        .slash(entity.getDeployment().getId()).slash("resources").slash(entity).withSelfRel());
    return resource;
  }
}
