package it.reply.orchestrator.service.deployment.providers;

/*
 * Copyright © 2015-2017 Santer Reply S.p.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.google.common.base.Strings;

import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingException;

import es.upv.i3m.grycap.im.InfrastructureManager;
import es.upv.i3m.grycap.im.States;
import es.upv.i3m.grycap.im.auth.credentials.AuthorizationHeader;
import es.upv.i3m.grycap.im.auth.credentials.Credentials;
import es.upv.i3m.grycap.im.auth.credentials.providers.AmazonEc2Credentials;
import es.upv.i3m.grycap.im.auth.credentials.providers.ImCredentials;
import es.upv.i3m.grycap.im.auth.credentials.providers.OpenNebulaCredentials;
import es.upv.i3m.grycap.im.auth.credentials.providers.OpenStackCredentials;
import es.upv.i3m.grycap.im.auth.credentials.providers.OpenstackAuthVersion;
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

import it.reply.orchestrator.config.properties.OidcProperties;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
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

  private static final Logger LOG = LoggerFactory.getLogger(ImServiceImpl.class);

  @Autowired
  private ApplicationContext ctx;

  @Value("${url}")
  private String imUrl;

  @Value("${auth.file.path}")
  private String authFilePath;

  @Value("${openstack.auth.file.path}")
  private String openstackAuthFilePath;

  @Value("${opennebula.auth.file.path}")
  private String opennebulaAuthFilePath;

  private static final Pattern VM_ID_PATTERN = Pattern.compile("(\\w+)$");
  private static final Pattern OS_ENDPOINT_PATTERN =
      Pattern.compile("(https?:\\/\\/[^\\/]*)\\/?([^\\/]*)");

  @Autowired
  private ToscaService toscaService;

  @Autowired
  private DeploymentRepository deploymentRepository;

  @Autowired
  private ResourceRepository resourceRepository;

  @Autowired
  private OidcProperties oidcProperties;

  protected InfrastructureManager getClient(DeploymentMessage dm) {
    IaaSType iaasType = dm.getChosenCloudProviderEndpoint().getIaasType();
    String authString = null;
    try {
      LOG.debug("Load {} credentials with: {}", iaasType, dm.getChosenCloudProviderEndpoint());
      switch (iaasType) {
        case OPENSTACK:
          // FIXME remove hardcoded string
          if (dm.getChosenCloudProviderEndpoint().getCpEndpoint().contains("recas.ba.infn")
              || !oidcProperties.isEnabled()) {
            try (InputStream is = ctx.getResource(openstackAuthFilePath).getInputStream()) {
              authString = IOUtils.toString(is);
            }
            if (oidcProperties.isEnabled()) {
              authString =
                  authString.replaceAll("InfrastructureManager; username = .+; password = .+",
                      "InfrastructureManager; token = " + dm.getOauth2Token());
            }
            authString = authString.replaceAll("\n", "\\\\n");
          } else {
            String endpoint = dm.getChosenCloudProviderEndpoint().getCpEndpoint();
            OpenstackAuthVersion authVersion = OpenstackAuthVersion.PASSWORD_2_0;
            Matcher matcher = OS_ENDPOINT_PATTERN.matcher(endpoint);
            if (!matcher.matches()) {
              throw new DeploymentException("Wrong OS endpoint format: " + endpoint);
            } else {
              endpoint = matcher.group(1);
              if (matcher.groupCount() > 1 && matcher.group(2).equals("v3")) {
                authVersion = OpenstackAuthVersion.PASSWORD_3_X;
              }
            }
            AuthorizationHeader ah = new AuthorizationHeader();
            Credentials cred = ImCredentials.buildCredentials().withToken(dm.getOauth2Token());
            ah.addCredential(cred);
            cred = OpenStackCredentials.buildCredentials().withTenant("oidc")
                .withUsername("indigo-dc").withPassword(dm.getOauth2Token()).withHost(endpoint)
                .withAuthVersion(authVersion);
            ah.addCredential(cred);
            InfrastructureManager im = new InfrastructureManager(imUrl, ah);
            return im;
          }
          break;
        case OPENNEBULA:
          if (oidcProperties.isEnabled()) {
            AuthorizationHeader ah = new AuthorizationHeader();
            Credentials cred = ImCredentials.buildCredentials().withToken(dm.getOauth2Token());
            ah.addCredential(cred);
            cred = OpenNebulaCredentials.buildCredentials()
                .withHost(dm.getChosenCloudProviderEndpoint().getCpEndpoint())
                .withToken(dm.getOauth2Token());
            ah.addCredential(cred);
            InfrastructureManager im = new InfrastructureManager(imUrl, ah);
            return im;
          } else {
            // read onedock auth file
            try (InputStream in = ctx.getResource(opennebulaAuthFilePath).getInputStream()) {
              authString = IOUtils.toString(in, StandardCharsets.UTF_8.toString());
            }
            authString = authString.replaceAll("\n", "\\\\n");
          }
          break;
        // inputStream = ctx.getResource(opennebulaAuthFilePath).getInputStream();
        // break;
        case AWS:
          AuthorizationHeader ah = new AuthorizationHeader();
          Credentials cred = ImCredentials.buildCredentials().withToken(dm.getOauth2Token());
          ah.addCredential(cred);
          cred = AmazonEc2Credentials.buildCredentials()
              .withUsername(dm.getChosenCloudProviderEndpoint().getUsername())
              .withPassword(dm.getChosenCloudProviderEndpoint().getPassword());
          ah.addCredential(cred);
          InfrastructureManager im = new InfrastructureManager(imUrl, ah);
          return im;
        default:
          throw new IllegalArgumentException(
              String.format("Unsupported provider type <%s>", iaasType));
      }
      InfrastructureManager im = new InfrastructureManager(imUrl, authString);
      return im;
    } catch (IOException | ImClientException ex) {
      throw new OrchestratorException("Cannot load IM auth file", ex);
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
      toscaService.addElasticClusterParameters(ar, deploymentUuid,
          deploymentMessage.getOauth2Token());
      toscaService.contextualizeImages(DeploymentProvider.IM, ar,
          deploymentMessage.getChosenCloudProvider(),
          deploymentMessage.getChosenCloudProviderEndpoint().getCpComputeServiceId());
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
      LOG.error("Error deploying", ex);
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
          StringBuilder errorMsg = new StringBuilder().append("Fail to deploy deployment <")
              .append(deployment.getId()).append(">\nIM id is: <").append(deployment.getEndpoint())
              .append(">\nIM response is: <")
              .append(infrastructureState.getFormattedInfrastructureStateString()).append(">");
          try {
            // Try to get the logs of the virtual infrastructure for debug
            // purpose.
            Property contMsg = im.getInfrastructureContMsg(deployment.getEndpoint());
            if (!Strings.isNullOrEmpty(contMsg.getValue())) {
              errorMsg.append("\nIM contMsg is: ").append(contMsg.getValue());
            }
          } catch (Exception ex) {
            // Do nothing
          }
          DeploymentException ex = new DeploymentException(errorMsg.toString());
          updateOnError(deployment.getId(), ex); // Set failure information in the deployment
          LOG.error(errorMsg.toString());
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
      // TODO: refactor this code and use a shared implementation for error handling and logging
      DeploymentException ex = new DeploymentException(errorMsg);
      updateOnError(deployment.getId(), ex); // Set failure information in the deployment
      LOG.error(errorMsg);
      throw ex;
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
        LOG.error("Error finalizing deployment", ex);
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
      toscaService.addElasticClusterParameters(newAr, deployment.getId(),
          deploymentMessage.getOauth2Token());
      toscaService.contextualizeImages(DeploymentProvider.IM, newAr,
          deploymentMessage.getChosenCloudProvider(),
          deploymentMessage.getChosenCloudProviderEndpoint().getCpComputeServiceId());
    } catch (ParsingException | IOException | ToscaException | ParseException ex) {
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

      if (deployment.getEndpoint() == null) {
        // updateOnSuccess(deploymentUuid);
        deploymentMessage.setDeleteComplete(true);
        return true;
      }

      // Generate IM Client
      InfrastructureManager im = getClient(deploymentMessage);

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
      LOG.error("Error undeploying", ex);
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
      LOG.error("Error checking for undeployment", ex);
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
