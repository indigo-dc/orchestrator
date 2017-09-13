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

package it.reply.orchestrator.resource;

import it.reply.orchestrator.controller.DeploymentController;
import it.reply.orchestrator.controller.ResourceController;
import it.reply.orchestrator.dal.entity.Resource;

import org.springframework.hateoas.core.DummyInvocationUtils;
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
    BaseResource resource = BaseResource.builder()
        .uuid(entity.getId())
        .creationTime(entity.getCreated())
        .updateTime(entity.getUpdated())
        .physicalId(entity.getIaasId())
        .state(entity.getState())
        .toscaNodeType(entity.getToscaNodeType())
        .toscaNodeName(entity.getToscaNodeName())
        .build();
    resource
        .add(ControllerLinkBuilder.linkTo(DummyInvocationUtils.methodOn(DeploymentController.class)
            .getDeployment(entity.getDeployment().getId())).withRel("deployment"));
    resource
        .add(
            ControllerLinkBuilder
                .linkTo(DummyInvocationUtils
                    .methodOn(ResourceController.class, entity.getDeployment().getId())
                    .getResource(entity.getDeployment().getId(), entity.getId()))
                .withSelfRel());
    return resource;
  }
}
