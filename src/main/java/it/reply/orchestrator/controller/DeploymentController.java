package it.reply.orchestrator.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dto.request.DeploymentRequest;
import it.reply.orchestrator.resource.DeploymentResource;
import it.reply.orchestrator.resource.DeploymentResourceAssembler;
import it.reply.orchestrator.service.DeploymentService;

@RestController
public class DeploymentController {

  private static final Logger LOG = LogManager.getLogger(DeploymentController.class);

  @Autowired
  private DeploymentService deploymentService;

  @Autowired
  DeploymentResourceAssembler deploymentResourceAssembler;

  @ResponseStatus(HttpStatus.OK)
  @RequestMapping(value = "/", method = RequestMethod.GET)
  public String getOrchestrator() {

    return "INDIGO-Orchestrator";
  }

  @ResponseStatus(HttpStatus.OK)
  @RequestMapping(value = "/deployments", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public PagedResources<DeploymentResource> getDeployments(@PageableDefault Pageable pageable,
      PagedResourcesAssembler<Deployment> pagedAssembler) {

    LOG.trace("Invoked method: getDeployments");

    Page<Deployment> deployments = deploymentService.getDeployments(pageable);

    PagedResources<DeploymentResource> pagedDeploymentResources = pagedAssembler
        .toResource(deployments, deploymentResourceAssembler);

    return pagedDeploymentResources;
  }

  @ResponseStatus(HttpStatus.CREATED)
  @RequestMapping(value = "/deployments", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
  public DeploymentResource createDeployment(@RequestBody DeploymentRequest request) {

    LOG.trace("Invoked method: createDeployment with parameter " + request.toString());
    Deployment deployment = deploymentService.createDeployment(request);
    return deploymentResourceAssembler.toResource(deployment);

  }

  @ResponseStatus(HttpStatus.OK)
  @RequestMapping(value = "/deployments/{deploymentId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public DeploymentResource getDeployment(@PathVariable("deploymentId") String id) {

    LOG.trace("Invoked method: getDeployment with id: " + id);
    Deployment deployment = deploymentService.getDeployment(id);
    return deploymentResourceAssembler.toResource(deployment);
  }

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @RequestMapping(value = "/deployments/{deploymentId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
  public void deleteDeployment(@PathVariable("deploymentId") String id) {

    LOG.trace("Invoked method: getDeployment with id: " + id);
    deploymentService.deleteDeployment(id);
  }
}
