package it.reply.orchestrator.service.deployment.providers;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.upv.i3m.grycap.im.api.InfrastructureManagerApiClient;
import es.upv.i3m.grycap.im.api.RestApiBodyContentType;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

@Service
@PropertySource("classpath:im-config/im-java-api.properties")
public class ImServiceImpl extends AbstractDeploymentProviderService {

  private static final Logger LOG = LogManager.getLogger(DeploymentController.class);

  @Value("${url}")
  private String IM_URL;

  @Value("${auth.file.path}")
  private String AUTH_FILE_PATH;

  private static final Pattern UUID_PATTERN = Pattern.compile(".*\\/([^\\/]+)\\/?");

  private InfrastructureManagerApiClient imClient;

  @Autowired
  private DeploymentRepository deploymentRepository;

  @Autowired
  private ResourceRepository resourceRepository;

  /**
   * Initialize the {@link InfrastructureManagerApiClient}.
   * 
   * @throws AuthFileNotFoundException
   *           if the file doesn't exist
   * @throws URISyntaxException
   *           if an error occurred while generating the auth file URI
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
      deployment.setTask(Task.DEPLOYER);
      deployment.setDeploymentProvider(DeploymentProvider.IM);
      deployment = deploymentRepository.save(deployment);

      // TODO improve with template inputs
      ServiceResponse response = imClient.createInfrastructure(deployment.getTemplate(),
          RestApiBodyContentType.TOSCA);
      if (!response.isReponseSuccessful()) {
        updateOnError(deploymentUuid, response.getReasonPhrase());
      } else {
        String infrastructureId = null;
        Matcher m = UUID_PATTERN.matcher(response.getResult());
        if (m.matches()) {
          infrastructureId = m.group(1);
          deployment.setEndpoint(infrastructureId);
          deployment = deploymentRepository.save(deployment);
        } else {
          throw new DeploymentException(String.format(
              "Creation of deployment < %s > : Couldn't extract infrastructureId from IM endpoint.\nIM endpoint was %s.",
              deploymentUuid, response.getResult()));
        }

        try {
          boolean result = doPoller(infrastructureId, this::isDeployed);
          if (result) {
            // Update the deployment entity
            response = imClient.getInfrastructureState(infrastructureId);
            InfrastructureStatus status = new ObjectMapper().readValue(response.getResult(),
                InfrastructureStatus.class);
            for (Map.Entry<String, String> entry : status.getVmStates().entrySet()) {
              Resource resource = new Resource();
              resource.setResourceType(entry.getKey());
              resource.setStatus(getOrchestratorStatusFromImStatus(entry.getValue()));
              resource.setDeployment(deployment);
              resourceRepository.save(resource);
            }
            updateOnSuccess(deploymentUuid);
          } else {
            updateOnError(deploymentUuid);
          }
        } catch (Exception e) {
          LOG.error(e);
          updateOnError(deploymentUuid, e);
        }
      }
    } catch (Exception e) {
      LOG.error(e);
      updateOnError(deploymentUuid, e);
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
  public boolean isDeployed(String infrastructureId) throws DeploymentException {
    try {

      ServiceResponse response = imClient.getInfrastructureState(infrastructureId);
      InfrastructureStatus status = new ObjectMapper().readValue(response.getResult(),
          InfrastructureStatus.class);

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

  @Override
  public void doUndeploy(String deploymentUuid) {
    Deployment deployment = deploymentRepository.findOne(deploymentUuid);
    try {
      // Update status of the deployment
      deployment.setTask(Task.DEPLOYER);
      deployment = deploymentRepository.save(deployment);

      ServiceResponse response = imClient.destroyInfrastructure(deployment.getEndpoint());
      if (!response.isReponseSuccessful()) {
        if (response.getServiceStatusCode() == 404) {
          updateOnSuccess(deploymentUuid);
        } else {
          updateOnError(deploymentUuid, response.getReasonPhrase());
        }
      } else {
        try {
          boolean result = doPoller(deploymentUuid, this::isUndeployed);
          if (result) {
            updateOnSuccess(deploymentUuid);
          } else {
            updateOnError(deploymentUuid);
          }
        } catch (Exception e) {
          LOG.error(e);
          updateOnError(deploymentUuid, e);
        }
      }
    } catch (Exception e) {
      LOG.error(e);
      updateOnError(deploymentUuid, e);
    }
  }

  @Override
  public boolean isUndeployed(String deploymentUuid) throws DeploymentException {
    try {
      ServiceResponse response = imClient.getInfrastructureState(deploymentUuid);
      if (!response.isReponseSuccessful()) {
        if (response.getServiceStatusCode() == 404) {
          return true;
        } else {
          return false;
        }
      } else {
        return false;
      }
    } catch (AuthFileNotFoundException e) {
      // TODO improve exception handling
      LOG.error(e);
      return false;
    }
  }

}
