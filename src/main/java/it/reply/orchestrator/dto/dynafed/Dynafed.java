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

package it.reply.orchestrator.dto.dynafed;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Dynafed {

  @Builder.Default
  private List<File> files = new ArrayList<>();

  @Deprecated
  private Dynafed() {
    files = new ArrayList<>();
  }

  @Data
  @Builder
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class File {

    private String endpoint;

    private Long size;

    @Builder.Default
    private List<Resource> resources = new ArrayList<>();

    @Deprecated
    private File() {
      resources = new ArrayList<>();
    }

  }

  @Data
  @Builder
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Resource {

    private String endpoint;

    private String cloudProviderId;

    private String cloudServiceId;

  }

}
