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

package it.reply.orchestrator.dto.onedata;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
public class OneData implements Cloneable {

  @Data
  @Builder
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  @AllArgsConstructor(access = AccessLevel.PROTECTED)
  public static class OneDataProviderInfo implements Cloneable {

    @Nullable
    private String id;

    @Nullable
    private String endpoint;

    @Nullable
    private String cloudProviderId;

    @Nullable
    private String cloudServiceId;

    @Override
    public OneDataProviderInfo clone() {
      return OneDataProviderInfo
          .builder()
          .id(this.id)
          .endpoint(this.endpoint)
          .cloudProviderId(this.cloudProviderId)
          .cloudServiceId(this.cloudServiceId)
          .build();
    }
  }

  @Nullable
  private String token;

  @Nullable
  private String space;

  @Nullable
  private String path;

  @Nullable
  private String zone;

  @NonNull
  @NotNull
  @Builder.Default
  private List<OneDataProviderInfo> providers = new ArrayList<>();

  private boolean smartScheduling;

  @Override
  public OneData clone() {
    return OneData
        .builder()
        .token(this.token)
        .space(this.space)
        .path(this.path)
        .zone(this.zone)
        .providers(providers
            .stream()
            .map(OneDataProviderInfo::clone)
            .collect(Collectors.toList()))
        .build();
  }

  @Deprecated
  protected OneData() {
    providers = new ArrayList<>();
  }

  public static class OneDataBuilder {

    public OneDataBuilder providersAsString(@Nullable String providers) {
      return providers(Optional
          .ofNullable(providers)
          .map(value -> value.split(","))
          .map(Stream::of)
          .orElseGet(Stream::empty)
          .map(endpoint -> OneDataProviderInfo.builder().endpoint(endpoint).build())
          .collect(Collectors.toList()));
    }
  }
}
