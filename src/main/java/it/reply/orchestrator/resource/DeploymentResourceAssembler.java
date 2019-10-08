/*
 * Copyright © 2015-2019 Santer Reply S.p.A.
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
import it.reply.orchestrator.controller.TemplateController;
import it.reply.orchestrator.dal.entity.Deployment;
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
public class DeploymentResourceAssembler
    extends ResourceAssemblerSupport<Deployment, DeploymentResource> {

  public DeploymentResourceAssembler() {
    super(DeploymentController.class, DeploymentResource.class);
  }

  @Override
  public DeploymentResource toResource(Deployment entity) {
    return getDeploymentResource(entity);
  }

  @SuppressWarnings("rawtypes")
  private CloudProviderEndpointResource
      getCloudProviderEndpointResource(CloudProviderEndpoint endpoint) {
    if (endpoint != null) {
      Map<String, CloudProviderEndpointResource> hybridCloudProviderEndpointsResource =
          new HashMap<>();
      Map<String, CloudProviderEndpoint> hybridCloudProviderEndpoints =
          endpoint.getHybridCloudProviderEndpoints();
      if (hybridCloudProviderEndpoints != null) {
        Iterator it = endpoint.getHybridCloudProviderEndpoints().entrySet().iterator();
        while (it.hasNext()) {
          Map.Entry pair = (Map.Entry)it.next();
          hybridCloudProviderEndpointsResource.put((String)pair.getKey(),
              getCloudProviderEndpointResource((CloudProviderEndpoint)pair.getValue()));
        }
      }
      return new CloudProviderEndpointResource(endpoint.getCpEndpoint(),
          endpoint.getCpComputeServiceId(),
          endpoint.getVaultEndpoint(),
          endpoint.getIaasType(),
          hybridCloudProviderEndpointsResource);
    } else {
      return null;
    }
  }

  private DeploymentResource getDeploymentResource(Deployment entity) {

    DeploymentResource resource = DeploymentResource.builder()
        .uuid(entity.getId())
        .creationTime(entity.getCreatedAt())
        .updateTime(entity.getUpdatedAt())
        .physicalId(entity.getEndpoint())
        .status(entity.getStatus())
        .statusReason(entity.getStatusReason())
        .cloudProviderName(entity.getCloudProviderName())
        .cloudProviderEndpoint(getCloudProviderEndpointResource(entity.getCloudProviderEndpoint()))
        .task(entity.getTask())
        .outputs(entity.getOutputs())
        .callback(entity.getCallback())
        .build();

    Optional.ofNullable(entity.getOwner())
        .map(OidcEntity::getOidcEntityId)
        .ifPresent(resource::setCreatedBy);

    // add hateoas links only if we are inside of a HTTP request otherwise, due to
    // https://github.com/spring-projects/spring-hateoas/issues/408, we'd have an error
    if (CommonUtils.isInHttpRequest()) {
      resource.add(ControllerLinkBuilder.linkTo(
          DummyInvocationUtils.methodOn(DeploymentController.class).getDeployment(entity.getId()))
          .withSelfRel());
      resource.add(ControllerLinkBuilder
          .linkTo(DummyInvocationUtils.methodOn(ResourceController.class, entity.getId())
              .getResources(entity.getId(), null, null))
          .withRel("resources"));
      resource.add(ControllerLinkBuilder
          .linkTo(
              DummyInvocationUtils.methodOn(TemplateController.class).getTemplate(entity.getId()))
          .withRel("template"));
    }

    return resource;
  }

}
