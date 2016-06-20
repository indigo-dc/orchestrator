package it.reply.orchestrator.service.deployment.providers;

import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingException;

import es.upv.i3m.grycap.im.InfrastructureManager;
import es.upv.i3m.grycap.im.States;
import es.upv.i3m.grycap.im.exceptions.ImClientErrorException;
import es.upv.i3m.grycap.im.exceptions.ImClientException;
import es.upv.i3m.grycap.im.pojo.InfOutputValues;
import es.upv.i3m.grycap.im.pojo.InfrastructureState;
import es.upv.i3m.grycap.im.pojo.InfrastructureUri;
import es.upv.i3m.grycap.im.pojo.InfrastructureUris;
import es.upv.i3m.grycap.im.pojo.Property;
import es.upv.i3m.grycap.im.pojo.ResponseError;
import es.upv.i3m.grycap.im.pojo.VirtualMachineInfo;
import es.upv.i3m.grycap.im.rest.client.BodyContentType;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.CloudProviderEndpoint.IaaSType;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.exception.service.ToscaException;
import it.reply.orchestrator.service.ToscaService;
import it.reply.utils.json.JsonUtility;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Qualifier("IM")
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

  private static final Pattern UUID_PATTERN = Pattern.compile(".*\\/([^\"\\/]+)\\/?\"?");
  private static final Pattern VM_ID_PATTERN = Pattern.compile("(\\w+)$");

  @Autowired
  private ToscaService toscaService;

  @Autowired
  private DeploymentRepository deploymentRepository;

  @Autowired
  private ResourceRepository resourceRepository;

  private InfrastructureManager getClient(DeploymentMessage dm) {
    // LOG.debug("Load {} credentials", cpe.getIaasType());
    // switch (cpe.getIaasType()) {
    // case ONEDOCK:
    // break;
    // case OPENNEBULA:
    // InfrastructureManager im = new InfrastructureManager(imUrl, tmp.toPath());
    // break;
    // case OPENSTACK:
    // InfrastructureManager im = new InfrastructureManager(imUrl, tmp.toPath());
    // break;
    //
    // }
    //
    // AuthorizationHeader ah = new AuthorizationHeader();
    // // Authenticate to IM with the OAuth2 token
    // Credential<?> cred = ImTokenCredential.getBuilder().withToken(dm.getOauth2Token()).build();
    // ah.addCredential(cred);
    //
    // cred = VmrcCredential.getBuilder().withUsername("demo").withPassword("pwd")
    // .withHost("http://servproject.i3m.upv.es:8080/vmrc/vmrc").build();
    // ah.addCredential(cred);
    // cred = OpenstackCredential.getBuilder().withId("ost").withUsername("usr").withPassword("pwd")
    // .withTenant("tenant").withServiceRegion("recas-cloud")
    // .withHost("https://cloud.recas.ba.infn.it:5000").build();
    // InfrastructureManager im = new InfrastructureManager(imUrl, ah);
    // }
    // FIXME Use CMDB EP!

    IaaSType iaasType = dm.getChosenCloudProviderEndpoint().getIaasType();
    InputStream inputStream = null;
    try {
      LOG.debug("Load {} credentials with: {}", iaasType, dm.getChosenCloudProviderEndpoint());
      switch (iaasType) {
        case OPENSTACK:
          inputStream = ctx.getResource(openstackAuthFilePath).getInputStream();
          break;
        case ONEDOCK:
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
          inputStream = ctx.getResource(opennebulaAuthFilePath).getInputStream();
          break;
        default:
          throw new IllegalArgumentException(
              String.format("Unsupported provider type <%s>", iaasType));
      }

      File tmp = File.createTempFile("authFileTmp", ".tmp");
      try (OutputStream outStream = new FileOutputStream(tmp)) {
        IOUtils.copy(inputStream, outStream);
      }
      InfrastructureManager im = new InfrastructureManager(imUrl, tmp.toPath());
      return im;
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

  @Override
  public boolean doDeploy(DeploymentMessage deploymentMessage) {
    Deployment deployment = deploymentMessage.getDeployment();
    String deploymentUuid = deployment.getId();
    try {
      // Update status of the deployment
      deployment.setTask(Task.DEPLOYER);
      deployment.setDeploymentProvider(DeploymentProvider.IM);
      deployment = deploymentRepository.save(deployment);

      ArchiveRoot ar =
          toscaService.prepareTemplate(deployment.getTemplate(), deployment.getParameters());
      toscaService.addElasticClusterParameters(ar, deploymentUuid);
      toscaService.contextualizeImages(ar, deploymentMessage.getChosenCloudProvider());
      String imCustomizedTemplate = toscaService.getTemplateFromTopology(ar);

      // Generate IM Client
      InfrastructureManager im = getClient(deploymentMessage);

      // Deploy on IM
      InfrastructureUri infrastructureUri =
          im.createInfrastructure(imCustomizedTemplate, BodyContentType.TOSCA);

      String infrastructureId = infrastructureUri.getInfrastructureId();
      if (infrastructureId != null) {
        deployment.setEndpoint(infrastructureId);
        deployment = deploymentRepository.save(deployment);
        deploymentMessage.setCreateComplete(true);
        return true;
      } else {
        updateOnError(deploymentUuid,
            String.format(
                "Creation of deployment <%s>: Couldn't extract infrastructureId from IM endpoint."
                    + "\nIM endpoint was %s.",
                deploymentUuid, infrastructureUri.getUri()));
        return false;
      }
      // Exception generated when the im produces an error message
    } catch (ImClientErrorException exception) {
      logImErrorResponse(exception);
      ResponseError responseError = getImResponseError(exception);
      updateOnError(deploymentUuid, responseError.getFormattedErrorMessage());
      return false;

    } catch (Exception ex) {
      LOG.error(ex);
      updateOnError(deploymentUuid, ex);
      return false;
    }
  }

  @Override
  public boolean isDeployed(DeploymentMessage deploymentMessage) throws DeploymentException {
    Deployment deployment = deploymentMessage.getDeployment();
    InfrastructureManager im = null;
    try {
      // Generate IM Client
      im = getClient(deploymentMessage);

      InfrastructureState infrastructureState = im.getInfrastructureState(deployment.getEndpoint());
      LOG.debug(infrastructureState.toString());

      States enumState = infrastructureState.getEnumState();
      switch (enumState) {
        case CONFIGURED:
          deploymentMessage.setPollComplete(true);
          return true;
        case FAILED:
        case UNCONFIGURED:
          String errorMsg = String.format(
              "Fail to deploy deployment <%s>." + "\nIM id is: <%s>" + "\nIM response is: <%s>",
              deployment.getId(), deployment.getEndpoint(),
              infrastructureState.getFormattedInfrastructureStateString());
          try {
            // Try to get the logs of the virtual infrastructure for debug
            // purpose.
            Property contMsg = im.getInfrastructureContMsg(deployment.getEndpoint());
            errorMsg = errorMsg.concat("\nIM contMsg is: " + contMsg.getValue());
          } catch (Exception ex) {
            // Do nothing
          }
          DeploymentException ex = new DeploymentException(errorMsg);
          updateOnError(deployment.getId(), ex);
          LOG.error(errorMsg);
          throw ex;
        default:
          return false;
      }
    } catch (ImClientException exception) {
      String errorResponse = exception.getMessage();
      if (exception instanceof ImClientErrorException) {
        ImClientErrorException ex = (ImClientErrorException) exception;
        errorResponse = ex.getResponseError().getFormattedErrorMessage();
      }

      String errorMsg = String.format(
          "Fail to deploy deployment <%s>." + "\nIM id is: <%s>" + "\nIM error is: <%s>",
          deployment.getId(), deployment.getEndpoint(), errorResponse);
      try {
        // Try to get the logs of the virtual infrastructure for debug
        // purpose.
        Property contMsg = im.getInfrastructureContMsg(deployment.getEndpoint());
        errorMsg = errorMsg.concat("\nIM contMsg is: " + contMsg.getValue());
      } catch (Exception ex) {
        // Do nothing
      }
      LOG.error(errorMsg);
      throw new DeploymentException(errorMsg);
    }
  }

  @Override
  public void finalizeDeploy(DeploymentMessage deploymentMessage, boolean deployed) {

    Deployment deployment = deploymentMessage.getDeployment();
    if (deployed) {
      try {
        // Generate IM Client
        InfrastructureManager im = getClient(deploymentMessage);

        if (deployment.getOutputs().isEmpty()) {
          InfOutputValues outputValues = im.getInfrastructureOutputs(deployment.getEndpoint());
          Map<String, String> outputs = new HashMap<String, String>();
          for (Entry<String, Object> entry : outputValues.getOutputs().entrySet()) {
            if (entry.getValue() != null) {
              outputs.put(entry.getKey(), JsonUtility.serializeJson(entry.getValue()));
            } else {
              outputs.put(entry.getKey(), "");
            }
          }
          deployment.setOutputs(outputs);
        }
        bindResources(deployment, deployment.getEndpoint(), im);

        updateOnSuccess(deployment.getId());

      } catch (ImClientErrorException exception) {
        logImErrorResponse(exception);
        updateOnError(deployment.getId(), exception);

      } catch (Exception ex) {
        LOG.error(ex);
        updateOnError(deployment.getId(), ex);
      }
    } else {
      updateOnError(deployment.getId());
    }
  }

  @Override
  public boolean doUpdate(DeploymentMessage deploymentMessage, String template) {

    Deployment deployment = deploymentMessage.getDeployment();
    // Check if count is increased or if there is a removal list, other kinds of update are
    // discarded

    ArchiveRoot oldAr;
    ArchiveRoot newAr;
    try {
      // FIXME Fugly

      // Get TOSCA in-memory repr. of current template
      oldAr = toscaService.prepareTemplate(deployment.getTemplate(), deployment.getParameters());

      // Get TOSCA in-memory repr. of new template
      newAr = toscaService.prepareTemplate(template, deployment.getParameters());
      toscaService.addElasticClusterParameters(newAr, deployment.getId());
      toscaService.contextualizeImages(newAr, deploymentMessage.getChosenCloudProvider());
    } catch (ParsingException | IOException | ToscaException ex) {
      throw new OrchestratorException(ex);
    }
    // find Count nodes into new and old template
    Map<String, NodeTemplate> oldNodes = toscaService.getCountNodes(oldAr);
    Map<String, NodeTemplate> newNodes = toscaService.getCountNodes(newAr);

    try {
      // Create the new template with the nodes to be added
      ArchiveRoot root = newAr;
      Map<String, NodeTemplate> nodes = new HashMap<>();

      // List of vmIds to be removed
      List<String> vmIds = new ArrayList<String>();

      // Find difference between the old template and the new
      for (Map.Entry<String, NodeTemplate> entry : oldNodes.entrySet()) {
        if (newNodes.containsKey(entry.getKey())) {
          int oldCount = toscaService.getCount(entry.getValue());
          int newCount = toscaService.getCount(newNodes.get(entry.getKey()));
          List<String> removalList = toscaService.getRemovalList(newNodes.get(entry.getKey()));
          if (newCount > oldCount && removalList.size() == 0) {
            Resource resource;
            for (int i = 0; i < (newCount - oldCount); i++) {
              resource = new Resource();
              resource.setDeployment(deployment);
              resource.setState(NodeStates.CREATING);
              resource.setToscaNodeName(entry.getKey());
              resource.setToscaNodeType(entry.getValue().getType());
              resourceRepository.save(resource);
            }
            nodes.put(entry.getKey(), newNodes.get(entry.getKey()));

          } else if (newCount < oldCount && removalList.size() == (oldCount - newCount)) {
            // delete a WN.

            // Find the nodes to be removed.
            for (String resourceId : removalList) {
              Resource resource = resourceRepository.findOne(resourceId);
              resource.setState(NodeStates.DELETING);
              resource = resourceRepository.save(resource);
              vmIds.add(resource.getIaasId());
            }
          } else if (newCount == oldCount && removalList.size() == 0) {
            // do nothing
          } else {
            throw new DeploymentException("An error occur during the update. Count is <" + newCount
                + "> but removal_list contains <" + removalList.size() + "> elements in the node: "
                + entry.getKey());
          }
        }
      }

      // Find if there is a new TOSCA node
      for (Map.Entry<String, NodeTemplate> entry : newNodes.entrySet()) {
        if (!oldNodes.containsKey(entry.getKey())) {
          int count = toscaService.getCount(newNodes.get(entry.getKey()));
          Resource resource;
          for (int i = 0; i < count; i++) {
            resource = new Resource();
            resource.setDeployment(deployment);
            resource.setState(NodeStates.CREATING);
            resource.setToscaNodeName(entry.getKey());
            resource.setToscaNodeType(entry.getValue().getType());
            resourceRepository.save(resource);
          }
          nodes.put(entry.getKey(), newNodes.get(entry.getKey()));
        }
      }

      // Generate IM Client
      InfrastructureManager im = getClient(deploymentMessage);

      // Pulisco gli output e aggiungo i nodi da creare
      root.getTopology().setOutputs(null);
      root.getTopology().setNodeTemplates(nodes);
      if (!root.getTopology().isEmpty()) {
        try {
          im.addResource(deployment.getEndpoint(), toscaService.getTemplateFromTopology(root),
              BodyContentType.TOSCA);
        } catch (ImClientErrorException exception) {
          throw new DeploymentException(
              "An error occur during the update: fail to add new resources.", exception);
        }
      }
      // DELETE
      if (vmIds.size() > 0) {
        try {
          im.removeResource(deployment.getEndpoint(), vmIds);
        } catch (ImClientErrorException exception) {
          throw new DeploymentException(
              "An error occur during the update: fail to delete resources.", exception);
        }
      }
      // FIXME: There's not check if the Template actually changed!
      deployment.setTemplate(toscaService.updateTemplate(template));
      return true;
    } catch (ImClientException | IOException | DeploymentException ex) {
      updateOnError(deployment.getId(), ex);
      return false;
    }
  }

  @Override
  public boolean doUndeploy(DeploymentMessage deploymentMessage) {
    Deployment deployment = deploymentMessage.getDeployment();
    String deploymentUuid = deployment.getId();
    try {
      // Update status of the deployment
      deployment.setTask(Task.DEPLOYER);
      deployment = deploymentRepository.save(deployment);

      // Generate IM Client
      InfrastructureManager im = getClient(deploymentMessage);

      if (deployment.getEndpoint() == null) {
        // updateOnSuccess(deploymentUuid);
        deploymentMessage.setDeleteComplete(true);
        return true;
      }
      im.destroyInfrastructure(deployment.getEndpoint());
      deploymentMessage.setDeleteComplete(true);
      return true;

    } catch (ImClientErrorException exception) {
      logImErrorResponse(exception);
      ResponseError error = getImResponseError(exception);
      if (error.is404Error()) {
        // updateOnSuccess(deploymentUuid);
        return true;

      } else {
        updateOnError(deploymentUuid, error.getFormattedErrorMessage());
        return false;
      }

    } catch (Exception ex) {
      LOG.error(ex);
      updateOnError(deploymentUuid, ex);
      return false;
    }
  }

  @Override
  public boolean isUndeployed(DeploymentMessage deploymentMessage) {

    Deployment deployment = deploymentMessage.getDeployment();
    try {
      // Generate IM Client
      InfrastructureManager im = getClient(deploymentMessage);

      // TODO verificare
      if (deployment.getEndpoint() == null) {
        return true;
      }
      im.getInfrastructureState(deployment.getEndpoint());

      // If IM throws 404 the undeploy is complete
      // It is not, otherwise
      return false;

    } catch (ImClientErrorException exception) {
      ResponseError error = getImResponseError(exception);
      return error.is404Error();

    } catch (ImClientException ex) {
      // TODO improve exception handling
      LOG.error(ex);
      return false;
    }
  }

  /**
   * Check if a resource is deleted.
   */
  @Override
  public void finalizeUndeploy(DeploymentMessage deploymentMessage, boolean undeployed) {
    if (undeployed) {
      updateOnSuccess(deploymentMessage.getDeploymentId());
    } else {
      updateOnError(deploymentMessage.getDeploymentId());
    }
  }

  // private boolean isResourceDeleted(Resource resource) {
  // try {
  // Deployment deployment = resource.getDeployment();
  // // Generate IM Client
  // InfrastructureManager im = getClient(deploymentMessage);
  //
  // im.getVmInfo(deployment.getEndpoint(), resource.getIaasId());
  // return false;
  //
  // } catch (ImClientErrorException exception) {
  // ResponseError error = getImResponseError(exception);
  // return error.is404Error();
  //
  // } catch (ImClientException ex) {
  // // TODO improve exception handling
  // LOG.error(ex);
  // return false;
  // }
  // }

  /**
   * Match the {@link Resource} to IM vms.
   * 
   */
  private void bindResources(Deployment deployment, String infrastructureId,
      InfrastructureManager im) throws ImClientException {

    // Get the URLs of the VMs composing the virtual infrastructure
    // TODO test in case of errors
    InfrastructureUris vmUrls = im.getInfrastructureInfo(infrastructureId);

    // for each URL get the information about the VM
    Map<String, VirtualMachineInfo> vmMap = new HashMap<String, VirtualMachineInfo>();
    for (InfrastructureUri vmUri : vmUrls.getUris()) {
      String vmId = extractVmId(vmUri);
      VirtualMachineInfo vmInfo = im.getVmInfo(infrastructureId, vmId);
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
      if (r.getState() == NodeStates.CREATING || r.getState() == NodeStates.CONFIGURING
          || r.getState() == NodeStates.ERROR) {
        for (Map.Entry<String, VirtualMachineInfo> entry : vmMap.entrySet()) {
          if (entry.getValue().toString().contains(r.getToscaNodeName())
              && !insered.contains(entry.getKey())) {
            r.setIaasId(entry.getKey());
            insered.add(entry.getKey());
            break;
          }
        }
      } else if (r.getState() == NodeStates.DELETING) {
        deployment.getResources().remove(r);
      }
    }
  }

  private String extractVmId(InfrastructureUri vmUri) {
    Matcher matcher = VM_ID_PATTERN.matcher(vmUri.getUri());
    if (matcher.find()) {
      return matcher.group(0);
    }
    return "";
  }

  private ResponseError getImResponseError(ImClientErrorException exception) {
    return exception.getResponseError();
  }

  private void logImErrorResponse(ImClientErrorException exception) {
    LOG.error(exception.getResponseError().getFormattedErrorMessage());
  }

  // FIXME Remove once IM handles single nodes state update
  /**
   * Update the status of the deployment with an error message.
   * 
   * @param deploymentUuid
   *          the deployment id
   * @param message
   *          the error message
   */
  @Override
  public void updateOnError(String deploymentUuid, String message) {
    // WARNING: In IM we don't have the resource mapping yet, so we update all the resources
    // FIXME Remove once IM handles single nodes state update!!!! And pay attention to the
    // AbstractDeploymentProviderService.updateOnError method!
    Deployment deployment = deploymentRepository.findOne(deploymentUuid);
    switch (deployment.getStatus()) {
      case CREATE_FAILED:
      case UPDATE_FAILED:
      case DELETE_FAILED:
        LOG.warn("Deployment < {} > was already in {} state.", deploymentUuid,
            deployment.getStatus());
        break;
      case CREATE_IN_PROGRESS:
        deployment.setStatus(Status.CREATE_FAILED);
        updateResources(deployment, Status.CREATE_FAILED);
        break;
      case DELETE_IN_PROGRESS:
        deployment.setStatus(Status.DELETE_FAILED);
        updateResources(deployment, Status.DELETE_FAILED);
        break;
      case UPDATE_IN_PROGRESS:
        deployment.setStatus(Status.UPDATE_FAILED);
        updateResources(deployment, Status.UPDATE_FAILED);
        break;
      default:
        LOG.error("updateOnError: unsupported deployment status: {}. Setting status to {}",
            deployment.getStatus(), Status.UNKNOWN.toString());
        deployment.setStatus(Status.UNKNOWN);
        updateResources(deployment, Status.UNKNOWN);
        break;
    }
    deployment.setTask(Task.NONE);
    // Do not delete a previous statusReason if there's no explicit value! (used when isDeploy
    // reports an error and then the PollDeploy task calls the finalizeDeploy, which also uses this
    // method but does not have any newer statusReason)
    if (message != null) {
      deployment.setStatusReason(message);
    }
    deploymentRepository.save(deployment);
  }
}
