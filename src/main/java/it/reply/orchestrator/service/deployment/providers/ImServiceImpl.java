package it.reply.orchestrator.service.deployment.providers;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

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
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dto.im.InfrastructureStatus;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.exception.DeploymentException;

@Service
@PropertySource("classpath:im-config/im-java-api.properties")
public class ImServiceImpl extends AbstractDeploymentProviderService {

  private static final Logger LOG = LogManager.getLogger(DeploymentController.class);

  @Value("${url}")
  private String IM_URL;

  @Value("${auth.file.path}")
  private String AUTH_FILE_PATH;

  private InfrastructureManagerApiClient imClient;

  private String infrastructureId;

  @Autowired
  private DeploymentRepository deploymentRepository;

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

      ServiceResponse response = imClient.createInfrastructure(deployment.getTemplate());
      if (!response.isReponseSuccessful()) {
        // deployment = deploymentRepository.findOne(deploymentUuid);
        deployment.setStatus(Status.CREATE_FAILED);
        deployment.setStatusReason(response.getReasonPhrase());
        deployment = deploymentRepository.save(deployment);
      } else {
        String[] parsedURI = response.getResult().split("/");
        infrastructureId = parsedURI[parsedURI.length - 1];
        // deployment = deploymentRepository.findOne(deploymentUuid);
        deployment.setEndpoint(infrastructureId);
        deployment = deploymentRepository.save(deployment);
        try {
          boolean result = doPoller();
          if (result) {
            updateOnSuccess(deploymentUuid);
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

  @Override
  public boolean isDeployed() throws DeploymentException {
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

}
