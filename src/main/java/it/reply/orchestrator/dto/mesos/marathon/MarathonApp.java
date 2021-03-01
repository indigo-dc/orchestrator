/*
 * Copyright © 2015-2021 I.N.F.N.
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

package it.reply.orchestrator.dto.mesos.marathon;

import it.reply.orchestrator.dto.mesos.MesosTask;
import it.reply.orchestrator.utils.ToscaConstants;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.checkerframework.checker.nullness.qual.NonNull;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MarathonApp extends MesosTask<MarathonApp> {

  private Integer instances;

  @NonNull
  private Map<String, String> secrets = new HashMap<>();

  private boolean enableHttps = false;

  @Override
  public final String getToscaNodeName() {
    return ToscaConstants.Nodes.Types.MARATHON;
  }

}
