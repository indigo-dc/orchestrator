package it.reply.orchestrator.dto;

import java.io.Serializable;

/**
 * This class holds information to connect (and authenticate) to a CloudProvider.
 * 
 * @author l.biava
 *
 */
public class CloudProviderEndpoint implements Serializable {

  private static final long serialVersionUID = -2585914648218602033L;

  public enum IaaSType {
    // @formatter:off
    OPENSTACK, OPENNEBULA
    // @formatter:on
  }

  private String imEndpoint;
  private String cpEndpoint;
  private String cpComputeServiceId;
  private IaaSType iaasType;

  public String getImEndpoint() {
    return imEndpoint;
  }

  public void setImEndpoint(String imEndpoint) {
    this.imEndpoint = imEndpoint;
  }

  public String getCpEndpoint() {
    return cpEndpoint;
  }

  public void setCpEndpoint(String cpEndpoint) {
    this.cpEndpoint = cpEndpoint;
  }

  public String getCpComputeServiceId() {
    return cpComputeServiceId;
  }

  public void setCpComputeServiceId(String cpComputeServiceId) {
    this.cpComputeServiceId = cpComputeServiceId;
  }

  public IaaSType getIaasType() {
    return iaasType;
  }

  public void setIaasType(IaaSType iaasType) {
    this.iaasType = iaasType;
  }

  @Override
  public String toString() {
    return "CloudProviderEndpoint [imEndpoint=" + imEndpoint + ", cpEndpoint=" + cpEndpoint
        + ", cpComputeServiceId=" + cpComputeServiceId + ", iaasType=" + iaasType + "]";
  }

}
