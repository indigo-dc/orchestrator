package it.reply.orchestrator.dto.cmdb;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloudService extends CmdbDataWrapper<CloudService, CloudServiceData>
    implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  private static final String COMPUTE_SERVICE_PREFIX = "eu.egi.cloud.vm-management";
  private static final String STORAGE_SERVICE_PREFIX = "eu.egi.cloud.storage-management";

  public static final String OPENSTACK_COMPUTE_SERVICE = COMPUTE_SERVICE_PREFIX + ".openstack";
  public static final String OPENNEBULA_COMPUTE_SERVICE = COMPUTE_SERVICE_PREFIX + ".opennebula";
  public static final String OCCI_COMPUTE_SERVICE = COMPUTE_SERVICE_PREFIX + ".occi";
  public static final String AWS_COMPUTE_SERVICE = "com.amazonaws.ec2";

  public static final String CDMI_STORAGE_SERVICE = STORAGE_SERVICE_PREFIX + ".cdmi";
  public static final String ONEPROVIDER_STORAGE_SERVICE = STORAGE_SERVICE_PREFIX + ".oneprovider";

  /**
   * Get if the the service is a OpenStack compute service.
   * 
   * @return true if the service is a OpenStack compute service
   */
  public boolean isOpenStackComputeProviderService() {
    if (getData() != null && OPENSTACK_COMPUTE_SERVICE.equals(getData().getServiceType())) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Get if the the service is a OpenNebula compute service.
   * 
   * @return true if the service is a OpenNebula compute service
   */
  public boolean isOpenNebulaComputeProviderService() {
    if (getData() != null && OPENNEBULA_COMPUTE_SERVICE.equals(getData().getServiceType())) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Get if the the service is a OCCI compute service.
   * 
   * @return true if the service is a OCCI compute service
   */
  public boolean isOcciComputeProviderService() {
    if (getData() != null && OCCI_COMPUTE_SERVICE.equals(getData().getServiceType())) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Get if the the service is a AWS compute service.
   * 
   * @return true if the service is a AWS compute service
   */
  public boolean isAwsComputeProviderService() {
    if (getData() != null && AWS_COMPUTE_SERVICE.equals(getData().getServiceType())) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Get if the the service is a OneProvider storage service.
   * 
   * @return true if the service is a OneProvider storage service
   */
  public boolean isOneProviderStorageService() {
    if (getData() != null && ONEPROVIDER_STORAGE_SERVICE.equals(getData().getServiceType())) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Get if the the service is a CDMI storage service.
   * 
   * @return true if the service is a CDMI storage service
   */
  public boolean isCdmiStorageProviderService() {
    if (getData() != null && CDMI_STORAGE_SERVICE.equals(getData().getServiceType())) {
      return true;
    } else {
      return false;
    }
  }

}
