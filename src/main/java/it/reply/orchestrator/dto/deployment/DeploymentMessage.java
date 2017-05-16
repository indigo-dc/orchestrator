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

package it.reply.orchestrator.dto.deployment;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.enums.DeploymentType;
import it.reply.orchestrator.service.deployment.providers.ChronosServiceImpl.IndigoJob;

import lombok.Data;
import lombok.ToString;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A message containing all the information needed during Deployment WF.
 * 
 * @author l.biava
 *
 */
@Data
@ToString(exclude = "deployment")
public class DeploymentMessage implements Serializable {

  private static final long serialVersionUID = 8003907220093782923L;

  /**
   * The internal deployment representation (stored in the DB).
   */
  private Deployment deployment;

  @Nullable
  private OidcTokenId requestedWithToken;

  private String deploymentId;

  private DeploymentType deploymentType;

  private TemplateTopologicalOrderIterator templateTopologicalOrderIterator;

  private boolean createComplete;
  private boolean deleteComplete;
  private boolean pollComplete;
  private boolean skipPollInterval;

  private CloudProvider chosenCloudProvider;
  private CloudProviderEndpoint chosenCloudProviderEndpoint;

  /**
   * The OneData information the user gave and to be used to select the best provider.
   */
  @NonNull
  private Map<String, OneData> oneDataRequirements = new HashMap<>();
  /**
   * The OneData information generated after the best provider choice.
   */
  @NonNull
  private Map<String, OneData> oneDataParameters = new HashMap<>();

  /**
   * The Placement policies provided in the template.
   */
  @NonNull
  private List<PlacementPolicy> placementPolicies = new ArrayList<>();

  /**
   * TEMPORARY Chronos Job Graph (to avoid regenerating the template representation each time).
   */
  private Map<String, IndigoJob> chronosJobGraph;

  /**
   * Class to contain template's nodes in topological order and to allow to iterate on the list.
   * 
   * @author l.biava
   *
   */
  @Data
  public static class TemplateTopologicalOrderIterator implements Serializable {

    private static final long serialVersionUID = 1557615023166610397L;

    /**
     * Template's nodes, topologically ordered.
     */
    private List<Resource> topologicalOrder;

    private int position = 0;

    public TemplateTopologicalOrderIterator(List<Resource> topologicalOrder) {
      this.topologicalOrder = topologicalOrder;
    }

    public int getNodeSize() {
      return topologicalOrder.size();
    }

    public synchronized boolean hasNext() {
      return topologicalOrder.size() - 1 > position;
    }

    /**
     * Get the node in the current position of the iterator. <br/>
     * <b>Note that the first time this method is called it returns the first element of the list,
     * or <tt>null</tt> if the list is empty</b>
     * 
     * @return the current node, or <tt>null</tt> if the list is empty.
     */
    public synchronized Resource getCurrent() {
      if (position >= topologicalOrder.size()) {
        return null;
      }
      return topologicalOrder.get(position);
    }

    /**
     * Get the next element of the collection (after incrementing the position pointer).
     * 
     * @return the next node, or <tt>null</tt> if there aren't any others.
     */
    public synchronized Resource getNext() {
      if (!hasNext()) {
        position++;
        return null;
      }
      position++;
      return topologicalOrder.get(position);
    }

    public synchronized void reset() {
      position = 0;
    }

  }

}
