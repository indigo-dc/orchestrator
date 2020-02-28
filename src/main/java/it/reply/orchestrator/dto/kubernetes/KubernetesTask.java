/*
 * Copyright © 2015-2020 Santer Reply S.p.A.
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

package it.reply.orchestrator.dto.kubernetes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import it.reply.orchestrator.utils.ToscaConstants;
import lombok.Data;

@Data
public class KubernetesTask {

  public final String getToscaNodeName() {
    return ToscaConstants.Nodes.Types.KUBERNETES;
  }

  private String id;

  private List<KubernetesContainer> containers;

  private KubernetesContainer container;

  private Double cpu;

  private Double memory;

  private Double replicas;

  private Integer instances;
  
  private List<String> volumes = new ArrayList<>();
  
  public Optional<KubernetesContainer> getContainer() {
    return Optional.ofNullable(this.container);
  }

}
