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
import alien4cloud.model.components.ScalarPropertyValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.springframework.util.Assert;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SlaPlacementPolicy implements PlacementPolicy {

  private List<String> nodes = new ArrayList<>();

  private String slaId;
  
  private List<String> serviceIds = new ArrayList<>();

  public SlaPlacementPolicy(List<String> nodes, String slaId) {
    this.setNodes(nodes);
    this.setSlaId(slaId);
  }

  public SlaPlacementPolicy(List<String> nodes, AbstractPropertyValue slaId) {
    this.setNodes(nodes);
    this.setSlaId(slaId);
  }

  @Override
  public void setNodes(List<String> nodes) {
    Objects.requireNonNull(nodes, "nodes list must not be null");
    this.nodes = nodes;
  }

  public void setSlaId(String slaId) {
    Objects.requireNonNull(slaId, PlacementPolicy.PLACEMENT_ID_PROPERTY_NAME + " must not be null");
    this.slaId = slaId;
  }

  /**
   * Set the SLA id from a {@link AbstractPropertyValue}.
   * 
   * @param slaId
   *          the SLA id
   */
  public void setSlaId(AbstractPropertyValue slaId) {
    Objects.requireNonNull(slaId, PlacementPolicy.PLACEMENT_ID_PROPERTY_NAME + " must not be null");
    Assert.isInstanceOf(ScalarPropertyValue.class, slaId,
        PlacementPolicy.PLACEMENT_ID_PROPERTY_NAME + " must be a scalar value");
    this.slaId = ((ScalarPropertyValue) slaId).getValue();
  }

  public void setServiceIds(List<String> serviceIds) {
    Objects.requireNonNull(serviceIds, "service IDs list must not be null");
    this.serviceIds = serviceIds;
  }

}
