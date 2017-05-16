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

package it.reply.orchestrator.dto.ranker;

import it.reply.orchestrator.dto.AdditionalPropertiesAwareDto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;

/**
 * Temporary response mapping - use imported library from CloudProviderRanker project when
 * available.
 * 
 * @author l.biava
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RankedCloudProvider extends AdditionalPropertiesAwareDto implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  private String name;
  private float rank;
  private boolean ranked;
  private String errorReason;

  public RankedCloudProvider() {
  }

  /**
   * Generates a new RankedCloudProvider representation.
   * 
   * @param name
   *          the name of the cloud provider
   * @param rank
   *          the rank of the clou provider
   * @param ranked
   *          the ranking status of the cloud provider
   * @param error
   *          the error generated during the ranking, if any
   */
  public RankedCloudProvider(String name, float rank, boolean ranked, String error) {
    this.name = name;
    this.rank = rank;
    this.errorReason = error;
    this.ranked = ranked;
  }

}
