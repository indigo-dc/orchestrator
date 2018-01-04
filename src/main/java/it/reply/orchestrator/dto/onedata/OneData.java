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

import it.reply.orchestrator.utils.CommonUtils;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Tolerate;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@ToString(exclude = "token")
@Builder
public class OneData implements Serializable {

  private static final long serialVersionUID = 8590316308119399053L;

  @Data
  @Builder
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class OneDataProviderInfo implements Serializable {

    private static final long serialVersionUID = -4904767929269221557L;

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
  private String zone;

  @NonNull
  @Builder.Default
  private List<OneDataProviderInfo> providers = new ArrayList<>();

  private boolean smartScheduling;

  /**
   * Generate a List of {@link OneDataProviderInfo} from a csv of providers endpoint.
   * 
   * @param providers
   *          the csv of providers endpoint
   * @return the List of {@link OneDataProviderInfo}
   */
  public static List<OneDataProviderInfo> providersListFromString(@Nullable String providers) {
    return Optional.ofNullable(providers)
        .map(value -> CommonUtils.checkNotNull(value).split(","))
        .map(Stream::of)
        .orElseGet(Stream::empty)
        .map(endpoint -> OneDataProviderInfo.builder().endpoint(endpoint).build())
        .collect(Collectors.toList());
  }

  public static class OneDataBuilder {

    @Tolerate
    public OneDataBuilder providers(@Nullable String providers) {
      return providers(providersListFromString(providers));
    }
  }
}
