package it.reply.orchestrator.service.deployment.providers;

import com.google.common.io.ByteStreams;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.upv.i3m.grycap.im.api.InfrastructureManagerApiClient;
import es.upv.i3m.grycap.im.api.RestApiBodyContentType;
import es.upv.i3m.grycap.im.api.VmStates;
import es.upv.i3m.grycap.im.client.ServiceResponse;
import es.upv.i3m.grycap.im.exceptions.ImClientException;

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
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

@Service
@PropertySource("classpath:im-config/im-java-api.properties")
public class ImServiceImpl extends AbstractDeploymentProviderService {

  private static final Logger LOG = LogManager.getLogger(ImServiceImpl.class);

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
  private void init() throws ImClientException, IOException, URISyntaxException {

    InputStream inputStream = ImServiceImpl.class.getClassLoader()
        .getResourceAsStream(AUTH_FILE_PATH);

    File tmp = File.createTempFile("authFileTmp", ".tmp");
    OutputStream outStream = new FileOutputStream(tmp);
    ByteStreams.copy(inputStream, new FileOutputStream(tmp));
    inputStream.close();
    outStream.close();

    imClient = new InfrastructureManagerApiClient(IM_URL, tmp.getAbsolutePath());
  }

  @Override
  public void doDeploy(String deploymentUuid) {
    Deployment deployment = deploymentRepository.findOne(deploymentUuid);
    doDeploy(deployment);
  }

  @Override
  public void doDeploy(Deployment deployment) {
    String deploymentUuid = deployment.getId();
    try {
      // Update status of the deployment
      deployment.setTask(Task.DEPLOYER);
      deployment.setDeploymentProvider(DeploymentProvider.IM);
      deployment = deploymentRepository.save(deployment);

      // TODO improve with template inputs
      ServiceResponse response = imClient.createInfrastructure(deployment.getTemplate(),
          RestApiBodyContentType.TOSCA);
      if (!response.isReponseSuccessful()) {
        // IM response is HTML encoded. Get the message between <pre> </pre> tag
        String responseError = response.getResult().substring(
            response.getResult().indexOf("<pre>") + 5, response.getResult().indexOf("</pre>"));
        updateOnError(deploymentUuid, response.getReasonPhrase() + ": " + responseError);
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
            // Save outputs
            es.upv.i3m.grycap.im.api.InfrastructureStatus statusResponse = imClient
                .getInfrastructureOutputs(infrastructureId);

            Map<String, String> outputs = new HashMap<String, String>();
            for (Entry<String, Object> entry : statusResponse.getProperties().entrySet()) {
              if (entry.getValue() != null) {
                outputs.put(entry.getKey(), entry.getValue().toString());
              } else {
                outputs.put(entry.getKey(), "");
              }
            }
            deployment.setOutputs(outputs);
            // deployment.setOutputs(statusResponse.getProperties().entrySet().stream()
            // .collect(Collectors.toMap(e -> ((Map.Entry<String, Object>) e).getKey(),
            // e -> ((Map.Entry<String, Object>) e).getValue().toString())));

            bindResources(deployment, infrastructureId);

            updateOnSuccess(deploymentUuid);
          } else {
            updateOnError(deploymentUuid, "An error occured during the deployment of the template");
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
    } catch (ImClientException | IOException e) {
      // TODO improve exception handling
      LOG.error(e);
      return false;
    }
  }

  @Override
  public void doUndeploy(String deploymentUuid) {
    Deployment deployment = deploymentRepository.findOne(deploymentUuid);
    doUndeploy(deployment);
  }

  @Override
  public void doUndeploy(Deployment deployment) {
    String deploymentUuid = deployment.getId();
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
    } catch (ImClientException e) {
      // TODO improve exception handling
      LOG.error(e);
      return false;
    }
  }

  /**
   * Match the {@link Resource} to IM vms.
   * 
   */
  private void bindResources(Deployment deployment, String infrastructureId)
      throws ImClientException {

    // Get the URLs of the VMs composing the virtual infrastructure
    String[] vmURLs = imClient.getInfrastructureInfo(infrastructureId).getResult().split("\\r?\\n");

    // for each URL get the information about the VM
    Map<String, String> vmMap = new HashMap<String, String>();
    for (String vm : vmURLs) {
      String vmId = null;
      int index = vm.lastIndexOf("/");
      if (index != -1) {
        vmId = vm.substring(index + 1);
      }
      String vmInfo = imClient.getVMInfo(infrastructureId, vmId, true).getResult();
      vmMap.put(vmId, vmInfo);
    }

    // Put all the resource status to CREATE_COMPLETE.
    // Find the Resource from the DB and bind it with the corresponding VM
    Page<Resource> resources = resourceRepository.findByDeployment_id(deployment.getId(), null);
    Set<String> insered = new HashSet<String>();
    for (Resource r : resources) {
      for (Map.Entry<String, String> entry : vmMap.entrySet()) {
        if (entry.getValue().contains(r.getToscaNodeName()) && !insered.contains(entry.getKey())) {
          r.setIaasId(entry.getKey());
          insered.add(entry.getKey());
          break;
        }
      }

    }
  }

}
