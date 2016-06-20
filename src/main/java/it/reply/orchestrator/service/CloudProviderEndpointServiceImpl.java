package it.reply.orchestrator.service;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.CloudProviderEndpoint.IaaSType;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.cmdb.Type;
import it.reply.orchestrator.dto.ranker.RankedCloudProvider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class CloudProviderEndpointServiceImpl {
  private static final Logger LOG = LogManager.getLogger(CloudProviderEndpointServiceImpl.class);

  public RankedCloudProvider chooseCloudProvider(Deployment deployment,
      RankCloudProvidersMessage rankCloudProvidersMessage) {
    // Choose Cloud Provider
    IaaSType iaasType = getIaaSTypeFromTosca(deployment.getTemplate());

    LOG.debug("Choosing Cloud Provider based on: {}, IaaSType: {}",
        rankCloudProvidersMessage.getRankedCloudProviders(), iaasType);

    // TODO Check ranker errors (i.e. providers with ranked = false)
    RankedCloudProvider chosenCp = null;
    for (RankedCloudProvider rcp : rankCloudProvidersMessage.getRankedCloudProviders()) {
      // Choose the one with lowest rank AND that matches iaasType (TEMPORARY)
      if ((isCompatible(iaasType, getProviderIaaSType(rankCloudProvidersMessage, rcp.getName())))
          && (chosenCp == null || rcp.getRank() < chosenCp.getRank())) {
        chosenCp = rcp;
      }
    }

    if (chosenCp == null) {
      String errorMsg = String.format("No Cloud Provider found for: {}, IaaSType: {}",
          rankCloudProvidersMessage.getRankedCloudProviders(), iaasType);
      LOG.error(errorMsg);
      throw new IllegalArgumentException(errorMsg);
    }

    LOG.debug("Selected Cloud Provider is: {}", chosenCp);
    return chosenCp;
  }

  public CloudProviderEndpoint getCloudProviderEndpoint(Deployment deployment,
      RankCloudProvidersMessage rankCloudProvidersMessage, RankedCloudProvider chosenCp) {

    IaaSType iaasType = getIaaSTypeFromTosca(deployment.getTemplate());

    CloudProviderEndpoint cpe = new CloudProviderEndpoint();

    it.reply.orchestrator.dto.cmdb.Service computeService = rankCloudProvidersMessage
        .getCloudProviders().get(chosenCp.getName()).getCmbdProviderServiceByType(Type.COMPUTE);

    cpe.setCpEndpoint(computeService.getData().getEndpoint());
    cpe.setIaasType(iaasType);
    // FIXME Add IM EP, if available

    return cpe;
  }

  /**
   * TEMPORARY method to decide whether it is needed to coerce CP choice based on template content
   * (i.e. currently used to force Mesos cluster deployment on OpenStack).
   * 
   * @param template
   *          .
   * @return .
   */
  public IaaSType getIaaSTypeFromTosca(String template) {

    if (template.contains("tosca.nodes.indigo.MesosMaster")) {
      return IaaSType.OPENSTACK;
    } else if (template.contains("onedock.i3m.upv.es")) {
      return IaaSType.ONEDOCK;
    } else {
      return IaaSType.OPENNEBULA;
    }
  }

  protected boolean isCompatible(IaaSType requiredType, IaaSType providerType) {
    if (requiredType == providerType) {
      return true;
    } else if (requiredType == IaaSType.ONEDOCK && providerType == IaaSType.OPENNEBULA) {
      // OneDock is a special case of OneNebula -.-
      return true;
    } else {
      return false;
    }
  }

  protected IaaSType getProviderIaaSType(RankCloudProvidersMessage rankCloudProvidersMessage,
      String providerName) {
    String serviceType = rankCloudProvidersMessage.getCloudProviders().get(providerName)
        .getCmbdProviderServiceByType(Type.COMPUTE).getData().getServiceType();
    if (serviceType.contains("openstack")) {
      return IaaSType.OPENSTACK;
    }

    return IaaSType.OPENNEBULA;
  }
}
