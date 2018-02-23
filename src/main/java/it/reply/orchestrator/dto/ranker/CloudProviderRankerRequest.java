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

package it.reply.orchestrator.dto.ranker;

import com.fasterxml.jackson.annotation.JsonProperty;

import it.reply.orchestrator.dto.slam.PreferenceCustomer;
import it.reply.orchestrator.dto.slam.Sla;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class CloudProviderRankerRequest implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  @JsonProperty("preferences")
  @Builder.Default
  private List<PreferenceCustomer> preferences = new ArrayList<>();

  @JsonProperty("sla")
  @Builder.Default
  private List<Sla> sla = new ArrayList<>();

  @JsonProperty("monitoring")
  @Builder.Default
  private List<Monitoring> monitoring = new ArrayList<>();

  @Deprecated
  protected CloudProviderRankerRequest() {
    preferences = new ArrayList<>();
    sla = new ArrayList<>();
    monitoring = new ArrayList<>();
  }
}
