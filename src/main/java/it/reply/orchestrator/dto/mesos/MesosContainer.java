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

package it.reply.orchestrator.dto.mesos;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

@Data
public class MesosContainer {

  @Getter
  public enum Type {
    DOCKER("DOCKER", "tosca.artifacts.Deployment.Image.Container.Docker");

    Type(String name, String toscaName) {
      this.name = name;
      this.toscaName = toscaName;
    }

    private final String name;

    private final String toscaName;
  }

  @Nonnull
  private Type type;

  private String image;

  private String network = "BRIDGE";

  @Nonnull
  private List<MesosPortMapping> portMappings = new ArrayList<>();

  @Nonnull
  private Multimap<String, String> parameters = ArrayListMultimap.create();

  private boolean forcePullImage;

  @Nonnull
  private List<String> volumes = new ArrayList<>();

}
