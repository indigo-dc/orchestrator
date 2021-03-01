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

package it.reply.orchestrator.dto.security;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TokenIntrospectionResponse {

  @JsonProperty("active")
  @NonNull
  @NotNull
  @Getter(AccessLevel.NONE)
  private Boolean active;

  @JsonProperty("scope")
  @NonNull
  @NotNull
  @JsonSerialize(using = Jackson2ScopeSerializer.class)
  @JsonDeserialize(using = Jackson2ScopeDeserializer.class)
  @Builder.Default
  private Set<String> scope = new HashSet<>();

  @JsonProperty("sub")
  @Nullable
  private String subject;

  @JsonProperty("client_id")
  @Nullable
  private String clientId;

  @JsonProperty("exp")
  @JsonFormat(shape = JsonFormat.Shape.NUMBER)
  @Nullable
  private Date expiration;

  @SuppressWarnings("null")
  @Deprecated
  protected TokenIntrospectionResponse() {
    scope = new HashSet<>();
  }

  public boolean isActive() {
    // Let it throw if null (because it shouldn't be)
    return active.booleanValue();
  }
}
