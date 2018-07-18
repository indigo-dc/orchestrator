/*
 * Copyright © 2015-2018 Santer Reply S.p.A.
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

package it.reply.orchestrator.dto.deployment;

import alien4cloud.model.components.AbstractPropertyValue;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import it.reply.orchestrator.utils.CommonUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(
        value = SlaPlacementPolicy.class,
        name = SlaPlacementPolicy.TOSCA_TYPE
    ),
    @JsonSubTypes.Type(
        value = CredentialsAwareSlaPlacementPolicy.class,
        name = CredentialsAwareSlaPlacementPolicy.TOSCA_TYPE
    )
})
public interface PlacementPolicy {

  public static final String PLACEMENT_ID_PROPERTY_NAME =
      alien4cloud.model.topology.PlacementPolicy.PLACEMENT_ID_PROPERTY;
  public static final String USERNAME_PROPERTY_NAME = "username";
  public static final String PASSWORD_PROPERTY_NAME = "password";
  public static final String TENANT_PROPERTY_NAME = "subscription_id";

  public List<String> getTargets();

  public void setTargets(List<String> targets);

  public String getType();

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
    Map<String, AbstractPropertyValue> properties =
        CommonUtils.notNullOrDefaultValue(toscaPolicy.getProperties(), HashMap::new);
    AbstractPropertyValue slaIdProperty = properties.get(PLACEMENT_ID_PROPERTY_NAME);
    AbstractPropertyValue usernameProperty = properties.get(USERNAME_PROPERTY_NAME);
    AbstractPropertyValue passwordProperty = properties.get(PASSWORD_PROPERTY_NAME);
    AbstractPropertyValue tenantProperty = properties.get(TENANT_PROPERTY_NAME);
    if (slaIdProperty != null) {
      SlaPlacementPolicy placementPolicy =
          new SlaPlacementPolicy(toscaPolicy.getTargets(), slaIdProperty);
      if (usernameProperty != null || passwordProperty != null) {
        placementPolicy = new CredentialsAwareSlaPlacementPolicy(placementPolicy, usernameProperty,
            passwordProperty, tenantProperty);
      }
      return placementPolicy;
    } else {
      throw new IllegalArgumentException("Only PlacementPolicies with SLA ids are supported");
    }
  }
}
