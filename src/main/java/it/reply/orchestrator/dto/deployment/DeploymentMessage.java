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

import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.service.deployment.providers.ChronosServiceImpl.IndigoJob;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.checkerframework.checker.nullness.qual.NonNull;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DeploymentMessage extends BaseWorkflowMessage implements Serializable {

  private static final long serialVersionUID = 8003907220093782923L;

  // Max value allowed by SQL
  // private static final Instant MAX_TIMEOUT = Instant.parse("9999-12-31T23:59:59.999Z");
  private static final Instant MAX_TIMEOUT = Instant.parse("2038-01-19T03:14:07.999Z");

  private String timeout;

  /**
   * Sets the Deployment timeout.
   * 
   * @param timeoutMins
   *          the timeout in Minutes
   */
  public void setTimeoutInMins(Integer timeoutMins) {
    this.timeout = Optional
        .ofNullable(timeoutMins)
        .map(value -> Instant.now().plus(Duration.ofMinutes(value)))
        .filter(value -> value.isBefore(MAX_TIMEOUT))
        .orElse(MAX_TIMEOUT)
        .toString();
  }

  private TemplateTopologicalOrderIterator templateTopologicalOrderIterator;

  private boolean createComplete;
  private boolean deleteComplete;
  private boolean pollComplete;
  private boolean skipPollInterval;

  private CloudProvider chosenCloudProvider;
  private CloudProviderEndpoint chosenCloudProviderEndpoint;

  /**
   * The OneData information generated after the best provider choice.
   */
  @NonNull
  private Map<String, OneData> oneDataParameters = new HashMap<>();

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

  public DeploymentMessage() {
    timeout = MAX_TIMEOUT.toString();
  }

  @Deprecated
  public RankCloudProvidersMessage toRankCloudProvidersMessage() {
    return new RankCloudProvidersMessage(this);
  }
}
