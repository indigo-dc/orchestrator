package it.reply.orchestrator.service;

import it.reply.orchestrator.controller.DeploymentController;
import it.reply.orchestrator.dto.common.Deployment;
import it.reply.orchestrator.dto.common.Link;
import it.reply.orchestrator.dto.request.DeploymentRequest;
import it.reply.orchestrator.exception.NotFoudException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Service;

@Service
public class DeploymentServiceImpl implements DeploymentService {

  private Map<String, Deployment> deployments = new ConcurrentHashMap<String, Deployment>();

  @Override
  public Map<String, Deployment> getDeployments() {
    return deployments;
  }

  @Override
  public Deployment getDeployment(String id) {
    if (deployments.containsKey(id)) {
      return deployments.get(id);
    } else {
      throw new NotFoudException("The Deployment <" + id + "> doesn't exist");
    }
  }

  @Override
  public Deployment createDeployment(DeploymentRequest request) {
    // dummy request handling
    String id = UUID.randomUUID().toString();
    Deployment deployment = new Deployment();
    deployment.withId(id);
    org.springframework.hateoas.Link hateoasLink = ControllerLinkBuilder
        .linkTo(DeploymentController.class).slash("deployments").slash(deployment).withSelfRel();

    Link link = new Link().withHref(hateoasLink.getHref()).withRel(
        org.springframework.hateoas.Link.REL_SELF);

    List<Link> links = new ArrayList<Link>();
    links.add(link);
    deployment.withLinks(links);
    deployments.put(id, deployment);
    return deployment;
  }

  @Override
  public void deleteDeployment(String id) {
    if (deployments.containsKey(id)) {
      deployments.remove(id);
    } else {
      throw new NotFoudException("The Deployment <" + id + "> doesn't exist");
    }
  }

}
