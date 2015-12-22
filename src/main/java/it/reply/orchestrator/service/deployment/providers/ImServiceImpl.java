package it.reply.orchestrator.service.deployment.providers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.upv.i3m.grycap.im.api.InfrastructureManagerApiClient;
import es.upv.i3m.grycap.im.api.VmStates;
import es.upv.i3m.grycap.im.client.ServiceResponse;
import es.upv.i3m.grycap.im.exceptions.AuthFileNotFoundException;
import it.reply.orchestrator.controller.DeploymentController;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.im.InfrastructureStatus;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.exception.service.DeploymentException;

@Service
@PropertySource("classpath:im-config/im-java-api.properties")
public class ImServiceImpl extends AbstractDeploymentProviderService {

  private static final Logger LOG = LogManager.getLogger(DeploymentController.class);

  @Value("${url}")
  private String IM_URL;

  @Value("${auth.file.path}")
  private String AUTH_FILE_PATH;

  @Autowired
  private DeploymentRepository deploymentRepository;

  @Autowired
  private ResourceRepository resourceRepository;

  private InfrastructureManagerApiClient imClient;

  private String infrastructureId;
  private InfrastructureStatus status;

  /**
   * Initialize the {@link InfrastructureManagerApiClient}.
   * 
   * @throws AuthFileNotFoundException
   *           if the file doesn't exist
   * @throws URISyntaxException
   */
  @PostConstruct
  private void init() throws AuthFileNotFoundException, IOException, URISyntaxException {
    String completeFilePath = ImServiceImpl.class.getClassLoader().getResource(AUTH_FILE_PATH)
        .toURI().getPath();

    // remove initial slash from windows paths
    completeFilePath = completeFilePath.replaceFirst("^/(.:/)", "$1");

    imClient = new InfrastructureManagerApiClient(IM_URL, completeFilePath);
  }

  @Override
  public void doDeploy(String deploymentUuid) {
    Deployment deployment = deploymentRepository.findOne(deploymentUuid);
    try {
      // Update status of the deployment
      deployment.setTask(Task.DEPLOY);
      deployment.setDeploymentProvider(DeploymentProvider.IM);
      deployment = deploymentRepository.save(deployment);

      // TODO improve with template inputs
      ServiceResponse response = imClient.createInfrastructure(deployment.getTemplate());
      if (!response.isReponseSuccessful()) {
        deployment.setStatus(Status.CREATE_FAILED);
        deployment.setStatusReason(response.getReasonPhrase());
        deployment = deploymentRepository.save(deployment);
      } else {
        String[] parsedURI = response.getResult().split("/");
        infrastructureId = parsedURI[parsedURI.length - 1];
        deployment.setEndpoint(infrastructureId);
        deployment = deploymentRepository.save(deployment);
        try {
          boolean result = doPoller();
          if (result) {
            // Update the deployment entity
            updateOnSuccess(deploymentUuid);
            Resource resource;
            for (Map.Entry<String, String> entry : status.getVmStates().entrySet()) {
              resource = new Resource();
              resource.setResourceType(entry.getKey());
              resource.setStatus(getOrchestratorStatusFromImStatus(entry.getValue()));
              resource.setDeployment(deployment);
              resourceRepository.save(resource);
            }
          } else {
            updateOnError(deploymentUuid);
          }
        } catch (Exception e) {
          LOG.error(e);
          updateOnError(deploymentUuid);
        }
      }
    } catch (AuthFileNotFoundException e) {
      LOG.error(e);
      updateOnError(deploymentUuid);
    }
  }

  private Status getOrchestratorStatusFromImStatus(String value) {
    VmStates vmState = VmStates.getEnumFromValue(value);
    switch (vmState) {
      case CONFIGURED:
      case PENDING:
        return Status.CREATE_IN_PROGRESS;

      case FAILED:
      case UNCONFIGURED:
        return Status.CREATE_FAILED;

      case RUNNING:
        return Status.CREATE_COMPLETE;

      // TODO Understand if we need other Status
      case OFF:
      case STOPPED:
      case UNKNOWN:
        return Status.UNKNOWN;

      default:
        return Status.UNKNOWN;
    }
  }

  @Override
  public boolean isDeployed() throws DeploymentException {
    try {

      ServiceResponse response = imClient.getInfrastructureState(infrastructureId);
      status = new ObjectMapper().readValue(response.getResult(), InfrastructureStatus.class);

      // FIXME Are the infrastructure states equals to the VmStates?
      if (status.getState().equals(VmStates.RUNNING.toString())) {
        return true;
      } else if (status.getState().equals(VmStates.FAILED.toString())) {
        throw new DeploymentException(
            "Fail to deploy infrastructure: <" + infrastructureId + "> " + response.getResult());
      } else {
        return false;
      }
    } catch (AuthFileNotFoundException | IOException e) {
      // TODO improve exception handling
      LOG.error(e);
      return false;
    }
  }

}
