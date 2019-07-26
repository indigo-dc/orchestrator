/*
 * Copyright © 2015-2019 Santer Reply S.p.A.
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

package it.reply.orchestrator.dto.onedata;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Data
@ToString(exclude = "token")
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class OneData {

  @Data
  @Builder
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  @AllArgsConstructor(access = AccessLevel.PROTECTED)
  public static class OneDataProviderInfo {

    @Nullable
    private String id;

    @Nullable
    private String endpoint;

    @Nullable
    private String cloudProviderId;

    @Nullable
    private String cloudServiceId;

  }

  @Nullable
  private String token;

  @Nullable
  private String space;

  @Nullable
  private String path;

  @Nullable
  private String onezone;

  @NonNull
  @NotNull
  @Builder.Default
  private List<OneDataProviderInfo> oneproviders = new ArrayList<>();

  @Nullable
  private OneDataProviderInfo selectedOneprovider;

  private boolean smartScheduling;

  private boolean serviceSpace;

  @Deprecated
  protected OneData() {
    oneproviders = new ArrayList<>();
  }

}
