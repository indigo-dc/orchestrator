/*
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AccessGrant implements Serializable {

  private static final long serialVersionUID = 1L;

  @JsonProperty("access_token")
  @NonNull
  @NotNull
  private String accessToken;

  @JsonProperty("token_type")
  @NonNull
  @NotNull
  private String tokenType;

  @JsonProperty("expires_in")
  @Nullable
  @JsonSerialize(using = ExpireInSerializer.class)
  @JsonDeserialize(using = ExpireInDeserializer.class)
  private Date expiration;

  @JsonProperty("refresh_token")
  @Nullable
  private String refreshToken;

  @JsonProperty("scope")
  @NonNull
  @NotNull
  @JsonSerialize(using = Jackson2ScopeSerializer.class)
  @JsonDeserialize(using = Jackson2ScopeDeserializer.class)
  @Builder.Default
  private Set<String> scopes = new HashSet<>();

  @SuppressWarnings("null")
  @Deprecated
  protected AccessGrant() {
    scopes = new HashSet<>();
  }

  @JsonIgnore
  public boolean isExpired() {
    return isExpiringIn(Duration.ZERO);
  }

  /**
   * Check if the grant is expiring in the specified {@link TemporalAmount}.
   *
   * @param temporalAmount
   *     the temporalAmount for the expiration evaluation
   * @return true if expired, false otherwise
   */
  @JsonIgnore
  public boolean isExpiringIn(TemporalAmount temporalAmount) {
    return Optional
        .ofNullable(expiration)
        .map(Date::toInstant)
        .map(instant -> instant
            // if expiring in less of the temporalAmount -> return true
            .minus(temporalAmount))
        .filter(Instant.now()::isAfter)
        .isPresent();
  }

  public static class ExpireInDeserializer extends StdDeserializer<Date> {

    private static final long serialVersionUID = 1L;

    protected ExpireInDeserializer() {
      super(Date.class);
    }

    @Override
    public Date deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
      return Optional
          .ofNullable(_parseLong(parser, ctxt))
          .map(Instant.now()::plusSeconds)
          .map(Date::from)
          .orElse(null);
    }
  }

  public static class ExpireInSerializer extends StdScalarSerializer<Date> {

    private static final long serialVersionUID = 1L;

    protected ExpireInSerializer() {
      super(Date.class);
    }

    @Override
    public void serialize(Date expiration, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      long duration = ChronoUnit.SECONDS.between(Instant.now(), expiration.toInstant());
      gen.writeNumber(Math.max(duration, 0));
    }

    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint) {
      return createSchemaNode("integer", true);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        throws JsonMappingException {
      visitIntFormat(visitor, typeHint, JsonParser.NumberType.LONG);
    }
  }
}
