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

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ProviderDetails implements Serializable {

  private static final long serialVersionUID = -368387049626457198L;

  @JsonProperty("csr")
  @Nullable
  private String csr;

  @JsonProperty("providerId")
  @NonNull
  @NotNull
  private String providerId;

  @JsonProperty("clientName")
  @Nullable
  private String clientName;

  @JsonProperty("redirectionPoint")
  @Nullable
  private String redirectionPoint;

  @JsonProperty("urls")
  @NonNull
  @NotNull
  @Builder.Default
  private List<String> urls = new ArrayList<>();

  @JsonProperty("latitude")
  @Nullable
  private Double latitude;

  @JsonProperty("longitude")
  @Nullable
  private Double longitude;

  @SuppressWarnings("null")
  @Deprecated
  protected ProviderDetails() {
    urls = new ArrayList<>();
  }

}
