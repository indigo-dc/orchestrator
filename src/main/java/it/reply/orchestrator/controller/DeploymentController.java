package it.reply.orchestrator.controller;

import it.reply.orchestrator.dto.common.Deployment;
import it.reply.orchestrator.dto.request.DeploymentRequest;
import it.reply.orchestrator.dto.response.Deployments;
import it.reply.orchestrator.service.DeploymentService;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeploymentController {

  private static final Logger LOG = LogManager.getLogger(DeploymentController.class);

  @Autowired
  private DeploymentService deploymentService;

  @RequestMapping(value = "/deployments", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public Deployments getDeployments() {
    LOG.trace("Invoked method: getDeployments");
    List<Deployment> deployments = new ArrayList<Deployment>(deploymentService.getDeployments()
        .values());

    return new Deployments().withDeployments(deployments);
  }

  @ResponseStatus(HttpStatus.CREATED)
  @RequestMapping(value = "/deployments", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
  public Deployment createDeployment(@RequestBody DeploymentRequest request) {
    LOG.trace("Invoked method: createDeployment with parameter " + request.toString());
    return deploymentService.createDeployment(request);
  }

  @RequestMapping(value = "/deployments/{deploymentId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public Deployment getDeployment(@PathVariable("deploymentId") String id) {

    LOG.trace("Invoked method: getDeployment with id: " + id);
    return deploymentService.getDeployment(id);
  }

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @RequestMapping(value = "/deployments/{deploymentId}", method = RequestMethod.DELETE)
  public void deleteDeployment(@PathVariable("deploymentId") String id) {

    LOG.trace("Invoked method: getDeployment with id: " + id);
    deploymentService.deleteDeployment(id);
  }
}
