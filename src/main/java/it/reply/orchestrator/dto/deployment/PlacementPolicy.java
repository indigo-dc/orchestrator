package it.reply.orchestrator.dto.deployment;

import alien4cloud.model.components.AbstractPropertyValue;

import java.io.Serializable;
import java.util.List;

public interface PlacementPolicy extends Serializable {

  public List<String> getNodes();

  public void setNodes(List<String> nodes);

  /**
   * Generate a Orchestrator PlacementPolicy from a TOSCA
   * {@link alien4cloud.model.topology.PlacementPolicy PlacementPolicy}.
   * 
   * @param toscaPolicy
   *          the TOSCA placement policy
   * @return the new Orchestrator PlacementPolicy
   */
  public static PlacementPolicy fromToscaType(
      alien4cloud.model.topology.PlacementPolicy toscaPolicy) {
    AbstractPropertyValue slaIdProperty = toscaPolicy.getProperties()
        .get(alien4cloud.model.topology.PlacementPolicy.PLACEMENT_ID_PROPERTY);
    AbstractPropertyValue usernameProperty = toscaPolicy.getProperties().get("username");
    AbstractPropertyValue passwordProperty = toscaPolicy.getProperties().get("password");
    if (slaIdProperty != null) {
      SlaPlacementPolicy placementPolicy =
          new SlaPlacementPolicy(toscaPolicy.getTargets(), slaIdProperty);
      if (usernameProperty != null || passwordProperty != null) {
        placementPolicy = new CredentialsAwareSlaPlacementPolicy(placementPolicy, usernameProperty,
            passwordProperty);
      }
      return placementPolicy;
    } else {
      throw new IllegalArgumentException("Only PlacementPolicies with SLA ids are supported");
    }
  }
}
