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

import java.io.Serializable;

/**
 * Temporary response mapping - use imported library from CloudProviderRanker project when
 * available.
 * 
 * @author l.biava
 *
 */

public class RankedCloudProvider implements Serializable {

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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public float getRank() {
    return rank;
  }

  public void setRank(float rank) {
    this.rank = rank;
  }

  public boolean isRanked() {
    return ranked;
  }

  public void setRanked(boolean ranked) {
    this.ranked = ranked;
  }

  public String getErrorReason() {
    return errorReason;
  }

  public void setErrorReason(String errorReason) {
    this.errorReason = errorReason;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((errorReason == null) ? 0 : errorReason.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + Float.floatToIntBits(rank);
    result = prime * result + (ranked ? 1231 : 1237);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    RankedCloudProvider other = (RankedCloudProvider) obj;
    if (errorReason == null) {
      if (other.errorReason != null) {
        return false;
      }
    } else if (!errorReason.equals(other.errorReason)) {
      return false;
    }
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    if (Float.floatToIntBits(rank) != Float.floatToIntBits(other.rank)) {
      return false;
    }
    if (ranked != other.ranked) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "RankedCloudProvider [name=" + name + ", rank=" + rank + ", ranked=" + ranked
        + ", errorReason=" + errorReason + "]";
  }

}
