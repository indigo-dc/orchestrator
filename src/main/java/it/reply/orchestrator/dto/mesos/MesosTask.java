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

package it.reply.orchestrator.dto.mesos;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

@Data
public abstract class MesosTask<T extends MesosTask<T>> {

  public abstract String getToscaNodeName();

  private String id;

  private String cmd;

  private MesosContainer container;

  private Double cpus;

  private Double memSize;

  @Nonnull
  private List<T> parents = new ArrayList<>();

  @Nonnull
  private List<List<String>> constraints = new ArrayList<>();

  @Nonnull
  private List<String> uris = new ArrayList<>();

  @Nonnull
  private Map<String, String> env = new HashMap<>();

  @Nonnull
  private Map<String, String> labels = new HashMap<>();

  public Optional<MesosContainer> getContainer() {
    return Optional.ofNullable(this.container);
  }

}
