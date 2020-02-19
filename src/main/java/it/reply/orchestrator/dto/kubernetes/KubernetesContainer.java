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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import lombok.Data;

@Data
public class KubernetesContainer {
  
  private String id;
  
  private final String name;

  private final String toscaName;

  private String image;
  
  private Multimap<String, String> parameters = ArrayListMultimap.create();
}
