package it.reply.orchestrator.dto.cmdb;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloudService extends CmdbDataWrapper<CloudService, CloudServiceData>
    implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  public static final String ONEPROVIDER_SERVICE = "eu.egi.cloud.storage-management.oneprovider";

  /**
   * Get if the the service is a OneProvider.
   * 
   * @return true if the service is a OneProvider
   */
  public boolean isOneProviderService() {
    if (getData() != null && ONEPROVIDER_SERVICE.equals(getData().getServiceType())) {
      return true;
    } else {
      return false;
    }
  }
}
