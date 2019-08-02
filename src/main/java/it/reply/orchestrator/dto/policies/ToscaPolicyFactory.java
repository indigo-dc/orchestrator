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

package it.reply.orchestrator.dto.policies;

import it.reply.orchestrator.utils.ToscaConstants.Policies.Properties;
import it.reply.orchestrator.utils.ToscaConstants.Policies.Types;
import it.reply.orchestrator.utils.ToscaUtils;

import lombok.experimental.UtilityClass;

import org.alien4cloud.tosca.model.templates.PolicyTemplate;

@UtilityClass
public class ToscaPolicyFactory {

  /**
   * Generate a ToscaPolicy from a {@link PolicyTemplate PolicyTemplate}.
   *
   * @param policyTemplate
   *     the TOSCA placement policy
   * @return the new ToscaPolicy
   */
  public static ToscaPolicy fromToscaType(PolicyTemplate policyTemplate) {

    if (Types.SLA_PLACEMENT.equals(policyTemplate.getType())) {
      String slaIdProperty = ToscaUtils
          .extractScalar(policyTemplate.getProperties(), Properties.PLACEMENT_ID)
          .orElse(null);
      return new SlaPlacementPolicy(policyTemplate.getTargets(), slaIdProperty);
    } else if (Types.CREDENTIALS_AWARE_SLA_PLACEMENT.equals(policyTemplate.getType())) {
      String slaIdProperty = ToscaUtils
          .extractScalar(policyTemplate.getProperties(), Properties.PLACEMENT_ID)
          .orElse(null);
      String usernameProperty = ToscaUtils
          .extractScalar(policyTemplate.getProperties(), Properties.USERNAME)
          .orElse(null);
      String passwordProperty = ToscaUtils
          .extractScalar(policyTemplate.getProperties(), Properties.PASSWORD)
          .orElse(null);
      String tenantProperty = ToscaUtils
          .extractScalar(policyTemplate.getProperties(), Properties.TENANT_ID)
          .orElse(null);

      return new CredentialsAwareSlaPlacementPolicy(
          policyTemplate.getTargets(),
          slaIdProperty,
          usernameProperty,
          passwordProperty,
          tenantProperty
      );
    } else {
      return new GenericToscaPolicy(policyTemplate.getType(), policyTemplate.getTargets());
    }
  }
}
