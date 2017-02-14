package it.reply.orchestrator.dto.deployment;

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
