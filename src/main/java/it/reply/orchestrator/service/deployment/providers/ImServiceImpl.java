package it.reply.orchestrator.service.deployment.providers;

import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;

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
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.ToscaService;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@PropertySource("classpath:im-config/im-java-api.properties")
public class ImServiceImpl extends AbstractDeploymentProviderService {

  private static final Logger LOG = LogManager.getLogger(ImServiceImpl.class);

  @Autowired
  private ApplicationContext ctx;

  @Value("${onedock.proxy.file.path}")
  private String proxyPath;

  @Value("${url}")
  private String imUrl;

  @Value("${auth.file.path}")
  private String authFilePath;

  @Value("${opennebula.auth.file.path}")
  private String opennebulaAuthFilePath;

  @Value("${openstack.auth.file.path}")
  private String openstackAuthFilePath;

  @Value("${onedock.auth.file.path}")
  private String onedockAuthFilePath;

  private static final Pattern UUID_PATTERN = Pattern.compile(".*\\/([^\\/]+)\\/?");
  private static final Pattern PRE_PATTERN = Pattern.compile(".*<pre>(.*)<\\/pre>.*");

  @Autowired
  private ToscaService toscaService;

  @Autowired
  private DeploymentRepository deploymentRepository;

  @Autowired
  private ResourceRepository resourceRepository;

  public enum IaaSSite {
    // @formatter:off
    OPENSTACK, OPENNEBULA, ONEDOCK
    // @formatter:on
  }

  private InfrastructureManagerApiClient getClient(IaaSSite iaaSSite) {
    InputStream inputStream = null;
    try {
      switch (iaaSSite) {
        case OPENSTACK:
          LOG.debug("Load {} credentials", IaaSSite.OPENSTACK.toString());

          inputStream = ctx.getResource(openstackAuthFilePath).getInputStream();
          break;
        case ONEDOCK:
          LOG.debug("Load {} credentials", IaaSSite.ONEDOCK.toString());
          // Read the proxy file
          String proxy;
          try (InputStream in = ctx.getResource(proxyPath).getInputStream()) {
            proxy = IOUtils.toString(in);
            proxy = proxy.replace(System.lineSeparator(), "\\\\n");
          } catch (Exception ex) {
            throw new OrchestratorException("Cannot load proxy file", ex);
          }
          // read onedock auth file
          inputStream = ctx.getResource(onedockAuthFilePath).getInputStream();
          String authFile = IOUtils.toString(inputStream, StandardCharsets.UTF_8.toString());
          inputStream.close();

          // replace the proxy as string
          authFile = authFile.replace("{proxy}", proxy);

          inputStream = IOUtils.toInputStream(authFile, StandardCharsets.UTF_8.toString());
          break;
        case OPENNEBULA:
          LOG.debug("Load {} credentials", IaaSSite.OPENNEBULA.toString());
          inputStream = ctx.getResource(opennebulaAuthFilePath).getInputStream();
          break;
        default:
          LOG.debug("Load fake credentials");
          inputStream = ctx.getResource(authFilePath).getInputStream();
          break;
      }

      File tmp = File.createTempFile("authFileTmp", ".tmp");
      try (OutputStream outStream = new FileOutputStream(tmp)) {
        IOUtils.copy(inputStream, outStream);
      }
      InfrastructureManagerApiClient imClient =
          new InfrastructureManagerApiClient(imUrl, tmp.getAbsolutePath());
      return imClient;
    } catch (IOException | ImClientException ex) {
      throw new OrchestratorException("Cannot load IM auth file", ex);
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException ex) {
          LOG.catching(ex);
        }
      }
    }
  }

  private IaaSSite getIaaSSiteFromTosca(String template) {

    if (template.contains("tosca.nodes.indigo.MesosMaster")) {
      return IaaSSite.OPENSTACK;
    } else if (template.contains("onedock.i3m.upv.es")) {
      return IaaSSite.ONEDOCK;
    } else {
      return IaaSSite.OPENNEBULA;
    }
  }

  @Override
  public boolean doDeploy(String deploymentUuid) {
    Deployment deployment = deploymentRepository.findOne(deploymentUuid);
    return doDeploy(deployment);
  }

  @Override
  public boolean doDeploy(Deployment deployment) {
    String deploymentUuid = deployment.getId();
    try {
      // Update status of the deployment
      deployment.setTask(Task.DEPLOYER);
      deployment.setDeploymentProvider(DeploymentProvider.IM);
      deployment = deploymentRepository.save(deployment);

      // FIXME this is a trick used only for demo purpose
      InfrastructureManagerApiClient imClient =
          getClient(getIaaSSiteFromTosca(deployment.getTemplate()));

      // TODO improve with template inputs
      ServiceResponse response =
          imClient.createInfrastructure(deployment.getTemplate(), RestApiBodyContentType.TOSCA);
      if (!response.isReponseSuccessful()) {
        LOG.error(response.getResult());
        // IM response is HTML encoded. Get the message between <pre> </pre> tag
        Matcher matcher = PRE_PATTERN.matcher(response.getResult());
        String responseError = "Error during the deployment " + response.getResult();
        if (matcher.matches()) {
          responseError = matcher.group(1);
        }
        updateOnError(deploymentUuid, response.getReasonPhrase() + ": " + responseError);
        return false;
      } else {
        String infrastructureId = null;
        Matcher matcher = UUID_PATTERN.matcher(response.getResult());
        if (matcher.matches()) {
          infrastructureId = matcher.group(1);
          deployment.setEndpoint(infrastructureId);
          deployment = deploymentRepository.save(deployment);
          return true;
        } else {
          updateOnError(deploymentUuid,
              String.format(
                  "Creation of deployment <%s>: Couldn't extract infrastructureId from IM endpoint."
                      + "\nIM endpoint was %s.",
                  deploymentUuid, response.getResult()));
          return false;
        }
      }
    } catch (Exception e) {
      LOG.error(e);
      updateOnError(deploymentUuid, e);
      return false;
    }
  }

  @Override
  public boolean isDeployed(String deploymentUuid) throws DeploymentException {
    Deployment deployment = deploymentRepository.findOne(deploymentUuid);
    return isDeployed(deployment);
  }

  @Override
  public boolean isDeployed(Deployment deployment) throws DeploymentException {
    try {
      // FIXME this is a trick used only for demo purpose
      InfrastructureManagerApiClient imClient =
          getClient(getIaaSSiteFromTosca(deployment.getTemplate()));
      ServiceResponse response = imClient.getInfrastructureState(deployment.getEndpoint());
      InfrastructureStatus status =
          new ObjectMapper().readValue(response.getResult(), InfrastructureStatus.class);

      // FIXME Are the infrastructure states equals to the VmStates?
      if (status.getState().equals(VmStates.CONFIGURED.toString())) {
        return true;
      } else if (status.getState().equals(VmStates.FAILED.toString())) {
        throw new DeploymentException("Fail to deploy infrastructure: <"
            + deployment.getEndpoint() + "> " + response.getResult());
      } else {
        return false;
      }
    } catch (ImClientException | IOException ex) {
      // TODO improve exception handling
      LOG.error(ex);
      return false;
    }
  }

  @Override
  public void finalizeDeploy(String deploymentUuid, boolean deployed) {
    Deployment deployment = deploymentRepository.findOne(deploymentUuid);
    finalizeDeploy(deployment, deployed);
  }

  @Override
  public void finalizeDeploy(Deployment deployment, boolean deployed) {
    if (deployed) {
      try {
        // FIXME this is a trick used only for demo purpose
        InfrastructureManagerApiClient imClient =
            getClient(getIaaSSiteFromTosca(deployment.getTemplate()));
        // Save outputs
        es.upv.i3m.grycap.im.api.InfrastructureStatus statusResponse =
            imClient.getInfrastructureOutputs(deployment.getEndpoint());

        Map<String, String> outputs = new HashMap<String, String>();
        for (Entry<String, Object> entry : statusResponse.getProperties().entrySet()) {
          if (entry.getValue() != null) {
            outputs.put(entry.getKey(), entry.getValue().toString());
          } else {
            outputs.put(entry.getKey(), "");
          }
        }
        deployment.setOutputs(outputs);

        bindResources(deployment, deployment.getEndpoint());

        updateOnSuccess(deployment.getId());

      } catch (Exception ex) {
        LOG.error(ex);
        updateOnError(deployment.getId(), ex);
      }
    } else {
      updateOnError(deployment.getId());
    }
  }

  @Override
  public void doUpdate(String deploymentId, String template) {
    Deployment deployment = deploymentRepository.findOne(deploymentId);
    doUpdate(deployment, template);
  }

  @Override
  public void doUpdate(Deployment deployment, String template) {

    // Check if count is increased or if there is a removal list, other kinds of update are
    // discarded

    ParsingResult<ArchiveRoot> oldParsingResult;
    ParsingResult<ArchiveRoot> newParsingResult;
    try {
      // FIXME Fugly
      oldParsingResult = toscaService.getArchiveRootFromTemplate(deployment.getTemplate());
      template = toscaService.customizeTemplate(template, deployment.getId());
      newParsingResult = toscaService.getArchiveRootFromTemplate(template);
    } catch (ParsingException | IOException ex) {
      throw new OrchestratorException(ex);
    }
    // find Count nodes into new and old template
    Map<String, NodeTemplate> oldNodes = toscaService.getCountNodes(oldParsingResult.getResult());
    Map<String, NodeTemplate> newNodes = toscaService.getCountNodes(newParsingResult.getResult());

    try {
      for (Map.Entry<String, NodeTemplate> entry : oldNodes.entrySet()) {
        if (newNodes.containsKey(entry.getKey())) {
          int oldCount = toscaService.getCount(entry.getValue());
          int newCount = toscaService.getCount(newNodes.get(entry.getKey()));
          if (newCount > oldCount) {
            Resource resource;
            for (int i = 0; i < (newCount - oldCount); i++) {
              resource = new Resource();
              resource.setDeployment(deployment);
              resource.setStatus(Status.CREATE_IN_PROGRESS);
              resource.setToscaNodeName(entry.getKey());
              resource.setToscaNodeType(entry.getValue().getType());
              resourceRepository.save(resource);
            }

            ArchiveRoot root = newParsingResult.getResult();
            Map<String, NodeTemplate> nodes = new HashMap<>();
            nodes.put(entry.getKey(), newNodes.get(entry.getKey()));
            root.getTopology().setNodeTemplates(nodes);
            // FIXME this is a trick used only for demo purpose
            InfrastructureManagerApiClient imClient =
                getClient(getIaaSSiteFromTosca(deployment.getTemplate()));
            ServiceResponse response =
                imClient.addResource(deployment.getEndpoint(),
                    toscaService.getTemplateFromTopology(root), RestApiBodyContentType.TOSCA, true);

            if (!response.isReponseSuccessful()) {
              // IM response is HTML encoded. Get the message between <pre> </pre> tag
              String responseError =
                  response.getResult().substring(response.getResult().indexOf("<pre>") + 5,
                      response.getResult().indexOf("</pre>"));
              // updateOnError(deploymentId, response.getReasonPhrase() + ": " + responseError);
              throw new DeploymentException(response.getReasonPhrase() + ": " + responseError);
            } else {

              boolean result = doPoller(this::isDeployed, deployment);
              if (!result) {
                throw new DeploymentException("An error occur during the update: polling failed");
              }
            }
          } else if (newCount < oldCount) {
            // delete a WN.
            List<String> removalList = toscaService.getRemovalList(newNodes.get(entry.getKey()));
            if (removalList.size() != (oldCount - newCount)) {
              throw new DeploymentException("An error occur during the update. Count is <"
                  + newCount + "> but removal_list contains <" + removalList.size()
                  + "> elements in the node: " + entry.getKey());
            }

            // Find the nodes to be removed.
            for (String resourceId : removalList) {
              Resource resource = resourceRepository.findOne(resourceId);
              resource.setStatus(Status.DELETE_IN_PROGRESS);
              resource = resourceRepository.save(resource);
              // FIXME this is a trick used only for demo purpose
              InfrastructureManagerApiClient imClient =
                  getClient(getIaaSSiteFromTosca(deployment.getTemplate()));
              ServiceResponse response =
                  imClient.removeResource(deployment.getEndpoint(), resource.getIaasId());
              if (!response.isReponseSuccessful()) {
                if (response.getServiceStatusCode() != 404) {
                  throw new DeploymentException("An error occur during the update: fail to delete: "
                      + resource.getToscaNodeName() + " with id: " + resourceId);
                }
              } else {
                boolean result = doPoller(this::isResourceDeleted, resource);
                if (result) {
                  List<Resource> resurces = deployment.getResources();
                  resurces.remove(resource);
                  resourceRepository.delete(resourceId);
                } else {
                  throw new DeploymentException(
                      "An error occur during the update: polling failed " + resource.getId());
                }

              }
            }
          }
        }
      }
      bindResources(deployment, deployment.getEndpoint());
      deployment.setTemplate(toscaService.updateTemplate(template));
      updateOnSuccess(deployment.getId());
    } catch (ImClientException | IOException | DeploymentException ex) {
      updateOnError(deployment.getId(), ex);
    }

  }

  private Status getOrchestratorStatusFromImStatus(String value) {
    VmStates vmState = VmStates.getEnumFromValue(value);
    switch (vmState) {
      case PENDING:
      case RUNNING:
        return Status.CREATE_IN_PROGRESS;

      case FAILED:
      case UNCONFIGURED:
        return Status.CREATE_FAILED;

      case CONFIGURED:
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
  public boolean doUndeploy(String deploymentUuid) {
    Deployment deployment = deploymentRepository.findOne(deploymentUuid);
    return doUndeploy(deployment);
  }

  @Override
  public boolean doUndeploy(Deployment deployment) {
    String deploymentUuid = deployment.getId();
    try {
      // Update status of the deployment
      deployment.setTask(Task.DEPLOYER);
      deployment = deploymentRepository.save(deployment);

      // FIXME this is a trick used only for demo purpose
      InfrastructureManagerApiClient imClient =
          getClient(getIaaSSiteFromTosca(deployment.getTemplate()));
      if (deployment.getEndpoint() == null) {
        updateOnSuccess(deploymentUuid);
        return true;
      }
      ServiceResponse response = imClient.destroyInfrastructure(deployment.getEndpoint());
      if (!response.isReponseSuccessful()) {
        if (response.getServiceStatusCode() == 404) {
          updateOnSuccess(deploymentUuid);
          return true;
        } else {
          updateOnError(deploymentUuid, response.getReasonPhrase());
          return false;
        }
      } else {
        return true;
      }
    } catch (Exception ex) {
      LOG.error(ex);
      updateOnError(deploymentUuid, ex);
      return false;
    }
  }

  @Override
  public boolean isUndeployed(String deploymentUuid) throws DeploymentException {
    Deployment deployment = deploymentRepository.findOne(deploymentUuid);
    return isUndeployed(deployment);
  }

  @Override
  public boolean isUndeployed(Deployment deployment) throws DeploymentException {
    try {

      // FIXME this is a trick used only for demo purpose
      InfrastructureManagerApiClient imClient =
          getClient(getIaaSSiteFromTosca(deployment.getTemplate()));
      if (deployment.getEndpoint() == null) {
        return true;
      }
      ServiceResponse response = imClient.getInfrastructureState(deployment.getEndpoint());
      if (!response.isReponseSuccessful()) {
        if (response.getServiceStatusCode() == 404) {
          return true;
        } else {
          return false;
        }
      } else {
        return false;
      }
    } catch (ImClientException ex) {
      // TODO improve exception handling
      LOG.error(ex);
      return false;
    }
  }

  /**
   * Check if a resource is deleted.
   * 
   * @return <code>true</code> if the resource is deleteted.
   */
  @Override
  public void finalizeUndeploy(String deploymentUuid, boolean undeployed) {
    if (undeployed) {
      updateOnSuccess(deploymentUuid);
    } else {
      updateOnError(deploymentUuid);
    }
  }

  private boolean isResourceDeleted(Resource resource) {
    try {
      Deployment deployment = resource.getDeployment();
      // FIXME this is a trick used only for demo purpose
      InfrastructureManagerApiClient imClient =
          getClient(getIaaSSiteFromTosca(deployment.getTemplate()));

      ServiceResponse response =
          imClient.getVMInfo(deployment.getEndpoint(), resource.getIaasId(), true);
      if (!response.isReponseSuccessful()) {
        if (response.getServiceStatusCode() == 404) {
          return true;
        } else {
          return false;
        }
      } else {
        return false;
      }
    } catch (ImClientException ex) {
      // TODO improve exception handling
      LOG.error(ex);
      return false;
    }
  }

  /**
   * Match the {@link Resource} to IM vms.
   * 
   */
  private void bindResources(Deployment deployment, String infrastructureId)
      throws ImClientException {

    // FIXME this is a trick used only for demo purpose
    InfrastructureManagerApiClient imClient =
        getClient(getIaaSSiteFromTosca(deployment.getTemplate()));

    // Get the URLs of the VMs composing the virtual infrastructure
    String[] vmUrls = imClient.getInfrastructureInfo(infrastructureId).getResult().split("\\r?\\n");

    // for each URL get the information about the VM
    Map<String, String> vmMap = new HashMap<String, String>();
    for (String vm : vmUrls) {
      String vmId = null;
      int index = vm.lastIndexOf("/");
      if (index != -1) {
        vmId = vm.substring(index + 1);
      }
      String vmInfo = imClient.getVMInfo(infrastructureId, vmId, true).getResult();
      vmMap.put(vmId, vmInfo);
    }

    // Find the Resource from the DB and bind it with the corresponding VM
    Page<Resource> resources = resourceRepository.findByDeployment_id(deployment.getId(), null);

    // Remove from vmMap all the resources already binded
    for (Resource r : resources) {
      if (r.getIaasId() != null) {
        vmMap.remove(r.getIaasId());
      }
    }

    Set<String> insered = new HashSet<String>();
    for (Resource r : resources) {
      if (r.getStatus() == Status.CREATE_IN_PROGRESS
          || r.getStatus() == Status.UPDATE_IN_PROGRESS) {
        for (Map.Entry<String, String> entry : vmMap.entrySet()) {
          if (entry.getValue().contains(r.getToscaNodeName())
              && !insered.contains(entry.getKey())) {
            r.setIaasId(entry.getKey());
            insered.add(entry.getKey());
            break;
          }
        }
      }
    }
  }

}
