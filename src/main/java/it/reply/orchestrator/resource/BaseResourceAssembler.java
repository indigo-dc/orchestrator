package it.reply.orchestrator.resource;

import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.stereotype.Component;

import it.reply.orchestrator.controller.DeploymentController;
import it.reply.orchestrator.controller.ResourceController;
import it.reply.orchestrator.dal.entity.Resource;

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
    // TODO complete the mapping
    BaseResource resource = new BaseResource();
    resource.setUuid(entity.getId());
    resource.setCreationTime(entity.getCreated());
    resource.setStatus(entity.getStatus());

    resource.add(ControllerLinkBuilder.linkTo(DeploymentController.class).slash("deployments")
        .slash("1").withRel("deployment"));
    resource.add(ControllerLinkBuilder.linkTo(DeploymentController.class).slash("deployments")
        .slash(entity).slash("resources").withSelfRel());
    return resource;
  }
}
