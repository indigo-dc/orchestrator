package it.reply.orchestrator.controller;

import it.reply.domain.dsl.prisma.DebugInformations;
import it.reply.orchestrator.dal.entity.AbstractResourceEntity;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dto.request.DeploymentRequest;
import it.reply.orchestrator.resource.DeploymentResource;
import it.reply.orchestrator.resource.DeploymentResourceAssembler;
import it.reply.orchestrator.service.DeploymentService;
import it.reply.orchestrator.validator.DeploymentRequestValidator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;

import javax.validation.Valid;

@RestController
@PropertySource("classpath:version.properties")
public class DeploymentController {

  private static final Logger LOG = LogManager.getLogger(DeploymentController.class);

  @Value("${build.version}")
  private String projectVersion;

  @Value("${build.revision}")
  private String projectRevision;

  @Value("${build.timestamp}")
  private String projectTimestamp;

  @Autowired
  private DeploymentService deploymentService;

  @Autowired
  DeploymentResourceAssembler deploymentResourceAssembler;

  @InitBinder
  protected void initBinder(WebDataBinder binder) {
    binder.setValidator(new DeploymentRequestValidator());
  }

  @ResponseStatus(HttpStatus.OK)
  @RequestMapping(value = "/", method = RequestMethod.GET)
  public String getOrchestrator() {
    return "INDIGO-Orchestrator";
  }

  /**
   * Get the orchestrator status info.
   * 
   * @return the orchestrator status info.
   */
  @ResponseStatus(HttpStatus.OK)
  @RequestMapping(value = "/info", method = RequestMethod.GET)
  public DebugInformations getInfo() {
    DebugInformations info = new DebugInformations();
    info.setProjectVersion(projectVersion);
    info.setProjectRevision(projectRevision);
    info.setProjectTimestamp(projectTimestamp);

    String hostname;
    try {
      hostname = InetAddress.getLocalHost().getHostName();
    } catch (Exception ex) {
      hostname = "-NOT AVAILABLE-";
    }
    info.setServerHostname(hostname);

    return info;
  }

  /**
   * Get all deployments.
   * 
   * @param pageable
   *          {@Link Pageable}
   * @param pagedAssembler
   *          {@link PagedResourcesAssembler}
   * @return {@link DeploymentResource}
   */
  @ResponseStatus(HttpStatus.OK)
  @RequestMapping(value = "/deployments", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public PagedResources<DeploymentResource> getDeployments(
      @PageableDefault(sort = AbstractResourceEntity.CREATED_COLUMN_NAME,
          direction = Direction.DESC) Pageable pageable,
      PagedResourcesAssembler<Deployment> pagedAssembler) {

    LOG.trace("Invoked method: getDeployments");

    Page<Deployment> deployments = deploymentService.getDeployments(pageable);

    PagedResources<DeploymentResource> pagedDeploymentResources =
        pagedAssembler.toResource(deployments, deploymentResourceAssembler);

    return pagedDeploymentResources;
  }

  /**
   * Create a deployment.
   * 
   * @param request
   *          {@link DeploymentRequest}
   * @return {@link DeploymentResource}
   */
  @ResponseStatus(HttpStatus.CREATED)
  @RequestMapping(value = "/deployments", method = RequestMethod.POST,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public DeploymentResource createDeployment(@Valid @RequestBody DeploymentRequest request) {

    LOG.trace("Invoked method: createDeployment with parameter " + request.toString());
    Deployment deployment = deploymentService.createDeployment(request);
    return deploymentResourceAssembler.toResource(deployment);

  }

  /**
   * Update the deployment.
   * 
   * @param id
   *          the deployment id
   * @param request
   *          {@link DeploymentRequest}
   */
  @ResponseStatus(HttpStatus.ACCEPTED)
  @RequestMapping(value = "/deployments/{deploymentId}", method = RequestMethod.PUT)
  public void updateDeployment(@PathVariable("deploymentId") String id,
      @Valid @RequestBody DeploymentRequest request) {

    LOG.trace("Invoked method: putDeployment with id: " + id);
    deploymentService.updateDeployment(id, request);
  }

  /**
   * Get the deployment.
   * 
   * @param id
   *          the deployment id
   * @return {@link DeploymentResource}
   */
  @ResponseStatus(HttpStatus.OK)
  @RequestMapping(value = "/deployments/{deploymentId}", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public DeploymentResource getDeployment(@PathVariable("deploymentId") String id) {

    LOG.trace("Invoked method: getDeployment with id: " + id);
    Deployment deployment = deploymentService.getDeployment(id);
    return deploymentResourceAssembler.toResource(deployment);
  }

  /**
   * Delete the deployment.
   * 
   * @param id
   *          the deployment id
   */
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @RequestMapping(value = "/deployments/{deploymentId}", method = RequestMethod.DELETE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public void deleteDeployment(@PathVariable("deploymentId") String id) {

    LOG.trace("Invoked method: getDeployment with id: " + id);
    deploymentService.deleteDeployment(id);
  }
}
