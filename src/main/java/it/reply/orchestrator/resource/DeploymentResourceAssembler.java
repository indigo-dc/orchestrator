package it.reply.orchestrator.resource;

import it.reply.orchestrator.controller.DeploymentController;
import it.reply.orchestrator.dal.entity.Deployment;

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

  private DeploymentResource getDeploymentResource(Deployment entity) {

    DeploymentResource resource = new DeploymentResource();
    resource.setUuid(entity.getId());
    resource.setCreationTime(entity.getCreated());
    resource.setUpdateTime(entity.getUpdated());
    resource.setStatus(entity.getStatus());

    resource.setTask(entity.getTask());

    resource.add(ControllerLinkBuilder.linkTo(DeploymentController.class).slash("deployments")
        .slash(entity).withSelfRel());
    resource.add(ControllerLinkBuilder.linkTo(DeploymentController.class).slash("deployments")
        .slash(entity).slash("resources").withRel("resources"));
    return resource;
  }
}
