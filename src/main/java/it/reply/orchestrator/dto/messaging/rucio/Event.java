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

package it.reply.orchestrator.dto.messaging.rucio;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "event_type",
    defaultImpl = GenericEvent.class,
    visible = true,
    include = JsonTypeInfo.As.EXISTING_PROPERTY
)
@JsonSubTypes({
    @JsonSubTypes.Type(
        value = RuleOkEvent.class,
        name = RuleOkEvent.EVENT_TYPE
    ),
    @JsonSubTypes.Type(
        value = TransferFailedEvent.class,
        name = TransferFailedEvent.EVENT_TYPE
    )
})
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Event<T extends EventPayload> {

  @JsonProperty("event_type")
  @NonNull
  @NotNull
  private String eventType;

  @JsonProperty("created_at")
  @NonNull
  @NotNull
  private String createdAt;

  @JsonProperty("payload")
  @NonNull
  @NotNull
  private T payload;

}

