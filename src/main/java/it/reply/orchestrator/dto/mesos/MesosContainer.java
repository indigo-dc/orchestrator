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

package it.reply.orchestrator.dto.mesos;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.Getter;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

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

  @NonNull
  private Type type;

  @Nullable
  private String image;

  private String network;

  @NonNull
  private List<MesosPortMapping> portMappings = new ArrayList<>();

  @NonNull
  private Multimap<String, String> parameters = ArrayListMultimap.create();

  @Nullable
  private Boolean forcePullImage;

  @Nullable
  private Boolean privileged;

  @NonNull
  private List<String> volumes = new ArrayList<>();

  public boolean isForcePullImage() {
    return Boolean.TRUE.equals(forcePullImage);
  }

  public boolean isPrivileged() {
    return Boolean.TRUE.equals(privileged);
  }

}
