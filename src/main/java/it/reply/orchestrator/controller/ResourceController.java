package it.reply.orchestrator.controller;

import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.resource.BaseResource;
import it.reply.orchestrator.resource.BaseResourceAssembler;
import it.reply.orchestrator.service.ResourceService;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/deployments/{deploymentId}")
public class ResourceController {

  private static final Logger LOG = LogManager.getLogger(ResourceController.class);

  @Autowired
  private ResourceService resourceService;

  @Autowired
  BaseResourceAssembler baseResourceAssembler;

  @ResponseStatus(HttpStatus.OK)
  @RequestMapping(value = "/resources", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public PagedResources<BaseResource> getResources(
      @PathVariable("deploymentId") String deploymentId, @PageableDefault Pageable pageable,
      PagedResourcesAssembler<Resource> pagedAssembler) {

    LOG.trace("Invoked method: getDeployments");

    Page<Resource> resources = resourceService.getResources(deploymentId, pageable);

    PagedResources<BaseResource> pagedResources = pagedAssembler.toResource(resources,
        baseResourceAssembler);

    return pagedResources;
  }

  @ResponseStatus(HttpStatus.OK)
  @RequestMapping(value = "/resources/{resourceId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public BaseResource getResource(@PathVariable("deploymentId") String deploymentId,
      @PathVariable("resourceId") String resourceId) {

    LOG.trace("Invoked method: getResource with id: " + resourceId);
    Resource resource = resourceService.getResource(resourceId, deploymentId);
    return baseResourceAssembler.toResource(resource);
  }

}
